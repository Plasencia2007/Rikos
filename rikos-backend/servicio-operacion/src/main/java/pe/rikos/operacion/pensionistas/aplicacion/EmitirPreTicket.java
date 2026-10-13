package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.tiempo.ConfiguracionReloj;
import pe.rikos.operacion.identidad.aplicacion.ResolverServicioVigente;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.PreTicket;
import pe.rikos.operacion.pensionistas.infraestructura.PreTicketRepositorio;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * US-019: Emite el pre-ticket de autoservicio del pensionista en el kiosco.
 * - Valida DNI y PIN del pensionista.
 * - Aplica bloqueo de 15 minutos al 5to intento fallido consecutivo (CA6).
 * - Resuelve el servicio vigente segun dia especial y horario semanal (CA2).
 * - Reutiliza pre-ticket pendiente del mismo turno si ya existe (RF-018).
 * - Genera codigo numerico de 6 digitos sin afectar saldo ni cuenta (CA3, CA4).
 */
@Service
public class EmitirPreTicket {

    public static final int MAX_INTENTOS_PIN = 5;
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private final PensionistaRepositorio pensionistas;
    private final PreTicketRepositorio preTickets;
    private final LocalRepositorio locales;
    private final ResolverServicioVigente resolverServicio;
    private final PasswordEncoder passwordEncoder;
    private final Clock reloj;
    private final HuellaPinPensionista huellas;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final java.security.SecureRandom azar = new java.security.SecureRandom();

    public EmitirPreTicket(PensionistaRepositorio pensionistas,
                           PreTicketRepositorio preTickets,
                           LocalRepositorio locales,
                           ResolverServicioVigente resolverServicio,
                           PasswordEncoder passwordEncoder,
                           Clock reloj, HuellaPinPensionista huellas, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.pensionistas = pensionistas;
        this.preTickets = preTickets;
        this.locales = locales;
        this.resolverServicio = resolverServicio;
        this.passwordEncoder = passwordEncoder;
        this.reloj = reloj;
        this.huellas = huellas;
        this.jdbc = jdbc;
    }

    @Transactional
    public Resultado<PreTicketEmitido, FalloPreTicket> ejecutar(SolicitudEmitirPreTicket solicitud) {
        // 1. Validar formato de DNI
        boolean legado = solicitud.dni() != null && !solicitud.dni().isBlank();
        if (legado && !solicitud.dni().matches("\\d{8}")) {
            return Resultado.fracaso(FalloPreTicket.de(ErrorPreTicket.DNI_INVALIDO));
        }
        if (solicitud.servicioManual() != null && !solicitud.servicioManual().isBlank()) {
            return Resultado.fracaso(FalloPreTicket.de(ErrorPreTicket.SERVICIO_MANUAL_NO_PERMITIDO));
        }
        if (!legado && (solicitud.pin() == null || !solicitud.pin().matches("\\d{" + GeneradorDePin.LONGITUD + "}"))) {
            return Resultado.fracaso(FalloPreTicket.de(ErrorPreTicket.CREDENCIALES_INVALIDAS));
        }

        // 2. Resolver Local
        if (solicitud.localCodigo() == null || solicitud.localCodigo().isBlank()) {
            return Resultado.fracaso(FalloPreTicket.de(ErrorPreTicket.LOCAL_NO_ENCONTRADO));
        }
        Optional<Local> localOpt = locales.findByCodigo(solicitud.localCodigo());
        if (localOpt.isEmpty()) {
            localOpt = locales.findById(solicitud.localCodigo());
        }
        if (localOpt.isEmpty()) {
            return Resultado.fracaso(FalloPreTicket.de(ErrorPreTicket.LOCAL_NO_ENCONTRADO));
        }
        Local local = localOpt.get();
        if (!local.isActivo()) {
            return Resultado.fracaso(FalloPreTicket.de(ErrorPreTicket.LOCAL_INACTIVO));
        }

        // 3. Buscar Pensionista por DNI
        Optional<Pensionista> pensionistaOpt = legado
                ? pensionistas.buscarPorDniParaActualizar(solicitud.dni())
                : pensionistas.buscarPorPinParaActualizar(huellas.de(solicitud.pin()));
        if (pensionistaOpt.isEmpty()) {
            return Resultado.fracaso(FalloPreTicket.de(ErrorPreTicket.CREDENCIALES_INVALIDAS));
        }
        Pensionista pensionista = pensionistaOpt.get();

        // Momento de operacion
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        LocalDateTime momentoLocal = solicitud.momento() != null
                ? solicitud.momento()
                : ahora.atZoneSameInstant(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toLocalDateTime();

        // 4. Validar si ya esta bloqueado
        if (pensionista.estaBloqueado(ahora)) {
            return Resultado.fracaso(FalloPreTicket.bloqueado(pensionista.segundosRestantesBloqueo(ahora)));
        }

        // 5. Validar estado activo
        if (!pensionista.isActivo()) {
            return Resultado.fracaso(FalloPreTicket.de(ErrorPreTicket.PENSIONISTA_INACTIVO));
        }

        // 6. Validar PIN
        if (solicitud.pin() == null || !passwordEncoder.matches(solicitud.pin(), pensionista.getHashPin())) {
            pensionista.registrarIntentoFallido(MAX_INTENTOS_PIN, DURACION_BLOQUEO, ahora);
            pensionistas.save(pensionista);
            if (pensionista.estaBloqueado(ahora)) {
                return Resultado.fracaso(FalloPreTicket.bloqueado(pensionista.segundosRestantesBloqueo(ahora)));
            } else {
                int restantes = Math.max(0, MAX_INTENTOS_PIN - pensionista.getIntentosFallidos());
                return Resultado.fracaso(FalloPreTicket.credencialesInvalidas(restantes));
            }
        }

        // PIN correcto: restablecer intentos fallidos
        if (pensionista.getIntentosFallidos() > 0) {
            pensionista.registrarIntentoExitoso();
            pensionistas.save(pensionista);
        }

        // 7. Resolver Servicio Vigente
        var franja = resolverServicio.franjaVigente(local.getId(), momentoLocal);
        if (franja.isEmpty()) {
            return Resultado.fracaso(FalloPreTicket.de(ErrorPreTicket.FUERA_DE_HORARIO));
        }
        String servicio = franja.get().servicio();
        OffsetDateTime finServicio = franja.get().fin().atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toOffsetDateTime();

        // 8. Reutilizar pre-ticket pendiente si ya existe para este turno (RF-018)
        Optional<PreTicket> existenteOpt = preTickets.findFirstByPensionistaIdAndLocalIdAndServicioAndEstadoOrderByEmitidoEnDesc(
                pensionista.getId(), local.getId(), servicio, PreTicket.ESTADO_PENDIENTE);

        if (existenteOpt.isPresent()) {
            PreTicket existente = existenteOpt.get();
            if (existente.estaPendiente(ahora) && !existente.getEmitidoEn().isBefore(
                    franja.get().inicio().atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toOffsetDateTime())) {
                // Tambien corrige tickets previos que se emitieron con cuatro horas de validez.
                existente.acotarVigencia(finServicio);
                return Resultado.exito(new PreTicketEmitido(
                        existente.getId(),
                        existente.getCodigo(),
                        pensionista.getId(),
                        pensionista.getNombreCompleto(),
                        pensionista.getDni(),
                        local.getId(),
                        local.getNombre(),
                        servicio,
                        existente.getEmitidoEn().toLocalDate(),
                        existente.getEmitidoEn(),
                        existente.getVenceEn(),
                        true
                ));
            } else {
                existente.marcarVencido();
                preTickets.save(existente);
            }
        }

        // 9. Generar nuevo pre-ticket
        jdbc.execute("SELECT pg_advisory_xact_lock(82647102)");
        String codigo = generarCodigoUnico();
        OffsetDateTime venceEn = finServicio;
        PreTicket nuevo = new PreTicket(
                Identificador.nuevo().valor(),
                codigo,
                pensionista.getId(),
                local.getId(),
                servicio,
                ahora,
                venceEn
        );
        preTickets.save(nuevo);

        return Resultado.exito(new PreTicketEmitido(
                nuevo.getId(),
                nuevo.getCodigo(),
                pensionista.getId(),
                pensionista.getNombreCompleto(),
                pensionista.getDni(),
                local.getId(),
                local.getNombre(),
                servicio,
                ahora.toLocalDate(),
                nuevo.getEmitidoEn(),
                nuevo.getVenceEn(),
                false
        ));
    }

    private String generarCodigoUnico() {
        for (int i = 0; i < 10; i++) {
            String codigo = String.format(java.util.Locale.ROOT, "%06d", azar.nextInt(1_000_000));
            if (!preTickets.existsByCodigo(codigo)) {
                return codigo;
            }
        }
        throw new IllegalStateException("No se pudo generar un codigo unico de pre-ticket");
    }
}

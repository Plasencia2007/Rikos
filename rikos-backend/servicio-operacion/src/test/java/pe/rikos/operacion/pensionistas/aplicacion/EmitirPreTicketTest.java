package pe.rikos.operacion.pensionistas.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.tiempo.ConfiguracionReloj;
import pe.rikos.operacion.identidad.aplicacion.ResolverServicioVigente;
import pe.rikos.operacion.identidad.infraestructura.Empresa;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.PreTicket;
import pe.rikos.operacion.pensionistas.infraestructura.PreTicketRepositorio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmitirPreTicketTest {

    private static final String DNI_VALIDO = "72345678";
    private static final String PIN_VALIDO = "1234";
    private static final String PIN_HASH = new BCryptPasswordEncoder().encode(PIN_VALIDO);
    private static final String LOCAL_CODIGO = "sanisidro";
    private static final String LOCAL_ID = "01M2QZ606763Q7XC00M8VD7WL1";

    @Mock
    private PensionistaRepositorio pensionistas;
    @Mock
    private PreTicketRepositorio preTickets;
    @Mock
    private LocalRepositorio locales;
    @Mock
    private ResolverServicioVigente resolverServicio;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private Clock relojFijo;
    private EmitirPreTicket caso;
    private Local local;

    @BeforeEach
    void preparar() {
        // Reloj fijado al 2026-10-12 a las 13:00 (hora de almuerzo en Lima)
        Instant instante = LocalDateTime.of(2026, 10, 12, 13, 0)
                .atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toInstant();
        relojFijo = Clock.fixed(instante, ConfiguracionReloj.ZONA_DEL_RESTAURANTE);

        caso = new EmitirPreTicket(pensionistas, preTickets, locales, resolverServicio, passwordEncoder, relojFijo);

        Empresa empresa = new Empresa("empresa-1", "20123456789", "RIKO'S S.A.C.", "RIKO'S", "Av. Principal 100");
        local = new Local(LOCAL_ID, empresa, LOCAL_CODIGO, "San Isidro", "Av. San Isidro 123", "actor-1");
    }

    private Pensionista pensionistaActivo() {
        Pensionista p = new Pensionista("pensionista-1", PIN_HASH, "actor-1");
        p.actualizarFicha(new Pensionista.Ficha(
                DNI_VALIDO, "Carlos", "Mendoza Ramos", "987654321", "carlos@correo.pe",
                "UNI", "20220001", "Sistemas", "6", "plan-1",
                java.util.EnumSet.of(Comida.ALMUERZO), "semanal"
        ), "actor-1");
        return p;
    }

    @Test
    void emiteNuevoPreTicketConExito() {
        Pensionista p = pensionistaActivo();
        when(locales.findByCodigo(LOCAL_CODIGO)).thenReturn(Optional.of(local));
        when(pensionistas.findByDni(DNI_VALIDO)).thenReturn(Optional.of(p));
        when(resolverServicio.resolver(eq(LOCAL_ID), any(LocalDateTime.class))).thenReturn(Optional.of("almuerzo"));
        when(preTickets.findFirstByPensionistaIdAndLocalIdAndServicioAndEstadoOrderByEmitidoEnDesc(
                p.getId(), LOCAL_ID, "almuerzo", PreTicket.ESTADO_PENDIENTE)).thenReturn(Optional.empty());

        SolicitudEmitirPreTicket solicitud = new SolicitudEmitirPreTicket(LOCAL_CODIGO, DNI_VALIDO, PIN_VALIDO);
        Resultado<PreTicketEmitido, FalloPreTicket> resultado = caso.ejecutar(solicitud);

        assertThat(resultado).isInstanceOf(Resultado.Exito.class);
        PreTicketEmitido emitido = ((Resultado.Exito<PreTicketEmitido, FalloPreTicket>) resultado).valor();
        assertThat(emitido.codigo()).matches("\\d{6}");
        assertThat(emitido.dni()).isEqualTo(DNI_VALIDO);
        assertThat(emitido.pensionistaNombre()).isEqualTo("Carlos Mendoza Ramos");
        assertThat(emitido.servicio()).isEqualTo("almuerzo");
        assertThat(emitido.reutilizado()).isFalse();
        assertThat(emitido.mensajeLegal()).contains("no constituye un consumo cargado");
        verify(preTickets).save(any(PreTicket.class));
    }

    @Test
    void reutilizaPreTicketPendienteExistente() {
        Pensionista p = pensionistaActivo();
        when(locales.findByCodigo(LOCAL_CODIGO)).thenReturn(Optional.of(local));
        when(pensionistas.findByDni(DNI_VALIDO)).thenReturn(Optional.of(p));
        when(resolverServicio.resolver(eq(LOCAL_ID), any(LocalDateTime.class))).thenReturn(Optional.of("almuerzo"));

        OffsetDateTime ahora = OffsetDateTime.now(relojFijo);
        PreTicket ticketExistente = new PreTicket("ticket-1", "381920", p.getId(), LOCAL_ID, "almuerzo",
                ahora.minusMinutes(10), ahora.plusHours(2));
        when(preTickets.findFirstByPensionistaIdAndLocalIdAndServicioAndEstadoOrderByEmitidoEnDesc(
                p.getId(), LOCAL_ID, "almuerzo", PreTicket.ESTADO_PENDIENTE)).thenReturn(Optional.of(ticketExistente));

        SolicitudEmitirPreTicket solicitud = new SolicitudEmitirPreTicket(LOCAL_CODIGO, DNI_VALIDO, PIN_VALIDO);
        Resultado<PreTicketEmitido, FalloPreTicket> resultado = caso.ejecutar(solicitud);

        assertThat(resultado).isInstanceOf(Resultado.Exito.class);
        PreTicketEmitido emitido = ((Resultado.Exito<PreTicketEmitido, FalloPreTicket>) resultado).valor();
        assertThat(emitido.codigo()).isEqualTo("381920");
        assertThat(emitido.reutilizado()).isTrue();
        verify(preTickets, never()).save(any(PreTicket.class));
    }

    @Test
    void rechazaSiDniTieneFormatoInvalido() {
        SolicitudEmitirPreTicket solicitud = new SolicitudEmitirPreTicket(LOCAL_CODIGO, "123", PIN_VALIDO);
        Resultado<PreTicketEmitido, FalloPreTicket> resultado = caso.ejecutar(solicitud);

        assertThat(resultado).isInstanceOf(Resultado.Fracaso.class);
        FalloPreTicket fallo = ((Resultado.Fracaso<PreTicketEmitido, FalloPreTicket>) resultado).error();
        assertThat(fallo.error()).isEqualTo(ErrorPreTicket.DNI_INVALIDO);
    }

    @Test
    void rechazaSiPensionistaNoExiste() {
        when(locales.findByCodigo(LOCAL_CODIGO)).thenReturn(Optional.of(local));
        when(pensionistas.findByDni(DNI_VALIDO)).thenReturn(Optional.empty());

        SolicitudEmitirPreTicket solicitud = new SolicitudEmitirPreTicket(LOCAL_CODIGO, DNI_VALIDO, PIN_VALIDO);
        Resultado<PreTicketEmitido, FalloPreTicket> resultado = caso.ejecutar(solicitud);

        assertThat(resultado).isInstanceOf(Resultado.Fracaso.class);
        FalloPreTicket fallo = ((Resultado.Fracaso<PreTicketEmitido, FalloPreTicket>) resultado).error();
        assertThat(fallo.error()).isEqualTo(ErrorPreTicket.CREDENCIALES_INVALIDAS);
    }

    @Test
    void fallaSiPinIncorrecto_incrementaIntentosFallidos() {
        Pensionista p = pensionistaActivo();
        when(locales.findByCodigo(LOCAL_CODIGO)).thenReturn(Optional.of(local));
        when(pensionistas.findByDni(DNI_VALIDO)).thenReturn(Optional.of(p));

        SolicitudEmitirPreTicket solicitud = new SolicitudEmitirPreTicket(LOCAL_CODIGO, DNI_VALIDO, "9999");
        Resultado<PreTicketEmitido, FalloPreTicket> resultado = caso.ejecutar(solicitud);

        assertThat(resultado).isInstanceOf(Resultado.Fracaso.class);
        FalloPreTicket fallo = ((Resultado.Fracaso<PreTicketEmitido, FalloPreTicket>) resultado).error();
        assertThat(fallo.error()).isEqualTo(ErrorPreTicket.CREDENCIALES_INVALIDAS);
        assertThat(fallo.intentosRestantes()).isEqualTo(4);
        assertThat(p.getIntentosFallidos()).isEqualTo(1);
        verify(pensionistas).save(p);
    }

    @Test
    void bloqueaPensionistaAlQuintoIntentoFallido() {
        Pensionista p = pensionistaActivo();
        // Simular 4 intentos fallidos previos
        p.registrarIntentoFallido(5, Duration.ofMinutes(15));
        p.registrarIntentoFallido(5, Duration.ofMinutes(15));
        p.registrarIntentoFallido(5, Duration.ofMinutes(15));
        p.registrarIntentoFallido(5, Duration.ofMinutes(15));
        assertThat(p.getIntentosFallidos()).isEqualTo(4);

        when(locales.findByCodigo(LOCAL_CODIGO)).thenReturn(Optional.of(local));
        when(pensionistas.findByDni(DNI_VALIDO)).thenReturn(Optional.of(p));

        SolicitudEmitirPreTicket solicitud = new SolicitudEmitirPreTicket(LOCAL_CODIGO, DNI_VALIDO, "9999");
        Resultado<PreTicketEmitido, FalloPreTicket> resultado = caso.ejecutar(solicitud);

        assertThat(resultado).isInstanceOf(Resultado.Fracaso.class);
        FalloPreTicket fallo = ((Resultado.Fracaso<PreTicketEmitido, FalloPreTicket>) resultado).error();
        assertThat(fallo.error()).isEqualTo(ErrorPreTicket.PENSIONISTA_BLOQUEADO);
        assertThat(fallo.segundosRestantes()).isGreaterThan(0);
        assertThat(p.estaBloqueado(OffsetDateTime.now(relojFijo))).isTrue();
        verify(pensionistas).save(p);
    }

    @Test
    void rechazaPensionistaPreviamenteBloqueadoSinVerificarPin() {
        Pensionista p = pensionistaActivo();
        p.registrarIntentoFallido(1, Duration.ofMinutes(15), OffsetDateTime.now(relojFijo)); // bloqueado
        when(locales.findByCodigo(LOCAL_CODIGO)).thenReturn(Optional.of(local));
        when(pensionistas.findByDni(DNI_VALIDO)).thenReturn(Optional.of(p));

        SolicitudEmitirPreTicket solicitud = new SolicitudEmitirPreTicket(LOCAL_CODIGO, DNI_VALIDO, PIN_VALIDO);
        Resultado<PreTicketEmitido, FalloPreTicket> resultado = caso.ejecutar(solicitud);

        assertThat(resultado).isInstanceOf(Resultado.Fracaso.class);
        FalloPreTicket fallo = ((Resultado.Fracaso<PreTicketEmitido, FalloPreTicket>) resultado).error();
        assertThat(fallo.error()).isEqualTo(ErrorPreTicket.PENSIONISTA_BLOQUEADO);
        assertThat(fallo.segundosRestantes()).isGreaterThan(0);
    }

    @Test
    void rechazaPensionistaInactivo() {
        Pensionista p = pensionistaActivo();
        p.cambiarEstado(false, "actor-admin");
        when(locales.findByCodigo(LOCAL_CODIGO)).thenReturn(Optional.of(local));
        when(pensionistas.findByDni(DNI_VALIDO)).thenReturn(Optional.of(p));

        SolicitudEmitirPreTicket solicitud = new SolicitudEmitirPreTicket(LOCAL_CODIGO, DNI_VALIDO, PIN_VALIDO);
        Resultado<PreTicketEmitido, FalloPreTicket> resultado = caso.ejecutar(solicitud);

        assertThat(resultado).isInstanceOf(Resultado.Fracaso.class);
        FalloPreTicket fallo = ((Resultado.Fracaso<PreTicketEmitido, FalloPreTicket>) resultado).error();
        assertThat(fallo.error()).isEqualTo(ErrorPreTicket.PENSIONISTA_INACTIVO);
    }

    @Test
    void rechazaSiEstaFueraDeHorario() {
        Pensionista p = pensionistaActivo();
        when(locales.findByCodigo(LOCAL_CODIGO)).thenReturn(Optional.of(local));
        when(pensionistas.findByDni(DNI_VALIDO)).thenReturn(Optional.of(p));
        when(resolverServicio.resolver(eq(LOCAL_ID), any(LocalDateTime.class))).thenReturn(Optional.empty());

        SolicitudEmitirPreTicket solicitud = new SolicitudEmitirPreTicket(LOCAL_CODIGO, DNI_VALIDO, PIN_VALIDO);
        Resultado<PreTicketEmitido, FalloPreTicket> resultado = caso.ejecutar(solicitud);

        assertThat(resultado).isInstanceOf(Resultado.Fracaso.class);
        FalloPreTicket fallo = ((Resultado.Fracaso<PreTicketEmitido, FalloPreTicket>) resultado).error();
        assertThat(fallo.error()).isEqualTo(ErrorPreTicket.FUERA_DE_HORARIO);
    }

    @Test
    void permiteServicioManualEnModoPrueba() {
        Pensionista p = pensionistaActivo();
        when(locales.findByCodigo(LOCAL_CODIGO)).thenReturn(Optional.of(local));
        when(pensionistas.findByDni(DNI_VALIDO)).thenReturn(Optional.of(p));
        when(preTickets.findFirstByPensionistaIdAndLocalIdAndServicioAndEstadoOrderByEmitidoEnDesc(
                p.getId(), LOCAL_ID, "cena", PreTicket.ESTADO_PENDIENTE)).thenReturn(Optional.empty());

        SolicitudEmitirPreTicket solicitud = new SolicitudEmitirPreTicket(LOCAL_CODIGO, DNI_VALIDO, PIN_VALIDO, "cena");
        Resultado<PreTicketEmitido, FalloPreTicket> resultado = caso.ejecutar(solicitud);

        assertThat(resultado).isInstanceOf(Resultado.Exito.class);
        PreTicketEmitido emitido = ((Resultado.Exito<PreTicketEmitido, FalloPreTicket>) resultado).valor();
        assertThat(emitido.servicio()).isEqualTo("cena");
        verify(preTickets).save(any(PreTicket.class));
    }
}

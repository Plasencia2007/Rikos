package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Salon;
import pe.rikos.operacion.venta.infraestructura.SalonRepositorio;

/** US-027: el administrador crea una mesa en el local activo. Queda en la bitacora. */
@Service
public class CrearMesa {

    private final MesaRepositorio mesas;
    private final SalonRepositorio salones;
    private final SalonPorDefecto salonPorDefecto;
    private final RegistradorAuditoria auditoria;

    public CrearMesa(MesaRepositorio mesas, SalonRepositorio salones, SalonPorDefecto salonPorDefecto,
                     RegistradorAuditoria auditoria) {
        this.mesas = mesas;
        this.salones = salones;
        this.salonPorDefecto = salonPorDefecto;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Mesa, ErrorMesa> ejecutar(DatosMesa datos, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorMesa.SIN_LOCAL_ACTIVO);
        }
        var invalido = ValidadorDeMesa.validar(datos);
        if (invalido.isPresent()) {
            return Resultado.fracaso(invalido.get());
        }
        String numero = ValidadorDeMesa.limpio(datos.numero());
        if (mesas.existsByLocalIdAndNumero(localId, numero)) {
            return Resultado.fracaso(ErrorMesa.NUMERO_DUPLICADO);
        }
        Salon salon = datos.salonId() == null ? salonPorDefecto.deLocal(localId, actorId)
                : salones.findByIdAndLocalId(datos.salonId(), localId).orElse(null);
        if (salon == null) {
            return Resultado.fracaso(ErrorMesa.SALON_NO_ENCONTRADO);
        }
        Mesa mesa = new Mesa(Identificador.nuevo().valor(), localId, numero, ValidadorDeMesa.nombreLimpio(datos),
                datos.capacidad(), actorId);
        mesa.configurar(salon.getId(), ValidadorDeMesa.forma(datos), datos.vip(), datos.zonaExterior(),
                datos.zonaFumadores(), ValidadorDeMesa.ancho(datos), ValidadorDeMesa.alto(datos), actorId);
        // Nace en un lugar libre de la cuadricula del plano (8 por fila); despues se acomoda en el editor.
        int orden = (int) mesas.countBySalonId(salon.getId());
        mesa.ubicar(60 + (orden % 8) * 130, 60 + (orden / 8) * 130, mesa.getAncho(), mesa.getAlto(), actorId);
        mesa = mesas.save(mesa);
        auditoria.registrar("cambio_mesa", "mesa", mesa.getId(), null,
                RegistradorAuditoria.campos("numero", numero, "nombre", mesa.getNombre(), "capacidad",
                        mesa.getCapacidad()));
        return Resultado.exito(mesa);
    }
}

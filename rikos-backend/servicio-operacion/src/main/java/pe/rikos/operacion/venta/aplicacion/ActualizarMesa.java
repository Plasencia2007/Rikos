package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.SalonRepositorio;

/** US-027: edita numero, nombre y capacidad de una mesa del local activo. Una mesa de otro local no se encuentra. */
@Service
public class ActualizarMesa {

    private final MesaRepositorio mesas;
    private final SalonRepositorio salones;
    private final RegistradorAuditoria auditoria;

    public ActualizarMesa(MesaRepositorio mesas, SalonRepositorio salones, RegistradorAuditoria auditoria) {
        this.mesas = mesas;
        this.salones = salones;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Mesa, ErrorMesa> ejecutar(String id, DatosMesa datos, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorMesa.SIN_LOCAL_ACTIVO);
        }
        Mesa mesa = mesas.findByIdAndLocalId(id, localId).orElse(null);
        if (mesa == null) {
            return Resultado.fracaso(ErrorMesa.NO_ENCONTRADA);
        }
        var invalido = ValidadorDeMesa.validar(datos);
        if (invalido.isPresent()) {
            return Resultado.fracaso(invalido.get());
        }
        String numero = ValidadorDeMesa.limpio(datos.numero());
        if (mesas.existsByLocalIdAndNumeroAndIdNot(localId, numero, id)) {
            return Resultado.fracaso(ErrorMesa.NUMERO_DUPLICADO);
        }
        var anterior = RegistradorAuditoria.campos("numero", mesa.getNumero(), "nombre", mesa.getNombre(),
                "capacidad", mesa.getCapacidad());
        // Sin salon en la peticion la mesa se queda donde esta; con uno, tiene que ser de este local.
        String salonId = mesa.getSalonId();
        if (datos.salonId() != null) {
            if (salones.findByIdAndLocalId(datos.salonId(), localId).isEmpty()) {
                return Resultado.fracaso(ErrorMesa.SALON_NO_ENCONTRADO);
            }
            salonId = datos.salonId();
        }
        mesa.actualizar(numero, ValidadorDeMesa.nombreLimpio(datos), datos.capacidad(), actorId);
        mesa.configurar(salonId, datos.forma() != null ? datos.forma() : mesa.getForma(), datos.vip(),
                datos.zonaExterior(), datos.zonaFumadores(),
                datos.ancho() != null ? datos.ancho() : mesa.getAncho(),
                datos.alto() != null ? datos.alto() : mesa.getAlto(), actorId);
        mesas.save(mesa);
        auditoria.registrar("cambio_mesa", "mesa", id, anterior,
                RegistradorAuditoria.campos("numero", mesa.getNumero(), "nombre", mesa.getNombre(), "capacidad",
                        mesa.getCapacidad()));
        return Resultado.exito(mesa);
    }
}

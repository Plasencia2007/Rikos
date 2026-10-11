package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Salon;
import pe.rikos.operacion.venta.infraestructura.SalonRepositorio;

/** Edita un salon del local activo. Un salon de otro local no se encuentra. */
@Service
public class ActualizarSalon {

    private final SalonRepositorio salones;
    private final RegistradorAuditoria auditoria;

    public ActualizarSalon(SalonRepositorio salones, RegistradorAuditoria auditoria) {
        this.salones = salones;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Salon, ErrorSalon> ejecutar(String id, DatosSalon datos, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorSalon.SIN_LOCAL_ACTIVO);
        }
        Salon salon = salones.findByIdAndLocalId(id, localId).orElse(null);
        if (salon == null) {
            return Resultado.fracaso(ErrorSalon.NO_ENCONTRADO);
        }
        var invalido = ValidadorDeSalon.validar(datos);
        if (invalido.isPresent()) {
            return Resultado.fracaso(invalido.get());
        }
        String nombre = ValidadorDeSalon.limpio(datos.nombre());
        if (salones.existsByLocalIdAndNombreAndIdNot(localId, nombre, id)) {
            return Resultado.fracaso(ErrorSalon.NOMBRE_DUPLICADO);
        }
        var anterior = RegistradorAuditoria.campos("nombre", salon.getNombre(), "accion", "editado");
        salon.actualizar(nombre, ValidadorDeSalon.limpio(datos.descripcion()), datos.icono(), datos.color(),
                ValidadorDeSalon.ancho(datos), ValidadorDeSalon.alto(datos), ValidadorDeSalon.colorPiso(datos), actorId);
        salones.save(salon);
        auditoria.registrar("cambio_mesa", "salon", id, anterior,
                RegistradorAuditoria.campos("nombre", nombre, "accion", "editado"));
        return Resultado.exito(salon);
    }
}

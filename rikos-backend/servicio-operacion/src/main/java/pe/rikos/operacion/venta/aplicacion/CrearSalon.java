package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Salon;
import pe.rikos.operacion.venta.infraestructura.SalonRepositorio;

/** El administrador crea un salon en el local activo. Queda en la bitacora. */
@Service
public class CrearSalon {

    private final SalonRepositorio salones;
    private final RegistradorAuditoria auditoria;

    public CrearSalon(SalonRepositorio salones, RegistradorAuditoria auditoria) {
        this.salones = salones;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Salon, ErrorSalon> ejecutar(DatosSalon datos, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorSalon.SIN_LOCAL_ACTIVO);
        }
        var invalido = ValidadorDeSalon.validar(datos);
        if (invalido.isPresent()) {
            return Resultado.fracaso(invalido.get());
        }
        String nombre = ValidadorDeSalon.limpio(datos.nombre());
        if (salones.existsByLocalIdAndNombre(localId, nombre)) {
            return Resultado.fracaso(ErrorSalon.NOMBRE_DUPLICADO);
        }
        Salon salon = salones.save(new Salon(Identificador.nuevo().valor(), localId, nombre,
                ValidadorDeSalon.limpio(datos.descripcion()), datos.icono(), datos.color(),
                ValidadorDeSalon.ancho(datos), ValidadorDeSalon.alto(datos), ValidadorDeSalon.colorPiso(datos), actorId));
        auditoria.registrar("cambio_mesa", "salon", salon.getId(), null,
                RegistradorAuditoria.campos("nombre", nombre, "accion", "creado"));
        return Resultado.exito(salon);
    }
}

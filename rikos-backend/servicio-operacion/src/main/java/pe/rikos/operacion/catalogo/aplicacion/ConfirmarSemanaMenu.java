package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenuRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.SemanaMenu;
import pe.rikos.operacion.catalogo.infraestructura.SemanaMenuRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

import java.util.List;

/**
 * US-013 CA4 / RN-041: borrador -> confirmado. Desde aqui el POS ve el menu.
 * Las repeticiones se devuelven junto con el exito: se avisan, nunca
 * bloquean. Se audita como cambio de estado de la semana.
 */
@Service
public class ConfirmarSemanaMenu {

    private final SemanaMenuRepositorio semanas;
    private final OpcionMenuRepositorio opciones;
    private final ConsultaMenu consulta;
    private final RegistradorAuditoria auditoria;

    public ConfirmarSemanaMenu(SemanaMenuRepositorio semanas, OpcionMenuRepositorio opciones, ConsultaMenu consulta,
                               RegistradorAuditoria auditoria) {
        this.semanas = semanas;
        this.opciones = opciones;
        this.consulta = consulta;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<List<ConsultaMenu.Advertencia>, ErrorProgramacion> ejecutar(String semanaId, String actorId) {
        SemanaMenu semana = semanas.findById(semanaId).orElse(null);
        if (semana == null) {
            return Resultado.fracaso(ErrorProgramacion.NO_ENCONTRADO);
        }
        if (semana.isConfirmada()) {
            return Resultado.fracaso(ErrorProgramacion.YA_CONFIRMADA);
        }
        if (!opciones.existsByDiaSemanaId(semanaId)) {
            return Resultado.fracaso(ErrorProgramacion.SEMANA_VACIA);
        }
        semana.confirmar(actorId);
        semanas.save(semana);
        String nombre = semana.getFechaInicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        auditoria.registrar("cambio_estado", "semana_menu", semanaId,
                RegistradorAuditoria.campos("estado", SemanaMenu.BORRADOR, "nombre", nombre),
                RegistradorAuditoria.campos("estado", SemanaMenu.CONFIRMADO, "nombre", nombre));
        return Resultado.exito(consulta.advertenciasDeLaSemana(semanaId));
    }
}

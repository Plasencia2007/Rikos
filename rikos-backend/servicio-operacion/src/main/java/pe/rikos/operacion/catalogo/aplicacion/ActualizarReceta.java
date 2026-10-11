package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Receta;
import pe.rikos.operacion.catalogo.infraestructura.RecetaRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * US-010 CA5, CA6: la receta maestra se edita en su lugar (sube version).
 * RN-045 -- "aplica solo a ventas nuevas" -- se cumple cuando la venta
 * copie lo que consumio (sprint 6); hasta entonces no hay ventas que
 * proteger. Cada cambio queda en la bitacora con el antes y el despues.
 */
@Service
public class ActualizarReceta {

    private final RecetaRepositorio recetas;
    private final ComposicionReceta composicion;
    private final CalculadoraCostoReceta calculadora;
    private final RegistradorAuditoria auditoria;

    public ActualizarReceta(RecetaRepositorio recetas, ComposicionReceta composicion,
                            CalculadoraCostoReceta calculadora, RegistradorAuditoria auditoria) {
        this.recetas = recetas;
        this.composicion = composicion;
        this.calculadora = calculadora;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Receta, FalloReceta> ejecutar(String recetaId, BigDecimal rendimiento, int porciones,
            List<ComposicionReceta.IngredienteEntrada> ingredientes, String actorId) {
        Receta receta = recetas.findById(recetaId).orElse(null);
        if (receta == null || !receta.isActivo()) {
            return Resultado.fracaso(FalloReceta.de(ErrorReceta.NO_ENCONTRADO));
        }
        var resueltos = composicion.resolver(receta.getProducto(), rendimiento, porciones, ingredientes);
        if (!resueltos.esExito()) {
            return Resultado.fracaso(((Resultado.Fracaso<?, FalloReceta>) resueltos).error());
        }
        Map<String, Object> antes = ComposicionReceta.foto(receta);
        receta.componer(rendimiento, porciones, resueltos.valorOLanza(), actorId);
        calculadora.recalcular(receta);
        Receta guardada = recetas.save(receta);
        calculadora.recalcularEnCascada(receta.getProducto().getId());
        auditoria.registrar("cambio_receta", "receta", recetaId, antes, ComposicionReceta.foto(guardada));
        return Resultado.exito(guardada);
    }
}

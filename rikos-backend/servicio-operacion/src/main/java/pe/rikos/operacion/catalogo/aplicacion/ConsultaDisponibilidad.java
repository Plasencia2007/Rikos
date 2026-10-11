package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.catalogo.infraestructura.DisponibilidadProducto;
import pe.rikos.operacion.catalogo.infraestructura.DisponibilidadProductoRepositorio;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * US-015 / RN-066: que no se ofrece en un local. Es una regla de consulta,
 * no de datos: el POS del sprint 6 la usa para ocultar en los pedidos
 * nuevos, y nada de lo que ya esta en una comanda la lee (CA2).
 */
@Component
public class ConsultaDisponibilidad {

    private final DisponibilidadProductoRepositorio disponibilidades;

    public ConsultaDisponibilidad(DisponibilidadProductoRepositorio disponibilidades) {
        this.disponibilidades = disponibilidades;
    }

    @Transactional(readOnly = true)
    public boolean estaAgotado(String localId, String productoId) {
        return disponibilidades.findByLocalIdAndProductoId(localId, productoId)
                .map(DisponibilidadProducto::isAgotado).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadProducto> agotadosDelLocal(String localId) {
        return disponibilidades.findAgotadosDelLocal(localId);
    }

    /** Los agotados del local activo de la sesion; vacio si no hay local activo. */
    @Transactional(readOnly = true)
    public Set<String> idsAgotadosDelLocalActivo() {
        Optional<String> local = LocalActivo.actual();
        return local.map(l -> agotadosDelLocal(l).stream().map(d -> d.getProducto().getId())
                .collect(Collectors.toSet())).orElse(Set.of());
    }
}

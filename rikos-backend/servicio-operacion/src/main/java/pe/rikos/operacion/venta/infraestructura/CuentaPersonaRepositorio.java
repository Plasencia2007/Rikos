package pe.rikos.operacion.venta.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuentaPersonaRepositorio extends JpaRepository<CuentaPersona, String> {

    List<CuentaPersona> findByPedidoIdOrderByOrden(String pedidoId);

    /** Las personas de varios pedidos de una vez (la lista del POS). */
    List<CuentaPersona> findByPedidoIdInOrderByOrden(java.util.Collection<String> pedidosIds);

    Optional<CuentaPersona> findByIdAndPedidoId(String id, String pedidoId);

    boolean existsByPedidoIdAndPensionistaId(String pedidoId, String pensionistaId);
}

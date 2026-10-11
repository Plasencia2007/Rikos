package pe.rikos.operacion.venta.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DetallePedidoComponenteRepositorio extends JpaRepository<DetallePedidoComponente, String> {

    List<DetallePedidoComponente> findByDetallePedidoIdIn(Collection<String> lineasIds);
}

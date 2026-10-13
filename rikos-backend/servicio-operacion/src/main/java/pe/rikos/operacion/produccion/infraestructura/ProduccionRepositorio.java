package pe.rikos.operacion.produccion.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.rikos.operacion.produccion.dominio.Produccion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProduccionRepositorio extends JpaRepository<Produccion, String>, JpaSpecificationExecutor<Produccion> {

    List<Produccion> findByLocalIdOrderByFechaDescCreadoEnDesc(String localId);

    List<Produccion> findByLocalIdAndFechaOrderByCreadoEnDesc(String localId, LocalDate fecha);

    List<Produccion> findByLocalIdAndProductoIdOrderByFechaDesc(String localId, String productoId);

    Optional<Produccion> findFirstByLocalIdAndProductoIdAndFechaAndServicio(String localId, String productoId, LocalDate fecha, String servicio);

    Optional<Produccion> findFirstByLocalIdAndProductoIdAndFechaOrderByCreadoEnDesc(String localId, String productoId, LocalDate fecha);
}

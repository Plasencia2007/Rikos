package pe.rikos.operacion.inventario.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoInventarioRepositorio extends JpaRepository<MovimientoInventario, String> {

    List<MovimientoInventario> findByLocalIdAndProductoIdOrderByOcurridoEnDesc(String localId, String productoId);

    List<MovimientoInventario> findByLocalIdOrderByOcurridoEnDesc(String localId);
}

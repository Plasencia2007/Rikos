package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresentacionCompraRepositorio extends JpaRepository<PresentacionCompra, String> {
    List<PresentacionCompra> findByProductoId(String productoId);

    boolean existsByProductoIdAndEsPredeterminadaTrueAndActivoTrue(String productoId);

    boolean existsByProductoId(String productoId);
}

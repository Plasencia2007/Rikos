package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversionUnidadRepositorio extends JpaRepository<ConversionUnidad, String> {
    List<ConversionUnidad> findByProductoId(String productoId);

    boolean existsByProductoIdAndUnidadOrigenIdAndUnidadDestinoId(String productoId, String unidadOrigenId,
            String unidadDestinoId);
}

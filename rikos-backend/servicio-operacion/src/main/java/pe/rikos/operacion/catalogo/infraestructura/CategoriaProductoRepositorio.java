package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaProductoRepositorio extends JpaRepository<CategoriaProducto, String> {

    /** Es hoja si esto es false: nadie cuelga de ella (4.1, regla de hoja). */
    boolean existsByCategoriaPadreIdAndActivoTrue(String categoriaPadreId);

    List<CategoriaProducto> findByCategoriaPadreIdIsNullOrderByOrden();

    List<CategoriaProducto> findByCategoriaPadreIdOrderByOrden(String categoriaPadreId);
}

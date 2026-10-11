package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaProductoRepositorio extends JpaRepository<CategoriaProducto, String> {

    /** Es hoja si esto es false: nadie cuelga de ella (4.1, regla de hoja). */
    boolean existsByCategoriaPadreIdAndActivoTrue(String categoriaPadreId);

    // Sin el filtro de activo: para saber si un borrado real chocaria con
    // el ON DELETE RESTRICT de categoria_padre_id (a la base de datos no le
    // importa si la hija esta activa, la fila sigue ahi).
    boolean existsByCategoriaPadreId(String categoriaPadreId);

    List<CategoriaProducto> findByCategoriaPadreIdIsNullOrderByOrden();

    List<CategoriaProducto> findByCategoriaPadreIdOrderByOrden(String categoriaPadreId);
}

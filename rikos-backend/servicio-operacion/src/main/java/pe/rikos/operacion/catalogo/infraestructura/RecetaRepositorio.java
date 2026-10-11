package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RecetaRepositorio extends JpaRepository<Receta, String> {

    Optional<Receta> findByProductoIdAndActivoTrue(String productoId);

    /** Recalculo en cascada: las recetas activas que usan este producto como ingrediente. */
    @Query("select distinct i.receta from IngredienteReceta i where i.producto.id = :productoId and i.receta.activo = true")
    List<Receta> findActivasQueUsan(String productoId);

    boolean existsByProductoId(String productoId);

    @Query("select count(i) > 0 from IngredienteReceta i where i.producto.id = :productoId")
    boolean usadoComoIngrediente(String productoId);
}

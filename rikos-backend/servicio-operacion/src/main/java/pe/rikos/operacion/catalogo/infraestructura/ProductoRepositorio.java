package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductoRepositorio extends JpaRepository<Producto, String> {
    boolean existsByCodigo(String codigo);

    // 4.1: antes de dejar que una categoria tenga hijas hay que saber si ya
    // le colgaron productos directamente -- si los tiene, dejaria de ser
    // hoja y esos productos quedarian huerfanos de la regla de hoja.
    boolean existsByCategoriaIdAndActivoTrue(String categoriaId);

    // Sin el filtro de activo: un producto desactivado sigue referenciando
    // la categoria (producto.categoria_id ON DELETE RESTRICT), asi que un
    // borrado real de la categoria debe seguir bloqueado aunque el
    // producto ya no este activo.
    boolean existsByCategoriaId(String categoriaId);

    // Conteo por categoria para la pantalla de Categorias (evita una
    // consulta por fila): [0] = categoria_id, [1] = total activo.
    @Query("select p.categoria.id, count(p) from Producto p where p.activo = true group by p.categoria.id")
    List<Object[]> contarActivosPorCategoria();

    /** US-014: el menu normal o el extra ('normal' | 'extra'). */
    Optional<Producto> findByTipoMenu(String tipoMenu);

    /**
     * La carta del POS: vendibles activos con precio y sin marca de menu, de las categorias de producto fijo, extra y de
     * componente de menu (entradas, segundos, guarniciones, refrescos y postres: tambien se piden sueltos). Los insumos y
     * las subrecetas no se venden.
     */
    @Query("select p from Producto p where p.esVendible = true and p.activo = true and p.tipoMenu is null "
            + "and p.precioVenta is not null "
            + "and p.categoria.naturaleza in ('producto_fijo', 'extra', 'componente_menu') "
            + "order by p.nombre")
    List<Producto> carta();
}

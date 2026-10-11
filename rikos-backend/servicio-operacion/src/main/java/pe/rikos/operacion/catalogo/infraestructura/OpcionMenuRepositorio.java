package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface OpcionMenuRepositorio extends JpaRepository<OpcionMenu, String> {

    /** Orden del menu (entrada, segundo, guarnicion, refresco, postre) y, dentro, el normal antes que el extra. */
    String ORDEN = "case o.componente when 'entrada' then 1 when 'segundo' then 2 when 'guarnicion' then 3 "
            + "when 'refresco' then 4 else 5 end, o.tipoMenu desc, p.nombre";

    boolean existsByDiaIdAndProductoId(String diaId, String productoId);

    @Query("select o from OpcionMenu o join fetch o.producto p join fetch o.dia d where d.semana.id = :semanaId "
            + "order by d.fecha, " + ORDEN)
    List<OpcionMenu> findDeLaSemana(String semanaId);

    @Query("select o from OpcionMenu o join fetch o.producto p join fetch o.dia d where d.fecha = :fecha "
            + "order by " + ORDEN)
    List<OpcionMenu> findDelDia(LocalDate fecha);

    /** RN-041: los segundos programados entre dos fechas, para la advertencia por repeticion. */
    @Query("select o from OpcionMenu o join fetch o.producto join fetch o.dia d where o.componente = 'segundo' "
            + "and d.fecha between :desde and :hasta")
    List<OpcionMenu> findSegundosEntre(LocalDate desde, LocalDate hasta);

    boolean existsByDiaSemanaId(String semanaId);

    /** Un producto que ya se programo alguna vez no se borra del catalogo (FK RESTRICT). */
    boolean existsByProductoId(String productoId);
}

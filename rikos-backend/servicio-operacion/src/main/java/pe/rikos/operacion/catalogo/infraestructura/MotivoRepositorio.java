package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MotivoRepositorio extends JpaRepository<Motivo, String> {

    /** Mirror de la parte del indice unico parcial ux_motivo_tipo_descripcion_activo. */
    boolean existsByTipoAndDescripcionAndActivoTrue(String tipo, String descripcion);

    List<Motivo> findAllByOrderByTipoAscDescripcionAsc();

    long countByTipoAndActivoTrue(String tipo);

    List<Motivo> findByTipoOrderByDescripcionAsc(String tipo);
}

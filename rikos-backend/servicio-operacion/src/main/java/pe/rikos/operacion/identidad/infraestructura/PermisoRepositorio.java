package pe.rikos.operacion.identidad.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermisoRepositorio extends JpaRepository<Permiso, String> {

    Optional<Permiso> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Permiso> findByIdIn(List<String> ids);

    List<Permiso> findAllByOrderByModuloAscAccionAsc();
}

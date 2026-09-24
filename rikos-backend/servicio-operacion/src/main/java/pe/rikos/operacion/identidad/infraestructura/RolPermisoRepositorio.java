package pe.rikos.operacion.identidad.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolPermisoRepositorio extends JpaRepository<RolPermiso, RolPermisoId> {

    List<RolPermiso> findByRolId(String rolId);

    List<RolPermiso> findByRolIdIn(List<String> rolesIds);

    void deleteByRolId(String rolId);
}

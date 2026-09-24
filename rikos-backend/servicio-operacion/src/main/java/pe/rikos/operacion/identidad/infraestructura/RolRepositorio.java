package pe.rikos.operacion.identidad.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepositorio extends JpaRepository<Rol, String> {

    Optional<Rol> findByNombre(String nombre);

    List<Rol> findByIdIn(List<String> ids);
}

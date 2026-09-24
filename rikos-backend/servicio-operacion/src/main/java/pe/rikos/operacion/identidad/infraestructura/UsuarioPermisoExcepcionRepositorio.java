package pe.rikos.operacion.identidad.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioPermisoExcepcionRepositorio
        extends JpaRepository<UsuarioPermisoExcepcion, UsuarioPermisoExcepcionId> {

    List<UsuarioPermisoExcepcion> findByUsuarioId(String usuarioId);
}

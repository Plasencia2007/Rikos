package pe.rikos.operacion.identidad.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioRolRepositorio extends JpaRepository<UsuarioRol, UsuarioRolId> {

    List<UsuarioRol> findByUsuarioId(String usuarioId);

    void deleteByUsuarioId(String usuarioId);
}

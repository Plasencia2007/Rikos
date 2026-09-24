package pe.rikos.operacion.identidad.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, String> {

    boolean existsByNombreUsuario(String nombreUsuario);

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    List<Usuario> findAllByOrderByApellidosAscNombresAsc();
}

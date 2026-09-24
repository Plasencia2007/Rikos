package pe.rikos.operacion.identidad.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SesionRepositorio extends JpaRepository<Sesion, String> {

    List<Sesion> findByUsuarioIdOrderByIniciadaEnDesc(String usuarioId);

    boolean existsByLocalId(String localId);
}

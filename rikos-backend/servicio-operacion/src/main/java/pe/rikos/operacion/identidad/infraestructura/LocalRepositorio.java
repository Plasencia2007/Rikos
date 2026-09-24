package pe.rikos.operacion.identidad.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocalRepositorio extends JpaRepository<Local, String> {

    boolean existsByCodigo(String codigo);

    Optional<Local> findByCodigo(String codigo);

    List<Local> findAllByOrderByNombreAsc();
}

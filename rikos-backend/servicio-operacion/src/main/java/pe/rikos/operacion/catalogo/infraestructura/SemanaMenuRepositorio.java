package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SemanaMenuRepositorio extends JpaRepository<SemanaMenu, String> {

    Optional<SemanaMenu> findByFechaInicio(LocalDate fechaInicio);

    boolean existsByFechaInicio(LocalDate fechaInicio);
}

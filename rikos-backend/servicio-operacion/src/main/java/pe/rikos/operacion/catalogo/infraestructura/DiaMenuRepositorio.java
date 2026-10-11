package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaMenuRepositorio extends JpaRepository<DiaMenu, String> {

    Optional<DiaMenu> findByFecha(LocalDate fecha);

    List<DiaMenu> findBySemanaId(String semanaId);
}

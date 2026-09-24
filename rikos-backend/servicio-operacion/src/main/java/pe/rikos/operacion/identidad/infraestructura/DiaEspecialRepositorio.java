package pe.rikos.operacion.identidad.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaEspecialRepositorio extends JpaRepository<DiaEspecial, String> {

    List<DiaEspecial> findByLocalIdOrderByFecha(String localId);

    Optional<DiaEspecial> findByLocalIdAndFecha(String localId, LocalDate fecha);

    boolean existsByLocalIdAndFecha(String localId, LocalDate fecha);
}

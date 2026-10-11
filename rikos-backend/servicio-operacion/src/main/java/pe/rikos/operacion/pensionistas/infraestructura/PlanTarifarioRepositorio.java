package pe.rikos.operacion.pensionistas.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanTarifarioRepositorio extends JpaRepository<PlanTarifario, String> {

    boolean existsByNombre(String nombre);

    List<PlanTarifario> findAllByOrderByNombreAsc();
}

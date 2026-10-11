package pe.rikos.operacion.pensionistas.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VersionPlanRepositorio extends JpaRepository<VersionPlan, String> {

    /** Las versiones de un plan, la mas nueva primero. */
    List<VersionPlan> findByPlanIdOrderByVigenteDesdeDesc(String planId);

    /** La ultima version (la abierta): la unica con vigente_hasta nulo. */
    Optional<VersionPlan> findFirstByPlanIdOrderByVigenteDesdeDesc(String planId);
}

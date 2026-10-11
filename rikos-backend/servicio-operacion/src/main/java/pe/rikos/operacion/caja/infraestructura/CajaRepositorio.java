package pe.rikos.operacion.caja.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CajaRepositorio extends JpaRepository<Caja, String> {

    List<Caja> findByLocalIdOrderByNombre(String localId);

    /** Una caja de otro local no se encuentra: nunca se filtra que existe. */
    Optional<Caja> findByIdAndLocalId(String id, String localId);

    boolean existsByLocalIdAndNombreIgnoreCase(String localId, String nombre);
}

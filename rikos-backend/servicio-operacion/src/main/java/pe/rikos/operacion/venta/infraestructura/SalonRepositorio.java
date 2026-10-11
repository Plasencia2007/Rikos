package pe.rikos.operacion.venta.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalonRepositorio extends JpaRepository<Salon, String> {

    List<Salon> findByLocalIdOrderByNombre(String localId);

    /** Un salon de otro local no se encuentra: nunca se filtra que existe. */
    Optional<Salon> findByIdAndLocalId(String id, String localId);

    Optional<Salon> findByLocalIdAndNombre(String localId, String nombre);

    boolean existsByLocalIdAndNombre(String localId, String nombre);

    boolean existsByLocalIdAndNombreAndIdNot(String localId, String nombre, String id);
}

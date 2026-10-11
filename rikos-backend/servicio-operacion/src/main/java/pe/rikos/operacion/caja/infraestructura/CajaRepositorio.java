package pe.rikos.operacion.caja.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CajaRepositorio extends JpaRepository<Caja, String> {

    List<Caja> findByLocalIdOrderByNombre(String localId);

    /** Una caja de otro local no se encuentra: nunca se filtra que existe. */
    Optional<Caja> findByIdAndLocalId(String id, String localId);

    /** Crea la caja si no existe: dos primeras consultas a la vez no chocan (la segunda no hace nada). */
    @Modifying
    @Query(value = "INSERT INTO caja (id, local_id, nombre, activo, version, creado_en, creado_por) "
            + "VALUES (:id, :localId, :nombre, TRUE, 0, now(), :creadoPor) ON CONFLICT (local_id, lower(nombre)) DO NOTHING",
            nativeQuery = true)
    int insertarSiNoExiste(String id, String localId, String nombre, String creadoPor);

    boolean existsByLocalIdAndNombreIgnoreCase(String localId, String nombre);
}

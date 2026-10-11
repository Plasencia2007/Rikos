package pe.rikos.operacion.caja.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CajaRepositorio extends JpaRepository<Caja, String> {

    List<Caja> findByLocalIdOrderByNombre(String localId);

    /** Para listar: la principal primero y luego por codigo. */
    List<Caja> findByLocalIdOrderByPrincipalDescCodigoAsc(String localId);

    /** Una caja de otro local no se encuentra: nunca se filtra que existe. */
    Optional<Caja> findByIdAndLocalId(String id, String localId);

    /** Crea la caja si no existe: dos primeras consultas a la vez no chocan (la segunda no hace nada). */
    @Modifying
    @Query(value = "INSERT INTO caja (id, local_id, nombre, codigo, principal, activo, version, creado_en, creado_por) "
            + "VALUES (:id, :localId, :nombre, :codigo, TRUE, TRUE, 0, now(), :creadoPor) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int insertarSiNoExiste(String id, String localId, String nombre, String codigo, String creadoPor);

    boolean existsByLocalIdAndCodigoIgnoreCase(String localId, String codigo);

    boolean existsByLocalIdAndNombreIgnoreCaseAndIdNot(String localId, String nombre, String id);

    boolean existsByLocalIdAndCodigoIgnoreCaseAndIdNot(String localId, String codigo, String id);

    boolean existsByLocalIdAndNombreIgnoreCase(String localId, String nombre);
}

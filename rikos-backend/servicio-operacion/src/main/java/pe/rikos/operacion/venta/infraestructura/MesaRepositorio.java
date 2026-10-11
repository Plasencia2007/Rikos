package pe.rikos.operacion.venta.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MesaRepositorio extends JpaRepository<Mesa, String> {

    List<Mesa> findByLocalIdOrderByNumero(String localId);

    /** Una mesa de otro local no se encuentra: nunca se filtra que existe. */
    Optional<Mesa> findByIdAndLocalId(String id, String localId);

    boolean existsByLocalIdAndNumero(String localId, String numero);

    List<Mesa> findBySalonId(String salonId);

    boolean existsBySalonId(String salonId);

    /** Hay mesas unidas a esta (es una principal). */
    boolean existsByMesaPrincipalId(String principalId);

    /** Las mesas unidas a esta (para separarlas al terminar el pedido). */
    List<Mesa> findByMesaPrincipalId(String principalId);

    long countBySalonId(String salonId);

    boolean existsByLocalIdAndNumeroAndIdNot(String localId, String numero, String id);
}

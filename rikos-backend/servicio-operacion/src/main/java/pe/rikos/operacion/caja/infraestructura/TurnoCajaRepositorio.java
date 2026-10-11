package pe.rikos.operacion.caja.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TurnoCajaRepositorio extends JpaRepository<TurnoCaja, String> {

    Optional<TurnoCaja> findByCajaIdAndEstado(String cajaId, String estado);

    Optional<TurnoCaja> findByCajeroIdAndEstado(String cajeroId, String estado);

    List<TurnoCaja> findByCajaIdInAndEstado(Collection<String> cajaIds, String estado);
}

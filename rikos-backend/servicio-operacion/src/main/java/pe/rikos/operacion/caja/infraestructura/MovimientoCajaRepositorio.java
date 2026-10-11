package pe.rikos.operacion.caja.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoCajaRepositorio extends JpaRepository<MovimientoCaja, String> {

    List<MovimientoCaja> findByTurnoCajaId(String turnoCajaId);
}

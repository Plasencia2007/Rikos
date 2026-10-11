package pe.rikos.operacion.caja.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CierreCajaRepositorio extends JpaRepository<CierreCaja, String> {

    Optional<CierreCaja> findByTurnoCajaId(String turnoCajaId);
}

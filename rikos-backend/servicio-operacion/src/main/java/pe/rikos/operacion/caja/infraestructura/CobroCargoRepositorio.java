package pe.rikos.operacion.caja.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CobroCargoRepositorio extends JpaRepository<CobroCargo, String> {

    List<CobroCargo> findByCobroId(String cobroId);

    List<CobroCargo> findByCobroIdIn(java.util.Collection<String> cobrosIds);
}

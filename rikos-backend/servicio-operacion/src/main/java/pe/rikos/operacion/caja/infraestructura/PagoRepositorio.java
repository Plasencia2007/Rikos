package pe.rikos.operacion.caja.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepositorio extends JpaRepository<Pago, String> {

    List<Pago> findByCobroIdIn(java.util.Collection<String> cobrosIds);
}

package pe.rikos.operacion.venta.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DetalleComandaRepositorio extends JpaRepository<DetalleComanda, String> {

    List<DetalleComanda> findByComandaId(String comandaId);

    List<DetalleComanda> findByComandaIdIn(Collection<String> comandasIds);
}

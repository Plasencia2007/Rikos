package pe.rikos.operacion.inventario.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ConsumoInternoRepositorio extends JpaRepository<ConsumoInterno, String> {

    List<ConsumoInterno> findByLocalIdOrderByFechaDescCreadoEnDesc(String localId);

    List<ConsumoInterno> findByLocalIdAndFechaOrderByCreadoEnDesc(String localId, LocalDate fecha);

    long countByLocalIdAndUsuarioIdAndFechaAndServicio(String localId, String usuarioId, LocalDate fecha, String servicio);
}

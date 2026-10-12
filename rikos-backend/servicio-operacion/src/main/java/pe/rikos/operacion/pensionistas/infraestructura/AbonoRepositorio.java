package pe.rikos.operacion.pensionistas.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbonoRepositorio extends JpaRepository<Abono, String> {

    List<Abono> findByTurnoCajaIdOrderByCreadoEnDesc(String turnoCajaId);

    List<Abono> findByPensionistaIdOrderByCreadoEnDesc(String pensionistaId);

    boolean existsByNumeroOperacionAndMedio(String numeroOperacion, String medio);

    List<Abono> findByEstadoOrderByCreadoEnAsc(String estado);

    List<Abono> findByEstadoOrderByCreadoEnDesc(String estado);

    List<Abono> findAllByOrderByCreadoEnDesc();

    long countByEstado(String estado);
}

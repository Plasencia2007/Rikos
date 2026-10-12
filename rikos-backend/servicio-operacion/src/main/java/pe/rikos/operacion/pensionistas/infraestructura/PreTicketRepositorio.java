package pe.rikos.operacion.pensionistas.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreTicketRepositorio extends JpaRepository<PreTicket, String> {

    Optional<PreTicket> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    Optional<PreTicket> findFirstByPensionistaIdAndLocalIdAndServicioAndEstadoOrderByEmitidoEnDesc(
            String pensionistaId, String localId, String servicio, String estado);
}

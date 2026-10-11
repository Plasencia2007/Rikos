package pe.rikos.operacion.caja.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CobroRepositorio extends JpaRepository<Cobro, String> {

    /** Un cobro de otro local no se encuentra: nunca se filtra que existe. */
    java.util.Optional<Cobro> findByIdAndLocalId(String id, String localId);

    List<Cobro> findByPedidoIdOrderByCobradoEn(String pedidoId);

    /** Los cobros de un turno, el mas reciente primero. */
    List<Cobro> findByTurnoCajaIdOrderByCobradoEnDesc(String turnoCajaId);
}

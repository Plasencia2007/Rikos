package pe.rikos.operacion.caja.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CobroRepositorio extends JpaRepository<Cobro, String> {

    List<Cobro> findByPedidoIdOrderByCobradoEn(String pedidoId);
}

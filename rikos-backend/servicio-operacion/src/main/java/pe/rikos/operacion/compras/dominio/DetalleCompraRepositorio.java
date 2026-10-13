package pe.rikos.operacion.compras.dominio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleCompraRepositorio extends JpaRepository<DetalleCompra, String> {

    List<DetalleCompra> findByCompraId(String compraId);
}

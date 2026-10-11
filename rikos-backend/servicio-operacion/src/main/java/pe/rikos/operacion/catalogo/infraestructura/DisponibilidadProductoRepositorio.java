package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DisponibilidadProductoRepositorio extends JpaRepository<DisponibilidadProducto, String> {

    Optional<DisponibilidadProducto> findByLocalIdAndProductoId(String localId, String productoId);

    /** Lo que el POS de este local no debe ofrecer (RN-066). */
    @Query("select d from DisponibilidadProducto d join fetch d.producto where d.localId = :localId and d.agotado = true")
    List<DisponibilidadProducto> findAgotadosDelLocal(String localId);

    boolean existsByProductoId(String productoId);
}

package pe.rikos.operacion.compras.dominio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepositorio extends JpaRepository<Compra, String> {

    List<Compra> findByLocalIdOrderByFechaDocumentoDescCreadoEnDesc(String localId);

    List<Compra> findByLocalIdAndEstadoOrderByFechaDocumentoDescCreadoEnDesc(String localId, EstadoCompra estado);

    Optional<Compra> findByIdAndLocalId(String id, String localId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Compra c " +
           "WHERE c.proveedor.id = :proveedorId " +
           "  AND c.tipoDocumento = :tipoDocumento " +
           "  AND c.serie = :serie " +
           "  AND c.numero = :numero " +
           "  AND c.estado != :estadoExcluido " +
           "  AND (:idExcluido IS NULL OR c.id != :idExcluido)")
    boolean existeComprobanteDuplicado(
            @Param("proveedorId") String proveedorId,
            @Param("tipoDocumento") TipoDocumentoCompra tipoDocumento,
            @Param("serie") String serie,
            @Param("numero") String numero,
            @Param("estadoExcluido") EstadoCompra estadoExcluido,
            @Param("idExcluido") String idExcluido);
}

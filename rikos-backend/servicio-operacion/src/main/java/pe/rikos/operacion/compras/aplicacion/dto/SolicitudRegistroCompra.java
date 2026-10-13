package pe.rikos.operacion.compras.aplicacion.dto;

import pe.rikos.operacion.compras.dominio.TipoDocumentoCompra;

import java.time.LocalDate;
import java.util.List;

public record SolicitudRegistroCompra(
        String proveedorId,
        TipoDocumentoCompra tipoDocumento,
        String serie,
        String numero,
        LocalDate fechaDocumento,
        String observacion,
        boolean confirmarInmediatamente,
        List<ItemDetalleCompraDto> detalles
) {}

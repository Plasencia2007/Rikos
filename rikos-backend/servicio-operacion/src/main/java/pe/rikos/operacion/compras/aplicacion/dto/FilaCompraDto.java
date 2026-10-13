package pe.rikos.operacion.compras.aplicacion.dto;

import pe.rikos.operacion.compras.dominio.EstadoCompra;
import pe.rikos.operacion.compras.dominio.TipoDocumentoCompra;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record FilaCompraDto(
        String id,
        String localId,
        String proveedorId,
        String proveedorRazonSocial,
        String proveedorRuc,
        TipoDocumentoCompra tipoDocumento,
        String serie,
        String numero,
        LocalDate fechaDocumento,
        Long total,
        String rutaAdjunto,
        EstadoCompra estado,
        OffsetDateTime confirmadaEn,
        String observacion,
        OffsetDateTime creadoEn,
        List<FilaDetalleCompraDto> detalles
) {}

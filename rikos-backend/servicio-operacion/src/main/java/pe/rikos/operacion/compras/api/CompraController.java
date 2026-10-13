package pe.rikos.operacion.compras.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.compras.aplicacion.ConfirmarCompra;
import pe.rikos.operacion.compras.aplicacion.ConsultarCompras;
import pe.rikos.operacion.compras.aplicacion.ErrorCompra;
import pe.rikos.operacion.compras.aplicacion.GestionarAdjuntoCompra;
import pe.rikos.operacion.compras.aplicacion.RegistrarCompra;
import pe.rikos.operacion.compras.aplicacion.dto.FilaCompraDto;
import pe.rikos.operacion.compras.aplicacion.dto.SolicitudRegistroCompra;
import pe.rikos.operacion.compras.dominio.EstadoCompra;
import pe.rikos.operacion.comun.web.ErrorRespuesta;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/locales/{localId}/compras")
@Tag(name = "Compras", description = "Gestión y registro de compras de inventario (US-050)")
public class CompraController {

    private final ConsultarCompras consultarCompras;
    private final RegistrarCompra registrarCompra;
    private final ConfirmarCompra confirmarCompra;
    private final GestionarAdjuntoCompra gestionarAdjuntoCompra;

    public CompraController(ConsultarCompras consultarCompras, RegistrarCompra registrarCompra,
                            ConfirmarCompra confirmarCompra, GestionarAdjuntoCompra gestionarAdjuntoCompra) {
        this.consultarCompras = consultarCompras;
        this.registrarCompra = registrarCompra;
        this.confirmarCompra = confirmarCompra;
        this.gestionarAdjuntoCompra = gestionarAdjuntoCompra;
    }

    @Operation(summary = "Listar compras de un local con filtro opcional de estado")
    @GetMapping
    public ResponseEntity<?> listar(@PathVariable String localId,
                                    @RequestParam(required = false) String estado) {
        EstadoCompra estadoEnum = null;
        if (estado != null && !estado.isBlank()) {
            try {
                estadoEnum = EstadoCompra.desdeCodigo(estado);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(new ErrorRespuesta("ESTADO_INVALIDO"));
            }
        }
        Resultado<List<FilaCompraDto>, ErrorCompra> res = consultarCompras.listarPorLocal(localId, estadoEnum);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<List<FilaCompraDto>, ErrorCompra>) res).error());
        }
        return ResponseEntity.ok(res.valorOLanza());
    }

    @Operation(summary = "Obtener detalle completo de una compra por ID")
    @GetMapping("/{compraId}")
    public ResponseEntity<?> obtenerPorId(@PathVariable String localId, @PathVariable String compraId) {
        Resultado<FilaCompraDto, ErrorCompra> res = consultarCompras.obtenerPorId(localId, compraId);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<FilaCompraDto, ErrorCompra>) res).error());
        }
        return ResponseEntity.ok(res.valorOLanza());
    }

    @Operation(summary = "Registrar una nueva compra (borrador o confirmación directa)")
    @PostMapping
    public ResponseEntity<?> registrar(@PathVariable String localId,
                                       @RequestBody SolicitudRegistroCompra solicitud,
                                       @AuthenticationPrincipal String usuarioId) {
        Resultado<FilaCompraDto, ErrorCompra> res = registrarCompra.ejecutar(localId, solicitud, usuarioId);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<FilaCompraDto, ErrorCompra>) res).error());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(res.valorOLanza());
    }

    @Operation(summary = "Confirmar una compra en borrador")
    @PostMapping("/{compraId}/confirmar")
    public ResponseEntity<?> confirmar(@PathVariable String localId,
                                       @PathVariable String compraId,
                                       @AuthenticationPrincipal String usuarioId) {
        Resultado<FilaCompraDto, ErrorCompra> res = confirmarCompra.ejecutar(localId, compraId, usuarioId);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<FilaCompraDto, ErrorCompra>) res).error());
        }
        return ResponseEntity.ok(res.valorOLanza());
    }

    @Operation(summary = "Subir y adjuntar archivo comprobante (factura/boleta)")
    @PostMapping("/{compraId}/adjunto")
    public ResponseEntity<?> subirAdjunto(@PathVariable String localId,
                                          @PathVariable String compraId,
                                          @RequestParam("archivo") MultipartFile archivo) {
        Resultado<String, ErrorCompra> res = gestionarAdjuntoCompra.guardarAdjunto(localId, compraId, archivo);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<String, ErrorCompra>) res).error());
        }
        return ResponseEntity.ok(Map.of("rutaAdjunto", res.valorOLanza()));
    }

    @Operation(summary = "Descargar o visualizar el archivo comprobante adjunto")
    @GetMapping("/{compraId}/adjunto")
    public ResponseEntity<?> descargarAdjunto(@PathVariable String localId,
                                              @PathVariable String compraId) {
        Resultado<Resource, ErrorCompra> res = gestionarAdjuntoCompra.obtenerAdjunto(localId, compraId);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<Resource, ErrorCompra>) res).error());
        }
        Resource recurso = res.valorOLanza();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        String filename = recurso.getFilename();
        if (filename != null) {
            if (filename.toLowerCase().endsWith(".pdf")) {
                mediaType = MediaType.APPLICATION_PDF;
            } else if (filename.toLowerCase().endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
                mediaType = MediaType.IMAGE_JPEG;
            }
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (filename != null ? filename : "comprobante") + "\"")
                .body(recurso);
    }

    private ResponseEntity<ErrorRespuesta> mapearError(ErrorCompra error) {
        HttpStatus status = switch (error) {
            case LOCAL_NO_ENCONTRADO, PROVEEDOR_NO_ENCONTRADO, PRODUCTO_NO_ENCONTRADO, COMPRA_NO_ENCONTRADA -> HttpStatus.NOT_FOUND;
            case COMPROBANTE_DUPLICADO -> HttpStatus.CONFLICT;
            case LOCAL_INACTIVO, PROVEEDOR_INACTIVO, PRODUCTO_INACTIVO, ESTADO_INVALIDO -> HttpStatus.UNPROCESSABLE_ENTITY;
            case TIPO_DOCUMENTO_REQUERIDO, SERIE_NUMERO_REQUERIDOS, FECHA_DOCUMENTO_REQUERIDA,
                 DETALLES_VACIOS, CANTIDAD_INVALIDA, PRECIO_INVALIDO, PRESENTACION_NO_PERTENECE -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(new ErrorRespuesta(error.name()));
    }
}

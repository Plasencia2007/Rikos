package pe.rikos.operacion.inventario.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.web.ErrorRespuesta;
import pe.rikos.operacion.inventario.aplicacion.ConfigurarStockMinimo;
import pe.rikos.operacion.inventario.aplicacion.ConsultarExistencias;
import pe.rikos.operacion.inventario.aplicacion.ErrorInventario;
import pe.rikos.operacion.inventario.aplicacion.dto.FilaExistencia;
import pe.rikos.operacion.inventario.aplicacion.dto.ResumenAlertasInventario;
import pe.rikos.operacion.inventario.aplicacion.dto.SolicitudStockMinimo;

import java.util.List;

/**
 * Endpoints REST para existencias y alertas de inventario por local (ADM-16, US-043).
 */
@RestController
public class ExistenciaController {

    private final ConsultarExistencias consultarExistencias;
    private final ConfigurarStockMinimo configurarStockMinimo;

    public ExistenciaController(ConsultarExistencias consultarExistencias,
                                ConfigurarStockMinimo configurarStockMinimo) {
        this.consultarExistencias = consultarExistencias;
        this.configurarStockMinimo = configurarStockMinimo;
    }

    @GetMapping("/locales/{localId}/existencias")
    public ResponseEntity<?> listar(@PathVariable String localId,
                                    @RequestParam(required = false) String filtro) {
        Resultado<List<FilaExistencia>, ErrorInventario> res = consultarExistencias.ejecutar(localId, filtro);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<List<FilaExistencia>, ErrorInventario>) res).error());
        }
        return ResponseEntity.ok(res.valorOLanza());
    }

    @GetMapping("/locales/{localId}/existencias/resumen-alertas")
    public ResponseEntity<?> resumen(@PathVariable String localId) {
        Resultado<ResumenAlertasInventario, ErrorInventario> res = consultarExistencias.resumen(localId);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<ResumenAlertasInventario, ErrorInventario>) res).error());
        }
        return ResponseEntity.ok(res.valorOLanza());
    }

    @PutMapping("/locales/{localId}/existencias/{productoId}/stock-minimo")
    public ResponseEntity<?> configurarStockMinimo(@PathVariable String localId,
                                                   @PathVariable String productoId,
                                                   @RequestBody SolicitudStockMinimo solicitud,
                                                   @AuthenticationPrincipal String usuarioId) {
        Resultado<FilaExistencia, ErrorInventario> res = configurarStockMinimo.ejecutar(
                localId, productoId, solicitud != null ? solicitud.stockMinimo() : null, usuarioId);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<FilaExistencia, ErrorInventario>) res).error());
        }
        return ResponseEntity.ok(res.valorOLanza());
    }

    private ResponseEntity<ErrorRespuesta> mapearError(ErrorInventario error) {
        return switch (error) {
            case LOCAL_NO_ENCONTRADO -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorRespuesta("LOCAL_NO_ENCONTRADO"));
            case LOCAL_INACTIVO -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorRespuesta("LOCAL_INACTIVO"));
            case PRODUCTO_NO_ENCONTRADO -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorRespuesta("PRODUCTO_NO_ENCONTRADO"));
            case PRODUCTO_INACTIVO -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorRespuesta("PRODUCTO_INACTIVO"));
            case STOCK_MINIMO_NEGATIVO -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorRespuesta("STOCK_MINIMO_NEGATIVO"));
        };
    }
}

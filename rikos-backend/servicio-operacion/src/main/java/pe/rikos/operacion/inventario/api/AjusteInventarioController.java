package pe.rikos.operacion.inventario.api;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.web.ErrorRespuesta;
import pe.rikos.operacion.inventario.api.dto.SolicitudRegistroAjuste;
import pe.rikos.operacion.inventario.aplicacion.ConsultarAjustesInventario;
import pe.rikos.operacion.inventario.aplicacion.ErrorInventario;
import pe.rikos.operacion.inventario.aplicacion.RegistrarAjusteInventario;
import pe.rikos.operacion.inventario.aplicacion.dto.DetalleAjusteDTO;
import pe.rikos.operacion.inventario.aplicacion.dto.RegistroAjusteComando;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST para el registro y consulta de ajustes de inventario con motivo (US-045, RN-052).
 */
@RestController
public class AjusteInventarioController {

    private static final String PERMISO_VER_COSTOS = "PERM_inventario.ver_costos";

    private final RegistrarAjusteInventario registrarAjuste;
    private final ConsultarAjustesInventario consultarAjustes;

    public AjusteInventarioController(RegistrarAjusteInventario registrarAjuste,
                                     ConsultarAjustesInventario consultarAjustes) {
        this.registrarAjuste = registrarAjuste;
        this.consultarAjustes = consultarAjustes;
    }

    @PostMapping("/locales/{localId}/inventario/ajustes")
    public ResponseEntity<?> registrar(
            @PathVariable String localId,
            @RequestBody SolicitudRegistroAjuste solicitud,
            @AuthenticationPrincipal String usuarioId
    ) {
        if (solicitud == null) {
            return ResponseEntity.badRequest().body(new ErrorRespuesta("SOLICITUD_REQUERIDA"));
        }

        RegistroAjusteComando comando = new RegistroAjusteComando(
                localId,
                solicitud.productoId(),
                solicitud.motivoId(),
                solicitud.tipoAjuste(),
                solicitud.cantidad(),
                solicitud.stockFinal(),
                solicitud.observacion(),
                usuarioId,
                solicitud.fecha()
        );

        Resultado<DetalleAjusteDTO, ErrorInventario> resultado = registrarAjuste.ejecutar(comando);
        if (resultado.esFracaso()) {
            return mapearError(((Resultado.Fracaso<DetalleAjusteDTO, ErrorInventario>) resultado).error());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(resultado.valorOLanza());
    }

    @GetMapping("/locales/{localId}/inventario/ajustes")
    public ResponseEntity<?> consultar(
            @PathVariable String localId,
            @RequestParam(required = false) String productoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication autenticacion
    ) {
        boolean puedeVerCostos = autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(a -> PERMISO_VER_COSTOS.equals(a.getAuthority())
                        || "ROLE_ADMINISTRADOR_CENTRAL".equals(a.getAuthority())
                        || "ROLE_ADMINISTRADOR_DE_LOCAL".equals(a.getAuthority()));

        Resultado<List<DetalleAjusteDTO>, ErrorInventario> resultado =
                consultarAjustes.ejecutar(localId, productoId, desde, hasta, puedeVerCostos);

        if (resultado.esFracaso()) {
            return mapearError(((Resultado.Fracaso<List<DetalleAjusteDTO>, ErrorInventario>) resultado).error());
        }

        return ResponseEntity.ok(resultado.valorOLanza());
    }

    private ResponseEntity<ErrorRespuesta> mapearError(ErrorInventario error) {
        return switch (error) {
            case LOCAL_NO_ENCONTRADO, PRODUCTO_NO_ENCONTRADO, MOTIVO_NO_ENCONTRADO ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorRespuesta(error.name()));
            case LOCAL_INACTIVO, PRODUCTO_INACTIVO, MOTIVO_INACTIVO, MOTIVO_TIPO_INVALIDO,
                 OBSERVACION_REQUERIDA, CANTIDAD_INVALIDA, SIN_VARIACION, STOCK_MINIMO_NEGATIVO ->
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorRespuesta(error.name()));
            case USUARIO_NO_ENCONTRADO ->
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorRespuesta(error.name()));
        };
    }
}

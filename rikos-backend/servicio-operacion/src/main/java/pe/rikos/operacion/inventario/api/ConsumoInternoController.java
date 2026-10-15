package pe.rikos.operacion.inventario.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.web.ErrorRespuesta;
import pe.rikos.operacion.inventario.aplicacion.ConsultarConsumosInternos;
import pe.rikos.operacion.inventario.aplicacion.ErrorInventario;
import pe.rikos.operacion.inventario.aplicacion.RegistrarConsumoInterno;
import pe.rikos.operacion.inventario.aplicacion.dto.FilaConsumoInternoDTO;
import pe.rikos.operacion.inventario.aplicacion.dto.RegistroConsumoInternoComando;
import pe.rikos.operacion.inventario.aplicacion.dto.ResumenConsumoInternoDTO;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/locales/{localId}/consumo-interno")
@Tag(name = "Consumo Interno", description = "Registro de comida del personal (US-051)")
public class ConsumoInternoController {

    private final ConsultarConsumosInternos consultarConsumosInternos;
    private final RegistrarConsumoInterno registrarConsumoInterno;

    public ConsumoInternoController(ConsultarConsumosInternos consultarConsumosInternos,
                                    RegistrarConsumoInterno registrarConsumoInterno) {
        this.consultarConsumosInternos = consultarConsumosInternos;
        this.registrarConsumoInterno = registrarConsumoInterno;
    }

    @Operation(summary = "Listar consumos internos del personal")
    @GetMapping
    public ResponseEntity<?> listar(@PathVariable String localId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        Resultado<List<FilaConsumoInternoDTO>, ErrorInventario> res = consultarConsumosInternos.listarPorLocal(localId, fecha);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<List<FilaConsumoInternoDTO>, ErrorInventario>) res).error());
        }
        return ResponseEntity.ok(res.valorOLanza());
    }

    @Operation(summary = "Resumen de métricas de consumo interno")
    @GetMapping("/resumen")
    public ResponseEntity<?> resumen(@PathVariable String localId,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        Resultado<ResumenConsumoInternoDTO, ErrorInventario> res = consultarConsumosInternos.obtenerResumen(localId, fecha);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<ResumenConsumoInternoDTO, ErrorInventario>) res).error());
        }
        return ResponseEntity.ok(res.valorOLanza());
    }

    @Operation(summary = "Registrar consumo de comida del personal")
    @PostMapping
    public ResponseEntity<?> registrar(@PathVariable String localId,
                                       @RequestBody RegistroConsumoInternoComando body,
                                       @AuthenticationPrincipal String operadorId) {
        RegistroConsumoInternoComando comando = new RegistroConsumoInternoComando(
                localId,
                body.usuarioId(),
                body.fecha() != null ? body.fecha() : LocalDate.now(),
                body.servicio(),
                body.turno(),
                body.productoId(),
                body.cantidad(),
                body.observacion(),
                operadorId != null ? operadorId : body.operadorId(),
                body.supervisorPin()
        );

        Resultado<FilaConsumoInternoDTO, ErrorInventario> res = registrarConsumoInterno.ejecutar(comando);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<FilaConsumoInternoDTO, ErrorInventario>) res).error());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(res.valorOLanza());
    }

    private ResponseEntity<?> mapearError(ErrorInventario error) {
        return switch (error) {
            case LOCAL_NO_ENCONTRADO, PRODUCTO_NO_ENCONTRADO, USUARIO_NO_ENCONTRADO, MOTIVO_NO_ENCONTRADO ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorRespuesta(error.name()));
            case LOCAL_INACTIVO, PRODUCTO_INACTIVO, MOTIVO_INACTIVO, MOTIVO_TIPO_INVALIDO,
                 OBSERVACION_REQUERIDA, CANTIDAD_INVALIDA, SIN_VARIACION, SERVICIO_REQUERIDO,
                 PRODUCTO_NO_ES_COMIDA, STOCK_MINIMO_NEGATIVO ->
                    ResponseEntity.badRequest().body(new ErrorRespuesta(error.name()));
            case SEGUNDA_COMIDA_REQUIERE_AUTORIZACION, SUPERVISOR_NO_AUTORIZADO, PIN_SUPERVISOR_INVALIDO ->
                    ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorRespuesta(error.name()));
        };
    }
}

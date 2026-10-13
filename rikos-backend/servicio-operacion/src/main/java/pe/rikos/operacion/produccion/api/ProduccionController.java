package pe.rikos.operacion.produccion.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.web.ErrorRespuesta;
import pe.rikos.operacion.produccion.aplicacion.ConsultarProducciones;
import pe.rikos.operacion.produccion.aplicacion.RegistrarProduccion;
import pe.rikos.operacion.produccion.aplicacion.dto.DetalleProduccionDTO;
import pe.rikos.operacion.produccion.aplicacion.dto.PrecargaProduccionDTO;
import pe.rikos.operacion.produccion.aplicacion.dto.RegistroProduccionComando;
import pe.rikos.operacion.produccion.dominio.ErrorProduccion;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/locales/{localId}/produccion")
@Tag(name = "Producción", description = "Producción de subrecetas y platos (US-048)")
public class ProduccionController {

    private final ConsultarProducciones consultarProducciones;
    private final RegistrarProduccion registrarProduccion;

    public ProduccionController(ConsultarProducciones consultarProducciones,
                                RegistrarProduccion registrarProduccion) {
        this.consultarProducciones = consultarProducciones;
        this.registrarProduccion = registrarProduccion;
    }

    @Operation(summary = "Listar registros de producción de un local")
    @GetMapping
    public ResponseEntity<?> listar(@PathVariable String localId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        Resultado<List<DetalleProduccionDTO>, ErrorProduccion> res = consultarProducciones.listarPorLocal(localId, fecha);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<List<DetalleProduccionDTO>, ErrorProduccion>) res).error());
        }
        return ResponseEntity.ok(res.valorOLanza());
    }

    @Operation(summary = "Obtener datos de precarga para el modal de producción (recetas, insumos, porciones planificadas)")
    @GetMapping("/precarga")
    public ResponseEntity<?> precarga(@PathVariable String localId,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                      @RequestParam(required = false) String servicio) {
        Resultado<PrecargaProduccionDTO, ErrorProduccion> res = consultarProducciones.obtenerPrecarga(localId, fecha, servicio);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<PrecargaProduccionDTO, ErrorProduccion>) res).error());
        }
        return ResponseEntity.ok(res.valorOLanza());
    }

    @Operation(summary = "Registrar producción de subreceta o plato")
    @PostMapping
    public ResponseEntity<?> registrar(@PathVariable String localId,
                                       @RequestBody RegistroProduccionComando comandoBody,
                                       @AuthenticationPrincipal String usuarioId) {
        // Asegurar que localId y usuarioId corresponden a la sesión
        RegistroProduccionComando comando = new RegistroProduccionComando(
                localId,
                comandoBody.productoId(),
                comandoBody.recetaId(),
                comandoBody.fecha(),
                comandoBody.servicio(),
                comandoBody.porcionesPlanificadas(),
                comandoBody.porcionesPreparadas(),
                comandoBody.rendimientoObtenido(),
                usuarioId != null ? usuarioId : comandoBody.usuarioId()
        );

        Resultado<DetalleProduccionDTO, ErrorProduccion> res = registrarProduccion.ejecutar(comando);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<DetalleProduccionDTO, ErrorProduccion>) res).error());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(res.valorOLanza());
    }

    private ResponseEntity<?> mapearError(ErrorProduccion error) {
        return switch (error) {
            case LOCAL_NO_ENCONTRADO, PRODUCTO_NO_ENCONTRADO, RECETA_NO_ENCONTRADA ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorRespuesta(error.name()));
            case LOCAL_INACTIVO, PRODUCTO_INACTIVO, RECETA_INACTIVA,
                 RECETA_NO_CORRESPONDE_A_PRODUCTO, RENDIMIENTO_INVALIDO,
                 PORCIONES_INVALIDAS, FECHA_REQUERIDA ->
                    ResponseEntity.badRequest().body(new ErrorRespuesta(error.name()));
            case USUARIO_NO_ENCONTRADO ->
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorRespuesta(error.name()));
        };
    }
}

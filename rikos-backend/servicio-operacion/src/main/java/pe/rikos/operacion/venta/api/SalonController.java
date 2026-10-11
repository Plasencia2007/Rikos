package pe.rikos.operacion.venta.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.web.ErrorRespuesta;
import pe.rikos.operacion.venta.aplicacion.ActualizarSalon;
import pe.rikos.operacion.venta.aplicacion.CambiarEstadoSalon;
import pe.rikos.operacion.venta.aplicacion.ConsultaSalones;
import pe.rikos.operacion.venta.aplicacion.CrearSalon;
import pe.rikos.operacion.venta.aplicacion.DatosSalon;
import pe.rikos.operacion.venta.aplicacion.EliminarSalon;
import pe.rikos.operacion.venta.aplicacion.ErrorSalon;
import pe.rikos.operacion.venta.aplicacion.GuardarPlano;
import pe.rikos.operacion.venta.aplicacion.PosicionDeMesa;
import pe.rikos.operacion.venta.aplicacion.ResumenDeSalon;
import pe.rikos.operacion.venta.infraestructura.Salon;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Salones del local de la sesion (terraza, salon principal, barra...) y el plano de sus mesas. Verlos,
 * venta.ver; crear, editar, activar, borrar y guardar el plano, venta.administrar. El local nunca viaja en la
 * peticion: es siempre el activo del token.
 */
@RestController
@RequestMapping("/salones")
public class SalonController {

    private final ConsultaSalones consulta;
    private final CrearSalon crearSalon;
    private final ActualizarSalon actualizarSalon;
    private final CambiarEstadoSalon cambiarEstado;
    private final EliminarSalon eliminarSalon;
    private final GuardarPlano guardarPlano;

    public SalonController(ConsultaSalones consulta, CrearSalon crearSalon, ActualizarSalon actualizarSalon,
                           CambiarEstadoSalon cambiarEstado, EliminarSalon eliminarSalon, GuardarPlano guardarPlano) {
        this.consulta = consulta;
        this.crearSalon = crearSalon;
        this.actualizarSalon = actualizarSalon;
        this.cambiarEstado = cambiarEstado;
        this.eliminarSalon = eliminarSalon;
        this.guardarPlano = guardarPlano;
    }

    public record SalonRequest(String nombre, String descripcion, String icono, String color, Integer anchoPlano,
                               Integer altoPlano, String colorPiso) {
        DatosSalon aDatos() {
            return new DatosSalon(nombre, descripcion, icono, color, anchoPlano, altoPlano, colorPiso);
        }
    }

    public record MesaEnPlano(String mesaId, int x, int y, int ancho, int alto) {
    }

    public record PlanoRequest(List<MesaEnPlano> mesas) {
    }

    /** mesas: todas las del salon; mesasActivas y capacidad: solo las activas. */
    public record SalonRespuesta(String id, String nombre, String descripcion, String icono, String color,
                                 boolean activo, int anchoPlano, int altoPlano, String colorPiso, int mesas,
                                 int mesasActivas, int capacidad) {
    }

    private static SalonRespuesta de(ResumenDeSalon r) {
        Salon s = r.salon();
        return new SalonRespuesta(s.getId(), s.getNombre(), s.getDescripcion(), s.getIcono(), s.getColor(), s.isActivo(),
                s.getAnchoPlano(), s.getAltoPlano(), s.getColorPiso(), r.mesas(), r.mesasActivas(), r.capacidad());
    }

    @Operation(summary = "Los salones del local activo, con cuantas mesas tiene cada uno")
    @GetMapping
    public ResponseEntity<?> listar() {
        Resultado<List<ResumenDeSalon>, ErrorSalon> resultado = consulta.listar();
        return resultado.esExito()
                ? ResponseEntity.ok(resultado.valorOLanza().stream().map(SalonController::de).toList())
                : errorDe(resultado);
    }

    @Operation(summary = "Un salon del local activo")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable String id) {
        Resultado<ResumenDeSalon, ErrorSalon> resultado = consulta.obtener(id);
        return resultado.esExito() ? ResponseEntity.ok(de(resultado.valorOLanza())) : errorDe(resultado);
    }

    @Operation(summary = "Crear un salon en el local activo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = SalonRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody SalonRequest cuerpo, @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Salon, ErrorSalon> resultado = crearSalon.ejecutar(cuerpo.aDatos(), actorId);
            return resultado.esExito()
                    ? ResponseEntity.status(HttpStatus.CREATED).body(unSalon(resultado.valorOLanza()))
                    : errorDe(resultado);
        });
    }

    @Operation(summary = "Editar un salon")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody SalonRequest cuerpo,
                                        @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Salon, ErrorSalon> resultado = actualizarSalon.ejecutar(id, cuerpo.aDatos(), actorId);
            return resultado.esExito() ? ResponseEntity.ok(unSalon(resultado.valorOLanza())) : errorDe(resultado);
        });
    }

    @Operation(summary = "Desactivar un salon (sale del mapa, conserva sus mesas)")
    @PostMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Salon, ErrorSalon> resultado = cambiarEstado.ejecutar(id, false, actorId);
            return resultado.esExito() ? ResponseEntity.ok(unSalon(resultado.valorOLanza())) : errorDe(resultado);
        });
    }

    @Operation(summary = "Reactivar un salon")
    @PostMapping("/{id}/activar")
    public ResponseEntity<?> activar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Salon, ErrorSalon> resultado = cambiarEstado.ejecutar(id, true, actorId);
            return resultado.esExito() ? ResponseEntity.ok(unSalon(resultado.valorOLanza())) : errorDe(resultado);
        });
    }

    @Operation(summary = "Eliminar un salon vacio")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Void, ErrorSalon> resultado = eliminarSalon.ejecutar(id, actorId);
            return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
        });
    }

    @Operation(summary = "Guardar el plano del salon: posicion y medidas de sus mesas, todo o nada")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/{id}/plano")
    public ResponseEntity<?> plano(@PathVariable String id, @RequestBody PlanoRequest cuerpo,
                                   @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            List<PosicionDeMesa> posiciones = cuerpo.mesas() == null ? List.of() : cuerpo.mesas().stream()
                    .map(m -> new PosicionDeMesa(m.mesaId(), m.x(), m.y(), m.ancho(), m.alto())).toList();
            Resultado<Void, ErrorSalon> resultado = guardarPlano.ejecutar(id, posiciones, actorId);
            return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
        });
    }

    /**
     * Las escrituras no van dentro de una transaccion del controlador: cada caso de uso confirma la suya, asi un
     * choque de concurrencia sale AQUI, ya cerrado, y se contesta con un 409 claro en vez de un 500.
     */
    private ResponseEntity<?> protegido(Supplier<ResponseEntity<?>> accion) {
        try {
            return accion.get();
        } catch (OptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "SALON_MODIFICADO"));
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMessage()).contains("salon_nombre_unico_por_local")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ErrorSalon.NOMBRE_DUPLICADO.name()));
            }
            throw e;
        }
    }

    /** El salon recien escrito, con su resumen de mesas. */
    private SalonRespuesta unSalon(Salon salon) {
        return de(consulta.obtener(salon.getId()).valorOLanza());
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorSalon> resultado) {
        ErrorSalon e = ((Resultado.Fracaso<?, ErrorSalon>) resultado).error();
        HttpStatus estado = switch (e) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case NOMBRE_DUPLICADO, TIENE_MESAS -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", e.name()));
    }
}

package pe.rikos.operacion.identidad.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
import pe.rikos.operacion.identidad.aplicacion.ActualizarLocal;
import pe.rikos.operacion.identidad.aplicacion.CambiarEstadoLocal;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.identidad.aplicacion.EliminarLocal;
import pe.rikos.operacion.identidad.aplicacion.ErrorLocal;
import pe.rikos.operacion.identidad.aplicacion.ListarLocales;
import pe.rikos.operacion.identidad.aplicacion.ObtenerGerentesLocal;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.comun.web.ErrorRespuesta;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * US-001. Rutas reales bajo /api/locales (context-path del servicio).
 */
@RestController
@RequestMapping("/locales")
public class LocalController {

    private final CrearLocal crearLocal;
    private final ActualizarLocal actualizarLocal;
    private final CambiarEstadoLocal cambiarEstadoLocal;
    private final EliminarLocal eliminarLocal;
    private final ListarLocales listarLocales;
    private final ObtenerGerentesLocal obtenerGerentesLocal;
    private final LocalRepositorio locales;

    public LocalController(CrearLocal crearLocal, ActualizarLocal actualizarLocal,
                            CambiarEstadoLocal cambiarEstadoLocal, EliminarLocal eliminarLocal,
                            ListarLocales listarLocales, ObtenerGerentesLocal obtenerGerentesLocal,
                            LocalRepositorio locales) {
        this.crearLocal = crearLocal;
        this.actualizarLocal = actualizarLocal;
        this.cambiarEstadoLocal = cambiarEstadoLocal;
        this.eliminarLocal = eliminarLocal;
        this.listarLocales = listarLocales;
        this.obtenerGerentesLocal = obtenerGerentesLocal;
        this.locales = locales;
    }

    public record LocalPublicoRespuesta(String codigo, String nombre) {
    }

    @Operation(summary = "Listar locales activos disponibles para autoservicio")
    @GetMapping("/publico")
    public ResponseEntity<List<LocalPublicoRespuesta>> listarPublicos() {
        List<LocalPublicoRespuesta> disponibles = locales.findAllByOrderByNombreAsc().stream()
                .filter(Local::isActivo)
                .map(local -> new LocalPublicoRespuesta(local.getCodigo(), local.getNombre()))
                .toList();
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(disponibles);
    }

    /**
     * Publica (sin autenticar): el equipo del POS necesita mostrar el
     * nombre real de su local en la pantalla de bloqueo, antes de que
     * cualquiera haya iniciado sesion. No expone nada sensible: solo
     * codigo y nombre, lo mismo que ya se ve en la propia pantalla.
     */
    @Operation(summary = "Obtener local publico por codigo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = LocalPublicoRespuesta.class))),
            @ApiResponse(responseCode = "404")
    })
    @GetMapping("/publico/{codigo}")
    public ResponseEntity<?> obtenerPublico(@PathVariable String codigo) {
        Optional<Local> local = locales.findByCodigo(codigo);
        return local
                .map(l -> ResponseEntity.ok((Object) new LocalPublicoRespuesta(l.getCodigo(), l.getNombre())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record CrearLocalRequest(@NotBlank String codigo, @NotBlank String nombre, @NotBlank String direccion) {
    }

    public record ActualizarLocalRequest(@NotBlank String nombre, @NotBlank String direccion,
                                          @NotBlank String telefono, @NotBlank @Email String correo,
                                          BigDecimal latitud, BigDecimal longitud,
                                          String facebookUrl, String instagramUrl, String tiktokUrl, String logoUrl,
                                          @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String colorPrincipal,
                                          String portadaUrl,
                                          boolean deliveryHabilitado, boolean recojoHabilitado,
                                          Integer tiempoMinEntregaMin, Integer tiempoMaxEntregaMin) {
        Local.DatosActualizacion aDatos() {
            return new Local.DatosActualizacion(nombre, direccion, telefono, correo, latitud, longitud,
                    facebookUrl, instagramUrl, tiktokUrl, logoUrl, colorPrincipal, portadaUrl,
                    deliveryHabilitado, recojoHabilitado, tiempoMinEntregaMin, tiempoMaxEntregaMin);
        }
    }

    public record LocalRespuesta(String id, String codigo, String nombre, String direccion, String telefono,
                                  String correo, BigDecimal latitud, BigDecimal longitud,
                                  String facebookUrl, String instagramUrl, String tiktokUrl, String logoUrl,
                                  String colorPrincipal, String portadaUrl,
                                  boolean deliveryHabilitado, boolean recojoHabilitado,
                                  Integer tiempoMinEntregaMin, Integer tiempoMaxEntregaMin,
                                  boolean activo, String gerente) {
        static LocalRespuesta de(Local local, String gerente) {
            return new LocalRespuesta(local.getId(), local.getCodigo(), local.getNombre(), local.getDireccion(),
                    local.getTelefono(), local.getCorreo(), local.getLatitud(), local.getLongitud(),
                    local.getFacebookUrl(), local.getInstagramUrl(), local.getTiktokUrl(), local.getLogoUrl(),
                    local.getColorPrincipal(), local.getPortadaUrl(),
                    local.isDeliveryHabilitado(), local.isRecojoHabilitado(),
                    local.getTiempoMinEntregaMin(), local.getTiempoMaxEntregaMin(),
                    local.isActivo(), gerente);
        }
    }

    @GetMapping
    public List<LocalRespuesta> listar() {
        List<Local> todos = listarLocales.ejecutar();
        Map<String, String> gerentes = obtenerGerentesLocal.resolver(todos.stream().map(Local::getId).toList());
        return todos.stream().map(l -> LocalRespuesta.de(l, gerentes.get(l.getId()))).toList();
    }

    @Operation(summary = "Crear local")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = LocalRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CrearLocalRequest cuerpo,
                                    @AuthenticationPrincipal String actorId) {
        Resultado<Local, ErrorLocal> resultado = crearLocal.ejecutar(cuerpo.codigo(), cuerpo.nombre(),
                cuerpo.direccion(), actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(LocalRespuesta.de(resultado.valorOLanza(), null))
                : errorDe(resultado);
    }

    @Operation(summary = "Actualizar local")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = LocalRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id, @Valid @RequestBody ActualizarLocalRequest cuerpo,
                                         @AuthenticationPrincipal String actorId) {
        Resultado<Local, ErrorLocal> resultado = actualizarLocal.ejecutar(id, cuerpo.aDatos(), actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(LocalRespuesta.de(resultado.valorOLanza(),
                        obtenerGerentesLocal.resolver(List.of(id)).get(id)))
                : errorDe(resultado);
    }

    @Operation(summary = "Desactivar local")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = LocalRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        return cambiarEstado(id, false, actorId);
    }

    @Operation(summary = "Activar local")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = LocalRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/{id}/activar")
    public ResponseEntity<?> activar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        return cambiarEstado(id, true, actorId);
    }

    private ResponseEntity<?> cambiarEstado(String id, boolean activo, String actorId) {
        Resultado<Local, ErrorLocal> resultado = cambiarEstadoLocal.ejecutar(id, activo, actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(LocalRespuesta.de(resultado.valorOLanza(),
                        obtenerGerentesLocal.resolver(List.of(id)).get(id)))
                : errorDe(resultado);
    }

    /**
     * Borrado real: distinto de desactivar. Solo procede si el local nunca
     * llego a usarse (sin usuarios asignados, sin sesiones). Con historial
     * responde 409 con TIENE_HISTORIAL en vez de tocar la base de datos.
     */
    @Operation(summary = "Eliminar local")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String id) {
        Resultado<Void, ErrorLocal> resultado = eliminarLocal.ejecutar(id);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorLocal> resultado) {
        ErrorLocal error = ((Resultado.Fracaso<?, ErrorLocal>) resultado).error();
        HttpStatus estado = switch (error) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case CODIGO_FORMATO_INVALIDO -> HttpStatus.UNPROCESSABLE_ENTITY;
            case CODIGO_DUPLICADO, TIENE_HISTORIAL -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}

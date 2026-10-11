package pe.rikos.operacion.pensionistas.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.web.ErrorRespuesta;
import pe.rikos.operacion.pensionistas.aplicacion.ActualizarPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.CambiarEstadoPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.Comida;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaPensionistas;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaPlanes;
import pe.rikos.operacion.pensionistas.aplicacion.CrearPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.DatosPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.DatosResponsable;
import pe.rikos.operacion.pensionistas.aplicacion.ErrorPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.RegenerarPin;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionista;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.pensionistas.infraestructura.Responsable;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * US-016: fichas de pensionistas. Ver, pensionistas.ver; crear,
 * pensionistas.crear; editar y regenerar el PIN, pensionistas.modificar;
 * activar y desactivar, pensionistas.desactivar. Las fichas se crean y editan
 * solo con conexion (sprint 5, 3.3: dos locales sin internet podrian dar el
 * mismo DNI a personas distintas).
 *
 * Ninguna respuesta lleva el PIN ni su hash, salvo `pin` al crear o al
 * regenerar: esa es la unica vez que existe en claro.
 */
@RestController
@RequestMapping("/pensionistas")
public class PensionistaController {

    private final CrearPensionista crearPensionista;
    private final ActualizarPensionista actualizarPensionista;
    private final CambiarEstadoPensionista cambiarEstado;
    private final RegenerarPin regenerarPin;
    private final ConsultaPensionistas consulta;
    private final ConsultaPlanes planes;

    public PensionistaController(CrearPensionista crearPensionista, ActualizarPensionista actualizarPensionista,
                                 CambiarEstadoPensionista cambiarEstado, RegenerarPin regenerarPin,
                                 ConsultaPensionistas consulta, ConsultaPlanes planes) {
        this.crearPensionista = crearPensionista;
        this.actualizarPensionista = actualizarPensionista;
        this.cambiarEstado = cambiarEstado;
        this.regenerarPin = regenerarPin;
        this.consulta = consulta;
        this.planes = planes;
    }

    public record ResponsableRequest(String nombreCompleto, String dni, String telefono, String correo,
                                     String parentesco, String direccion) {
    }

    /** comidas: desayuno | almuerzo | cena. */
    public record PensionistaRequest(String dni, String nombres, String apellidos, String telefono, String correo,
                                     String institucion, String codigoEstudiante, String carrera, String ciclo,
                                     String planId, List<String> comidas, String frecuenciaAbono,
                                     ResponsableRequest responsable) {
    }

    public record ResponsableRespuesta(String nombreCompleto, String dni, String telefono, String correo,
                                       String parentesco, String direccion) {
        static ResponsableRespuesta de(Responsable r) {
            return new ResponsableRespuesta(r.getNombreCompleto(), r.getDni(), r.getTelefono(), r.getCorreo(),
                    r.getParentesco(), r.getDireccion());
        }
    }

    public record ResumenRespuesta(String id, String dni, String nombres, String apellidos, String institucion,
                                   String planId, String planNombre, List<String> comidas, boolean activo) {
    }

    /** limiteCredito: null = el tope se calcula (entrega 4). Sin PIN ni contrasena, nunca. */
    public record DetalleRespuesta(String id, String dni, String nombres, String apellidos, String telefono,
                                   String correo, String institucion, String codigoEstudiante, String carrera,
                                   String ciclo, String planId, String planNombre, List<String> comidas,
                                   String frecuenciaAbono, Long limiteCredito, boolean activo, String cuentaId,
                                   ResponsableRespuesta responsable, OffsetDateTime creadoEn) {
    }

    /** pin: en claro, solo aqui. Anotalo: no se puede volver a ver. */
    public record CreadoRespuesta(DetalleRespuesta pensionista, String pin) {
    }

    public record PinRespuesta(String pin) {
    }

    private List<String> nombresDeComidas(Set<Comida> comidas) {
        return EnumSet.copyOf(comidas.isEmpty() ? EnumSet.noneOf(Comida.class) : comidas).stream()
                .map(Comida::codigo).toList();
    }

    private String nombreDelPlan(String planId) {
        return planes.plan(planId).map(PlanTarifario::getNombre).orElse(null);
    }

    private ResumenRespuesta resumenDe(Pensionista p) {
        return new ResumenRespuesta(p.getId(), p.getDni(), p.getNombres(), p.getApellidos(), p.getInstitucion(),
                p.getPlanTarifarioId(), nombreDelPlan(p.getPlanTarifarioId()), nombresDeComidas(p.getComidas()),
                p.isActivo());
    }

    private DetalleRespuesta detalleDe(Pensionista p) {
        return new DetalleRespuesta(p.getId(), p.getDni(), p.getNombres(), p.getApellidos(), p.getTelefono(),
                p.getCorreo(), p.getInstitucion(), p.getCodigoEstudiante(), p.getCarrera(), p.getCiclo(),
                p.getPlanTarifarioId(), nombreDelPlan(p.getPlanTarifarioId()), nombresDeComidas(p.getComidas()),
                p.getFrecuenciaAbono(), p.getLimiteCredito(), p.isActivo(),
                consulta.cuenta(p.getId()).map(CuentaPensionista::getId).orElse(null),
                consulta.responsable(p.getId()).map(ResponsableRespuesta::de).orElse(null), p.getCreadoEn());
    }

    /** null si alguna comida no existe. */
    private static Set<Comida> comidasDe(List<String> codigos) {
        Set<Comida> comidas = EnumSet.noneOf(Comida.class);
        for (String codigo : codigos == null ? List.<String>of() : codigos) {
            var comida = Comida.desde(codigo);
            if (comida.isEmpty()) {
                return null;
            }
            comidas.add(comida.get());
        }
        return comidas;
    }

    private static DatosPensionista datosDe(PensionistaRequest c, Set<Comida> comidas) {
        ResponsableRequest r = c.responsable();
        return new DatosPensionista(c.dni(), c.nombres(), c.apellidos(), c.telefono(), c.correo(), c.institucion(),
                c.codigoEstudiante(), c.carrera(), c.ciclo(), c.planId(), comidas, c.frecuenciaAbono(),
                r == null ? null : new DatosResponsable(r.nombreCompleto(), r.dni(), r.telefono(), r.correo(),
                        r.parentesco(), r.direccion()));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ResumenRespuesta> listar(@RequestParam(required = false) String q) {
        return consulta.buscar(q).stream().map(this::resumenDe).toList();
    }

    @Operation(summary = "Ficha de un pensionista")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = DetalleRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> obtener(@PathVariable String id) {
        return consulta.pensionista(id).<ResponseEntity<?>>map(p -> ResponseEntity.ok(detalleDe(p)))
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, ErrorPensionista.NO_ENCONTRADO.name()));
    }

    @Operation(summary = "Dar de alta a un pensionista (devuelve el PIN una sola vez)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = CreadoRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@RequestBody PensionistaRequest cuerpo, @AuthenticationPrincipal String actorId) {
        Set<Comida> comidas = comidasDe(cuerpo.comidas());
        if (comidas == null) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY, "COMIDA_INVALIDA");
        }
        Resultado<CrearPensionista.Creado, ErrorPensionista> resultado =
                crearPensionista.ejecutar(datosDe(cuerpo, comidas), actorId);
        if (!resultado.esExito()) {
            return errorDe(resultado);
        }
        CrearPensionista.Creado creado = resultado.valorOLanza();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreadoRespuesta(detalleDe(creado.pensionista()), creado.pin()));
    }

    @Operation(summary = "Editar la ficha de un pensionista")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = DetalleRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody PensionistaRequest cuerpo,
                                        @AuthenticationPrincipal String actorId) {
        Set<Comida> comidas = comidasDe(cuerpo.comidas());
        if (comidas == null) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY, "COMIDA_INVALIDA");
        }
        Resultado<Pensionista, ErrorPensionista> resultado =
                actualizarPensionista.ejecutar(id, datosDe(cuerpo, comidas), actorId);
        return resultado.esExito() ? ResponseEntity.ok(detalleDe(resultado.valorOLanza())) : errorDe(resultado);
    }

    @Operation(summary = "Desactivar un pensionista")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        Resultado<Pensionista, ErrorPensionista> resultado = cambiarEstado.ejecutar(id, false, actorId);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    @Operation(summary = "Reactivar un pensionista (misma cuenta)")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/{id}/activar")
    public ResponseEntity<?> activar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        Resultado<Pensionista, ErrorPensionista> resultado = cambiarEstado.ejecutar(id, true, actorId);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    @Operation(summary = "Generar un PIN nuevo (se muestra una sola vez)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PinRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/{id}/pin")
    public ResponseEntity<?> nuevoPin(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        Resultado<String, ErrorPensionista> resultado = regenerarPin.ejecutar(id, actorId);
        return resultado.esExito() ? ResponseEntity.ok(new PinRespuesta(resultado.valorOLanza())) : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorPensionista> resultado) {
        ErrorPensionista e = ((Resultado.Fracaso<?, ErrorPensionista>) resultado).error();
        HttpStatus estado = switch (e) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case DNI_DUPLICADO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return error(estado, e.name());
    }

    private static ResponseEntity<?> error(HttpStatus estado, String codigo) {
        return ResponseEntity.status(estado).body(Map.of("error", codigo));
    }
}

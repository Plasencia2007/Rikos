package pe.rikos.operacion.catalogo.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.*;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenu;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenuRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.SemanaMenu;
import pe.rikos.operacion.catalogo.infraestructura.SemanaMenuRepositorio;
import pe.rikos.operacion.comun.web.ErrorRespuesta;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * US-013: programacion semanal del almuerzo. Permisos del modulo "menu":
 * ver (GET), crear la semana (POST /menu/semanas), modificar (opciones,
 * confirmar) y desactivar (quitar opciones, descartar un borrador).
 *
 * Todo lo que cambia la semana devuelve la semana completa, con sus
 * advertencias por repeticion recalculadas (CA3): asi la pantalla muestra
 * el aviso en el mismo momento en que se programa el segundo repetido.
 */
@RestController
@RequestMapping("/menu")
public class ProgramacionMenuController {

    private final CrearSemanaMenu crearSemana;
    private final AgregarOpcionMenu agregarOpcion;
    private final ActualizarOpcionMenu actualizarOpcion;
    private final QuitarOpcionMenu quitarOpcion;
    private final ConfirmarSemanaMenu confirmarSemana;
    private final EliminarSemanaMenu eliminarSemana;
    private final ConsultaMenu consulta;
    private final SemanaMenuRepositorio semanas;
    private final OpcionMenuRepositorio opciones;
    private final ConsultaDisponibilidad disponibilidad;

    public ProgramacionMenuController(CrearSemanaMenu crearSemana, AgregarOpcionMenu agregarOpcion,
            ActualizarOpcionMenu actualizarOpcion, QuitarOpcionMenu quitarOpcion, ConfirmarSemanaMenu confirmarSemana,
            EliminarSemanaMenu eliminarSemana, ConsultaMenu consulta, SemanaMenuRepositorio semanas,
            OpcionMenuRepositorio opciones, ConsultaDisponibilidad disponibilidad) {
        this.disponibilidad = disponibilidad;
        this.crearSemana = crearSemana;
        this.agregarOpcion = agregarOpcion;
        this.actualizarOpcion = actualizarOpcion;
        this.quitarOpcion = quitarOpcion;
        this.confirmarSemana = confirmarSemana;
        this.eliminarSemana = eliminarSemana;
        this.consulta = consulta;
        this.semanas = semanas;
        this.opciones = opciones;
    }

    public record CrearSemanaRequest(LocalDate fechaInicio) {
    }

    public record OpcionRequest(LocalDate fecha, String productoId, String tipoMenu, Integer porcionesEstimadas) {
    }

    public record ActualizarOpcionRequest(String tipoMenu, Integer porcionesEstimadas) {
    }

    public record OpcionRespuesta(String id, String productoId, String productoNombre, String componente,
                                  String tipoMenu, Integer porcionesEstimadas) {
        static OpcionRespuesta de(OpcionMenu o) {
            return new OpcionRespuesta(o.getId(), o.getProducto().getId(), o.getProducto().getNombre(),
                    o.getComponente(), o.getTipoMenu(), o.getPorcionesEstimadas());
        }
    }

    /** cerrado: la fecha es anterior a hoy y ya no se edita. */
    public record DiaRespuesta(LocalDate fecha, boolean cerrado, List<OpcionRespuesta> opciones) {
    }

    public record AdvertenciaRespuesta(LocalDate fecha, String productoId, String productoNombre,
                                       LocalDate repetidoEl) {
        static AdvertenciaRespuesta de(ConsultaMenu.Advertencia a) {
            return new AdvertenciaRespuesta(a.fecha(), a.productoId(), a.productoNombre(), a.repetidoEl());
        }
    }

    /**
     * estado: lo guardado ('borrador' | 'confirmado'). estadoVisible: lo que
     * se muestra, con 'en_servicio' y 'pasada' calculados por fecha. Los
     * siete dias siempre vienen, con o sin opciones.
     */
    public record SemanaRespuesta(String id, LocalDate fechaInicio, LocalDate fechaFin, String estado,
                                  String estadoVisible, OffsetDateTime confirmadaEn, LocalDate hoy,
                                  List<DiaRespuesta> dias, List<AdvertenciaRespuesta> advertencias) {
    }

    /** agotado: en el local activo de quien pregunta (US-015); el POS lo oculta en pedidos nuevos. */
    public record OpcionDelDiaRespuesta(String id, String productoId, String productoNombre, String componente,
                                        String tipoMenu, Integer porcionesEstimadas, boolean agotado) {
    }

    public record MenuDelDiaRespuesta(LocalDate fecha, List<OpcionDelDiaRespuesta> opciones) {
    }

    @Operation(summary = "Semana de menú que empieza en un lunes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SemanaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping("/semanas")
    @Transactional(readOnly = true)
    public ResponseEntity<?> semanaDel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lunes) {
        return semanas.findByFechaInicio(lunes)
                .<ResponseEntity<?>>map(s -> ResponseEntity.ok(respuestaDe(s)))
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "NO_ENCONTRADO"));
    }

    @Operation(summary = "Crear semana de menú")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = SemanaRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/semanas")
    @Transactional
    public ResponseEntity<?> crear(@RequestBody CrearSemanaRequest cuerpo, @AuthenticationPrincipal String actorId) {
        Resultado<SemanaMenu, ErrorProgramacion> resultado = crearSemana.ejecutar(cuerpo.fechaInicio(), actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(respuestaDe(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    @Operation(summary = "Agregar opción a un día")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = SemanaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/semanas/{id}/opciones")
    @Transactional
    public ResponseEntity<?> agregar(@PathVariable String id, @RequestBody OpcionRequest cuerpo) {
        Resultado<OpcionMenu, ErrorProgramacion> resultado = agregarOpcion.ejecutar(id, cuerpo.fecha(),
                cuerpo.productoId(), cuerpo.tipoMenu() == null ? "normal" : cuerpo.tipoMenu(),
                cuerpo.porcionesEstimadas());
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(respuestaDe(semanas.findById(id).orElseThrow()))
                : errorDe(resultado);
    }

    @Operation(summary = "Cambiar porciones o normal/extra de una opción")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SemanaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/opciones/{id}")
    @Transactional
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody ActualizarOpcionRequest cuerpo) {
        Resultado<OpcionMenu, ErrorProgramacion> resultado = actualizarOpcion.ejecutar(id,
                cuerpo.tipoMenu() == null ? "normal" : cuerpo.tipoMenu(), cuerpo.porcionesEstimadas());
        return resultado.esExito()
                ? ResponseEntity.ok(respuestaDe(resultado.valorOLanza().getDia().getSemana()))
                : errorDe(resultado);
    }

    @Operation(summary = "Quitar una opción del día")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SemanaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @DeleteMapping("/opciones/{id}")
    @Transactional
    public ResponseEntity<?> quitar(@PathVariable String id) {
        SemanaMenu semana = opciones.findById(id).map(o -> o.getDia().getSemana()).orElse(null);
        Resultado<Void, ErrorProgramacion> resultado = quitarOpcion.ejecutar(id);
        return resultado.esExito() ? ResponseEntity.ok(respuestaDe(semana)) : errorDe(resultado);
    }

    @Operation(summary = "Confirmar la semana (avisa repeticiones, no bloquea)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SemanaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/semanas/{id}/confirmar")
    @Transactional
    public ResponseEntity<?> confirmar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        Resultado<List<ConsultaMenu.Advertencia>, ErrorProgramacion> resultado = confirmarSemana.ejecutar(id, actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(respuestaDe(semanas.findById(id).orElseThrow()))
                : errorDe(resultado);
    }

    @Operation(summary = "Descartar una semana en borrador")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @DeleteMapping("/semanas/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String id) {
        Resultado<Void, ErrorProgramacion> resultado = eliminarSemana.ejecutar(id);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    /** CA4 / CA6: lo que el POS ofrece ese dia. Solo de una semana confirmada; nunca publico. */
    @Operation(summary = "Menú confirmado de un día (para el POS)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MenuDelDiaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping("/dias/{fecha}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> menuDelDia(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        var agotados = disponibilidad.idsAgotadosDelLocalActivo();
        return consulta.menuConfirmadoDel(fecha)
                .<ResponseEntity<?>>map(lista -> ResponseEntity.ok(new MenuDelDiaRespuesta(fecha,
                        lista.stream().map(o -> new OpcionDelDiaRespuesta(o.getId(), o.getProducto().getId(),
                                o.getProducto().getNombre(), o.getComponente(), o.getTipoMenu(),
                                o.getPorcionesEstimadas(), agotados.contains(o.getProducto().getId()))).toList())))
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "SIN_MENU_CONFIRMADO"));
    }

    private SemanaRespuesta respuestaDe(SemanaMenu semana) {
        LocalDate hoy = consulta.hoy();
        List<OpcionMenu> todas = consulta.opcionesDeLaSemana(semana.getId());
        List<DiaRespuesta> dias = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate fecha = semana.getFechaInicio().plusDays(i);
            dias.add(new DiaRespuesta(fecha, fecha.isBefore(hoy), todas.stream()
                    .filter(o -> o.getDia().getFecha().equals(fecha)).map(OpcionRespuesta::de).toList()));
        }
        return new SemanaRespuesta(semana.getId(), semana.getFechaInicio(), semana.getFechaFin(), semana.getEstado(),
                semana.estadoEn(hoy), semana.getConfirmadaEn(), hoy, dias,
                consulta.advertenciasDeLaSemana(semana.getId()).stream().map(AdvertenciaRespuesta::de).toList());
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorProgramacion> resultado) {
        ErrorProgramacion e = ((Resultado.Fracaso<?, ErrorProgramacion>) resultado).error();
        HttpStatus estado = switch (e) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case SEMANA_YA_EXISTE, OPCION_REPETIDA, YA_CONFIRMADA, SEMANA_CONFIRMADA, DIA_CERRADO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return error(estado, e.name());
    }

    private static ResponseEntity<?> error(HttpStatus estado, String codigo) {
        return ResponseEntity.status(estado).body(Map.of("error", codigo));
    }
}

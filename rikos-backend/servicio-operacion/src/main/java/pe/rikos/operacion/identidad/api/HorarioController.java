package pe.rikos.operacion.identidad.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.aplicacion.CrearDiaEspecial;
import pe.rikos.operacion.identidad.aplicacion.EliminarDiaEspecial;
import pe.rikos.operacion.identidad.aplicacion.ErrorHorario;
import pe.rikos.operacion.identidad.aplicacion.GuardarHorarioSemanal;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecial;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecialRepositorio;
import pe.rikos.operacion.identidad.infraestructura.HorarioDiaEspecial;
import pe.rikos.operacion.identidad.infraestructura.HorarioDiaEspecialRepositorio;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicio;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicioRepositorio;
import pe.rikos.operacion.comun.web.ErrorRespuesta;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Bajo /locales/{localId} a proposito -- las mismas reglas de seguridad de
 * /locales/** (ConfiguracionSeguridad) aplican sin agregar un matcher
 * nuevo, y las rutas quedan anidadas bajo el local al que pertenecen.
 */
@RestController
@RequestMapping("/locales/{localId}")
public class HorarioController {

    private final GuardarHorarioSemanal guardarHorarioSemanal;
    private final CrearDiaEspecial crearDiaEspecial;
    private final EliminarDiaEspecial eliminarDiaEspecial;
    private final HorarioServicioRepositorio horarios;
    private final DiaEspecialRepositorio diasEspeciales;
    private final HorarioDiaEspecialRepositorio franjasDiaEspecial;

    public HorarioController(GuardarHorarioSemanal guardarHorarioSemanal, CrearDiaEspecial crearDiaEspecial,
                              EliminarDiaEspecial eliminarDiaEspecial, HorarioServicioRepositorio horarios,
                              DiaEspecialRepositorio diasEspeciales,
                              HorarioDiaEspecialRepositorio franjasDiaEspecial) {
        this.guardarHorarioSemanal = guardarHorarioSemanal;
        this.crearDiaEspecial = crearDiaEspecial;
        this.eliminarDiaEspecial = eliminarDiaEspecial;
        this.horarios = horarios;
        this.diasEspeciales = diasEspeciales;
        this.franjasDiaEspecial = franjasDiaEspecial;
    }

    public record TurnoRequest(@NotBlank String diaSemana, @NotBlank String servicio,
                                @NotNull LocalTime horaInicio, @NotNull LocalTime horaFin) {
    }

    public record TurnoRespuesta(String id, String diaSemana, String servicio, LocalTime horaInicio,
                                  LocalTime horaFin) {
        static TurnoRespuesta de(HorarioServicio h) {
            return new TurnoRespuesta(h.getId(), h.getDiaSemana(), h.getServicio(), h.getHoraInicio(), h.getHoraFin());
        }
    }

    public record FranjaRequest(@NotBlank String servicio, @NotNull LocalTime horaInicio, @NotNull LocalTime horaFin) {
    }

    public record FranjaRespuesta(String servicio, LocalTime horaInicio, LocalTime horaFin) {
        static FranjaRespuesta de(HorarioDiaEspecial f) {
            return new FranjaRespuesta(f.getServicio(), f.getHoraInicio(), f.getHoraFin());
        }
    }

    public record DiaEspecialRequest(@NotNull LocalDate fecha, boolean cerrado, String descripcion,
                                      List<FranjaRequest> franjas) {
    }

    public record DiaEspecialRespuesta(String id, LocalDate fecha, boolean cerrado, String descripcion,
                                        List<FranjaRespuesta> franjas) {
    }

    public record HorariosRespuesta(List<TurnoRespuesta> turnos, List<DiaEspecialRespuesta> diasEspeciales) {
    }

    @GetMapping("/horarios")
    public HorariosRespuesta obtener(@PathVariable String localId) {
        List<TurnoRespuesta> turnos = horarios.findByLocalIdAndActivoTrue(localId).stream()
                .map(TurnoRespuesta::de).toList();

        List<DiaEspecial> dias = diasEspeciales.findByLocalIdOrderByFecha(localId);
        List<String> idsDias = dias.stream().map(DiaEspecial::getId).toList();
        Map<String, List<HorarioDiaEspecial>> franjasPorDia = franjasDiaEspecial.findByDiaEspecialIdIn(idsDias)
                .stream().collect(java.util.stream.Collectors.groupingBy(HorarioDiaEspecial::getDiaEspecialId));

        List<DiaEspecialRespuesta> diasRespuesta = dias.stream()
                .map(d -> new DiaEspecialRespuesta(d.getId(), d.getFecha(), d.isCerrado(), d.getDescripcion(),
                        franjasPorDia.getOrDefault(d.getId(), List.of()).stream().map(FranjaRespuesta::de).toList()))
                .toList();

        return new HorariosRespuesta(turnos, diasRespuesta);
    }

    @Operation(summary = "Guardar horario semanal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TurnoRespuesta.class)))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/horarios")
    public ResponseEntity<?> guardar(@PathVariable String localId, @RequestBody List<TurnoRequest> cuerpo) {
        List<GuardarHorarioSemanal.Turno> turnos = cuerpo.stream()
                .map(t -> new GuardarHorarioSemanal.Turno(t.diaSemana(), t.servicio(), t.horaInicio(), t.horaFin()))
                .toList();
        Resultado<List<HorarioServicio>, ErrorHorario> resultado = guardarHorarioSemanal.ejecutar(localId, turnos);
        return resultado.esExito()
                ? ResponseEntity.ok(resultado.valorOLanza().stream().map(TurnoRespuesta::de).toList())
                : errorDe(resultado);
    }

    @Operation(summary = "Crear dia especial")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = DiaEspecialRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/dias-especiales")
    public ResponseEntity<?> crearDiaEspecial(@PathVariable String localId, @RequestBody DiaEspecialRequest cuerpo) {
        List<CrearDiaEspecial.Franja> franjas = (cuerpo.franjas() == null ? List.<FranjaRequest>of() : cuerpo.franjas())
                .stream().map(f -> new CrearDiaEspecial.Franja(f.servicio(), f.horaInicio(), f.horaFin())).toList();
        Resultado<DiaEspecial, ErrorHorario> resultado = crearDiaEspecial.ejecutar(localId, cuerpo.fecha(),
                cuerpo.cerrado(), cuerpo.descripcion(), franjas);
        if (!resultado.esExito()) {
            return errorDe(resultado);
        }
        DiaEspecial creado = resultado.valorOLanza();
        List<FranjaRespuesta> franjasRespuesta = franjasDiaEspecial
                .findByDiaEspecialIdOrderByHoraInicio(creado.getId()).stream().map(FranjaRespuesta::de).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(new DiaEspecialRespuesta(creado.getId(),
                creado.getFecha(), creado.isCerrado(), creado.getDescripcion(), franjasRespuesta));
    }

    @Operation(summary = "Eliminar dia especial")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @DeleteMapping("/dias-especiales/{id}")
    public ResponseEntity<?> eliminarDiaEspecial(@PathVariable String localId, @PathVariable String id) {
        Resultado<Void, ErrorHorario> resultado = eliminarDiaEspecial.ejecutar(id);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorHorario> resultado) {
        ErrorHorario error = ((Resultado.Fracaso<?, ErrorHorario>) resultado).error();
        HttpStatus estado = switch (error) {
            case LOCAL_NO_ENCONTRADO, NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case FRANJAS_SOLAPADAS, FECHA_DUPLICADA -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}

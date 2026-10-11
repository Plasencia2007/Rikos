package pe.rikos.operacion.identidad.api;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.operacion.identidad.aplicacion.AuditoriaRespuesta;
import pe.rikos.operacion.identidad.aplicacion.ListarAuditoria;

import java.time.OffsetDateTime;

/**
 * ADM-31: pantalla de bitacora. Solo lectura a proposito -- CA4
 * (inmutabilidad) no deja ningun endpoint de escritura ni de borrado sobre
 * auditoria, ni aqui ni en ningun otro controlador.
 */
@RestController
@RequestMapping("/auditoria")
public class AuditoriaController {

    private final ListarAuditoria listarAuditoria;

    public AuditoriaController(ListarAuditoria listarAuditoria) {
        this.listarAuditoria = listarAuditoria;
    }

    // DT-049: open-in-view esta desactivado, y esto arma su respuesta con
    // EntityManager directo (no por un caso de uso @Transactional propio) --
    // sin esto, la consulta corre sin sesion abierta.
    @Transactional(readOnly = true)
    @Operation(summary = "Listar la bitácora de auditoría")
    @GetMapping
    public Page<AuditoriaRespuesta> listar(
            @RequestParam(required = false) String localId,
            @RequestParam(required = false) String usuarioId,
            @RequestParam(required = false) String evento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "30") int tamano) {

        var filtro = new ListarAuditoria.Filtro(localId, usuarioId, evento, desde, hasta);
        var paginado = PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "ocurridoEn"));
        return listarAuditoria.ejecutar(filtro, paginado);
    }
}

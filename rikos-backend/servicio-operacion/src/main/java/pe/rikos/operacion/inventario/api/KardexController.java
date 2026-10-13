package pe.rikos.operacion.inventario.api;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.web.ErrorRespuesta;
import pe.rikos.operacion.inventario.aplicacion.ConsultarKardex;
import pe.rikos.operacion.inventario.aplicacion.ErrorInventario;
import pe.rikos.operacion.inventario.aplicacion.dto.FiltroKardex;
import pe.rikos.operacion.inventario.aplicacion.dto.PaginaKardex;

import java.time.LocalDate;

/**
 * Endpoint REST para consulta del Kardex por local (ADM-17, US-044).
 * Protegido por PERM_inventario.ver; los costos se omiten sin PERM_inventario.ver_costos.
 */
@RestController
public class KardexController {

    private static final String PERMISO_VER_COSTOS = "PERM_inventario.ver_costos";

    private final ConsultarKardex consultarKardex;

    public KardexController(ConsultarKardex consultarKardex) {
        this.consultarKardex = consultarKardex;
    }

    @GetMapping("/locales/{localId}/inventario/kardex")
    public ResponseEntity<?> consultarKardex(
            @PathVariable String localId,
            @RequestParam(required = false) String productoId,
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false, defaultValue = "desc") String orden,
            @RequestParam(required = false, defaultValue = "0") Integer pagina,
            @RequestParam(required = false, defaultValue = "25") Integer tamano,
            Authentication autenticacion
    ) {
        boolean puedeVerCostos = autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(a -> PERMISO_VER_COSTOS.equals(a.getAuthority()));

        FiltroKardex filtro = new FiltroKardex(productoId, origen, desde, hasta, orden, pagina, tamano);
        Resultado<PaginaKardex, ErrorInventario> res = consultarKardex.ejecutar(localId, filtro, puedeVerCostos);

        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<PaginaKardex, ErrorInventario>) res).error());
        }

        return ResponseEntity.ok(res.valorOLanza());
    }

    private ResponseEntity<ErrorRespuesta> mapearError(ErrorInventario error) {
        return switch (error) {
            case LOCAL_NO_ENCONTRADO -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorRespuesta("LOCAL_NO_ENCONTRADO"));
            case LOCAL_INACTIVO -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorRespuesta("LOCAL_INACTIVO"));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorRespuesta(error.name()));
        };
    }
}

package pe.rikos.operacion.identidad.aplicacion;

import java.time.OffsetDateTime;

/**
 * ADM-31: forma para la pantalla de bitacora. "descripcion" es la frase en
 * lenguaje entendible (CA3: "una bitacora que nadie puede leer es una
 * bitacora que nadie mira"); valorAnterior/valorNuevo van crudos (JSON de
 * texto) para el detalle al expandir la fila.
 */
public record AuditoriaRespuesta(
        String id,
        String evento,
        String entidad,
        String entidadId,
        String usuarioId,
        String usuarioNombre,
        String localId,
        String localNombre,
        String descripcion,
        String valorAnterior,
        String valorNuevo,
        String observacion,
        OffsetDateTime ocurridoEn) {
}

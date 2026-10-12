package pe.rikos.operacion.pensionistas.aplicacion;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record FilaAbono(
        String id,
        String pensionistaId,
        String pensionistaNombre,
        String pensionistaDni,
        String localId,
        String medio,
        long monto,
        LocalDate fechaOperacion,
        String numeroOperacion,
        String cuentaDestino,
        String rutaAdjunto,
        String origen,
        String estado,
        String registradoPor,
        String registradoPorNombre,
        String aprobadoPor,
        String aprobadoPorNombre,
        OffsetDateTime aprobadoEn,
        String motivoRechazoId,
        String motivoRechazoDescripcion,
        String observacion,
        OffsetDateTime creadoEn
) {}

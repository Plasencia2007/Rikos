package pe.rikos.operacion.pensionistas.aplicacion;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Datos del pre-ticket emitido devueltos al kiosco para su impresion y visualizacion (US-019 CA3, CA4).
 */
public record PreTicketEmitido(
        String id,
        String codigo,
        String pensionistaId,
        String pensionistaNombre,
        String dni,
        String localId,
        String localNombre,
        String servicio,
        LocalDate fecha,
        OffsetDateTime emitidoEn,
        OffsetDateTime venceEn,
        boolean reutilizado,
        String mensajeLegal
) {
    public static final String MENSAJE_LEGAL_DEFAULT =
            "Este pre-ticket no constituye un consumo cargado ni comprobante de pago.";

    public PreTicketEmitido(String id, String codigo, String pensionistaId, String pensionistaNombre,
                            String dni, String localId, String localNombre, String servicio,
                            LocalDate fecha, OffsetDateTime emitidoEn, OffsetDateTime venceEn,
                            boolean reutilizado) {
        this(id, codigo, pensionistaId, pensionistaNombre, dni, localId, localNombre, servicio,
                fecha, emitidoEn, venceEn, reutilizado, MENSAJE_LEGAL_DEFAULT);
    }
}

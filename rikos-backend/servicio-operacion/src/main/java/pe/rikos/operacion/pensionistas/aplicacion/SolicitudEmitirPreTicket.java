package pe.rikos.operacion.pensionistas.aplicacion;

import java.time.LocalDateTime;

/**
 * Peticion de emision de pre-ticket desde el kiosco de autoservicio (US-019).
 */
public record SolicitudEmitirPreTicket(
        String localCodigo,
        String dni,
        String pin,
        String servicioManual,
        LocalDateTime momento
) {
    public SolicitudEmitirPreTicket(String localCodigo, String dni, String pin) {
        this(localCodigo, dni, pin, null, null);
    }

    public SolicitudEmitirPreTicket(String localCodigo, String dni, String pin, String servicioManual) {
        this(localCodigo, dni, pin, servicioManual, null);
    }
}

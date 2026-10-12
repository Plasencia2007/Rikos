package pe.rikos.operacion.pensionistas.aplicacion;

/**
 * Detalle del fallo en la emision del pre-ticket (US-019 CA6).
 * Incluye segundos restantes si la cuenta del pensionista esta bloqueada,
 * o intentos restantes antes del bloqueo si las credenciales fueron erroneas.
 */
public record FalloPreTicket(ErrorPreTicket error, Long segundosRestantes, Integer intentosRestantes) {

    public static FalloPreTicket de(ErrorPreTicket error) {
        return new FalloPreTicket(error, null, null);
    }

    public static FalloPreTicket credencialesInvalidas(int intentosRestantes) {
        return new FalloPreTicket(ErrorPreTicket.CREDENCIALES_INVALIDAS, null, intentosRestantes);
    }

    public static FalloPreTicket bloqueado(long segundosRestantes) {
        return new FalloPreTicket(ErrorPreTicket.PENSIONISTA_BLOQUEADO, segundosRestantes, 0);
    }
}

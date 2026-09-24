package pe.rikos.operacion.identidad.aplicacion;

/**
 * Envuelve el tipo de error con, si aplica, cuanto falta para que el
 * bloqueo se levante. La pantalla de PIN/login no debe rechazar en
 * silencio: tiene que poder decir cuanto tiempo falta.
 */
public record FalloAutenticacion(ErrorAutenticacion tipo, Long segundosRestantes) {

    public static FalloAutenticacion de(ErrorAutenticacion tipo) {
        return new FalloAutenticacion(tipo, null);
    }

    public static FalloAutenticacion bloqueado(long segundosRestantes) {
        return new FalloAutenticacion(ErrorAutenticacion.BLOQUEADO, segundosRestantes);
    }
}

package pe.rikos.operacion.caja.aplicacion;

public record SolicitudAbono(
        String pensionistaId,
        String medio,
        Long monto,
        String numeroOperacion,
        String cuentaDestino,
        String observacion
) {
}

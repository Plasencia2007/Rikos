package pe.rikos.operacion.caja.aplicacion;

import pe.rikos.operacion.pensionistas.infraestructura.Abono;

import java.time.OffsetDateTime;

public record AbonoRegistrado(
        Abono abono,
        String pensionistaNombre,
        String pensionistaDni,
        long saldoAnterior,
        long nuevoSaldo,
        long saldoAFavor,
        boolean alertaDuplicado,
        ConstanciaAbono constancia
) {
    public record ConstanciaAbono(
            String localNombre,
            String cajeroNombre,
            OffsetDateTime fecha,
            String leyenda
    ) {
    }
}

package pe.rikos.operacion.pensionistas.aplicacion;

import pe.rikos.operacion.pensionistas.infraestructura.Abono;

public record AbonoAprobado(
        Abono abono,
        String pensionistaNombre,
        String pensionistaDni,
        long nuevoSaldo,
        long saldoAFavor
) {}

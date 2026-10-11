package pe.rikos.operacion.caja.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MovimientoCajaRepositorio extends JpaRepository<MovimientoCaja, String> {

    List<MovimientoCaja> findByTurnoCajaId(String turnoCajaId);

    /**
     * Lo que entro a la caja en un turno, por medio: [medio, suma en centimos]. No cuenta los cobros reabiertos (revertidos):
     * ese dinero ya no es de la caja. Un abono en efectivo (sin pago) siempre cuenta.
     */
    @Query(value = "SELECT m.medio, COALESCE(SUM(m.importe), 0) FROM movimiento_caja m LEFT JOIN pago p ON p.id = m.pago_id "
            + "LEFT JOIN cobro c ON c.id = p.cobro_id WHERE m.turno_caja_id = :turnoId AND (c.id IS NULL OR c.revertido_en IS NULL) "
            + "GROUP BY m.medio", nativeQuery = true)
    List<Object[]> sumasPorMedio(String turnoId);

    /** Cuantos cobros vigentes tiene el turno. */
    @Query(value = "SELECT COUNT(*) FROM cobro c WHERE c.turno_caja_id = :turnoId AND c.revertido_en IS NULL", nativeQuery = true)
    long contarCobrosVigentes(String turnoId);
}

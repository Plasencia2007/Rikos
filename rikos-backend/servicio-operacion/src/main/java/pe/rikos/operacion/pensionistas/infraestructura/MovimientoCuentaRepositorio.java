package pe.rikos.operacion.pensionistas.infraestructura;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Del libro solo se agrega y se lee: extiende Repository (no JpaRepository)
 * a proposito, para que no exista ningun metodo de borrado ni de actualizacion
 * (mismo criterio que AuditoriaRepositorio). La base lo refuerza (V28).
 */
public interface MovimientoCuentaRepositorio extends Repository<MovimientoCuenta, String> {

    MovimientoCuenta save(MovimientoCuenta movimiento);

    Optional<MovimientoCuenta> findById(String id);

    Optional<MovimientoCuenta> findByAbonoId(String abonoId);

    boolean existsByCuentaIdAndTipo(String cuentaId, String tipo);

    /** Del mas reciente al mas antiguo segun cuando OCURRIO, no cuando llego. */
    List<MovimientoCuenta> findByCuentaIdOrderByOcurridoEnDescRegistradoEnDesc(String cuentaId);

    /** Creditos menos cargos. Negativo = deuda; positivo = saldo a favor. */
    @Query("select coalesce(sum(case when m.signo = 'credito' then m.importe else -m.importe end), 0) "
            + "from MovimientoCuenta m where m.cuentaId = :cuentaId")
    long saldo(String cuentaId);

    /** El saldo de todas las cuentas que tienen movimientos, en una sola consulta: [cuentaId, saldo]. */
    @Query("select m.cuentaId, sum(case when m.signo = 'credito' then m.importe else -m.importe end) "
            + "from MovimientoCuenta m group by m.cuentaId")
    java.util.List<Object[]> saldos();
}

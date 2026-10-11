package pe.rikos.operacion.venta.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DetallePedidoRepositorio extends JpaRepository<DetallePedido, String> {

    List<DetallePedido> findByCuentaPersonaIdInOrderByCreadoEn(Collection<String> personasIds);

    boolean existsByCuentaPersonaId(String cuentaPersonaId);

    Optional<DetallePedido> findByIdAndCuentaPersonaIdIn(String id, Collection<String> personasIds);

    /**
     * El segundo consumo del dia (3.4): cuantas comidas del plan (menu normal, desayuno o cena del plan) ya
     * pidio este pensionista ese dia y ese servicio, en CUALQUIER local (su cuenta es una sola) y sin
     * contar las anuladas.
     */
    @Query("""
            select count(d) from DetallePedido d, CuentaPersona c, Pedido p
            where d.cuentaPersonaId = c.id and c.pedidoId = p.id
              and c.pensionistaId = :pensionistaId and p.fechaOperativa = :fecha and p.servicio = :servicio
              and d.tipoLinea in ('menu_normal', 'plan_desayuno', 'plan_cena') and d.estado <> 'anulado'
            """)
    long consumosDelPlanDelDia(String pensionistaId, LocalDate fecha, String servicio);
}

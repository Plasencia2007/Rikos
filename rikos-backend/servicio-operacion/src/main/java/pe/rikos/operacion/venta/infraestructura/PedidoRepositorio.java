package pe.rikos.operacion.venta.infraestructura;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepositorio extends JpaRepository<Pedido, String> {

    /** Un pedido de otro local no se encuentra: nunca se filtra que existe. */
    Optional<Pedido> findByIdAndLocalId(String id, String localId);

    /**
     * El mismo pedido tomado con bloqueo de escritura: dos cajeros cobrando partes del mismo pedido a la vez se turnan, y el
     * segundo ve las lineas que el primero ya cobro (si no, ninguno veria "nada por cobrar" y el pedido no se cerraria).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id and p.localId = :localId")
    Optional<Pedido> bloquearPorIdYLocal(String id, String localId);

    Optional<Pedido> findByMesaIdAndEstadoIn(String mesaId, Collection<String> estados);

    List<Pedido> findByLocalIdAndEstadoIn(String localId, Collection<String> estados);

    long countByLocalIdAndFechaOperativa(String localId, LocalDate fechaOperativa);

    long countByLocalIdAndEstadoIn(String localId, Collection<String> estados);

    /**
     * La lista del POS (sprint 6b): los pedidos del local en esos estados y canales, dentro de un rango de fechas
     * operativas, cuyo numero, nombre de cliente o mesa (numero o nombre) contiene el texto ya en minusculas y entre
     * "%". Sin parametros nulos (Postgres no infiere su tipo): el llamador pasa comodines y extremos abiertos. Mas
     * reciente primero, con el id de desempate para que la paginacion sea estable.
     */
    @Query(value = """
            select p from Pedido p left join Mesa m on m.id = p.mesaId
            where p.localId = :localId and p.estado in :estados and p.canal in :canales
              and p.fechaOperativa >= :desde and p.fechaOperativa <= :hasta
              and (lower(p.numero) like :texto or lower(coalesce(p.nombreCliente, '')) like :texto
                   or lower(coalesce(m.numero, '')) like :texto or lower(coalesce(m.nombre, '')) like :texto)
            order by p.abiertoEn desc, p.id desc
            """,
            countQuery = """
            select count(p) from Pedido p left join Mesa m on m.id = p.mesaId
            where p.localId = :localId and p.estado in :estados and p.canal in :canales
              and p.fechaOperativa >= :desde and p.fechaOperativa <= :hasta
              and (lower(p.numero) like :texto or lower(coalesce(p.nombreCliente, '')) like :texto
                   or lower(coalesce(m.numero, '')) like :texto or lower(coalesce(m.nombre, '')) like :texto)
            """)
    Page<Pedido> buscar(String localId, Collection<String> estados, Collection<String> canales, LocalDate desde,
                        LocalDate hasta, String texto, Pageable pagina);
}

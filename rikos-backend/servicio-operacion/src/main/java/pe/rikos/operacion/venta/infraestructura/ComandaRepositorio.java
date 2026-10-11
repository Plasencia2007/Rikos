package pe.rikos.operacion.venta.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ComandaRepositorio extends JpaRepository<Comanda, String> {

    List<Comanda> findByPedidoIdOrderByNumero(String pedidoId);

    /** Una comanda de otro local no se encuentra: nunca se filtra que existe. */
    Optional<Comanda> findByIdAndLocalId(String id, String localId);

    @Query("select coalesce(max(c.numero), 0) from Comanda c where c.pedidoId = :pedidoId")
    int maxNumero(String pedidoId);

    /**
     * Cuenta una impresion con un UPDATE atomico (impresiones = impresiones + 1): dos impresiones a la vez suman
     * dos. Devuelve cuantas impresiones habia ANTES (null si la comanda no existe); la que ya estaba impresa una
     * vez o mas hace de esta una copia. Es una consulta nativa (UPDATE ... RETURNING): la entidad ya cargada queda
     * vieja y quien llama la refresca.
     */
    @Query(value = """
            update comanda set impresiones = impresiones + 1, ultima_impresion_en = now(),
                               estado_impresion = 'impresa'
            where id = :id
            returning impresiones - 1
            """, nativeQuery = true)
    Integer sumarImpresion(String id);
}

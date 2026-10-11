package pe.rikos.operacion.comun.auditoria;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CA4 (inmutabilidad): a proposito NO extiende JpaRepository/CrudRepository
 * -- esos traen delete()/deleteById()/deleteAll() en la interfaz, y "esta
 * tabla no se modifica ni se borra jamas" (13.1) deberia ser imposible de
 * violar por accidente, no solo una convencion que nadie llama. Con
 * Repository<> a secas, esos metodos ni existen para llamar.
 *
 * Tampoco extiende JpaSpecificationExecutor a proposito: esa interfaz trae
 * su propio delete(Specification) (descubierto por
 * InmutabilidadDeAuditoriaTest -- fallaba con esto extendido). El filtro
 * dinamico de la bitacora (ListarAuditoria) se arma a mano con
 * EntityManager/CriteriaBuilder en vez de depender de esa interfaz.
 */
public interface AuditoriaRepositorio extends Repository<Auditoria, String> {

    Auditoria save(Auditoria auditoria);

    Optional<Auditoria> findById(String id);

    List<Auditoria> findByEntidadAndEntidadIdOrderByOcurridoEnDesc(String entidad, String entidadId);

    /** Lo que hizo una persona (como actor), lo mas reciente primero. */
    List<Auditoria> findTop10ByUsuarioIdOrderByOcurridoEnDesc(String usuarioId);
}

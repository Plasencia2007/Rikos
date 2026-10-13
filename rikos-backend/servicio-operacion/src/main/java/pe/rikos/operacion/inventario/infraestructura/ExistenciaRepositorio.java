package pe.rikos.operacion.inventario.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExistenciaRepositorio extends JpaRepository<Existencia, ExistenciaId> {

    Optional<Existencia> findByLocalIdAndProductoId(String localId, String productoId);

    List<Existencia> findByLocalId(String localId);

    List<Existencia> findByLocalIdAndProductoIdIn(String localId, Collection<String> productoIds);
}

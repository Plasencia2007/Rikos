package pe.rikos.operacion.identidad.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioLocalRepositorio extends JpaRepository<UsuarioLocal, UsuarioLocalId> {

    List<UsuarioLocal> findByUsuarioId(String usuarioId);

    List<UsuarioLocal> findByLocalIdAndUsuario_ActivoTrue(String localId);

    boolean existsByLocalId(String localId);

    void deleteByUsuarioId(String usuarioId);
}

package pe.rikos.operacion.identidad.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRepositorio extends JpaRepository<Auditoria, String> {

    List<Auditoria> findByEntidadAndEntidadIdOrderByOcurridoEnDesc(String entidad, String entidadId);
}

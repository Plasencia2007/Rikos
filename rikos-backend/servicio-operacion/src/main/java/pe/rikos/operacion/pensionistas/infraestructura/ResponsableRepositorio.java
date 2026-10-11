package pe.rikos.operacion.pensionistas.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResponsableRepositorio extends JpaRepository<Responsable, String> {

    Optional<Responsable> findByPensionistaId(String pensionistaId);
}

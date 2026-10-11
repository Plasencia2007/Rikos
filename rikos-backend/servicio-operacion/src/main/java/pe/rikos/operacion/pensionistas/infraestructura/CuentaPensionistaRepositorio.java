package pe.rikos.operacion.pensionistas.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaPensionistaRepositorio extends JpaRepository<CuentaPensionista, String> {

    Optional<CuentaPensionista> findByPensionistaId(String pensionistaId);
}

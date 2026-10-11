package pe.rikos.operacion.pensionistas.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParametroGlobalRepositorio extends JpaRepository<ParametroGlobal, String> {

    Optional<ParametroGlobal> findByClave(String clave);
}

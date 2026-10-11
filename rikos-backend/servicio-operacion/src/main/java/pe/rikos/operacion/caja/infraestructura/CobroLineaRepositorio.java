package pe.rikos.operacion.caja.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CobroLineaRepositorio extends JpaRepository<CobroLinea, String> {

    List<CobroLinea> findByCobroIdIn(java.util.Collection<String> cobrosIds);
}

package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadMedidaRepositorio extends JpaRepository<UnidadMedida, String> {
    boolean existsByCodigo(String codigo);
}

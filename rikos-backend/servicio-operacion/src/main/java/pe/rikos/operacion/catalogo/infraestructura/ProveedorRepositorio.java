package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepositorio extends JpaRepository<Proveedor, String> {
    boolean existsByRuc(String ruc);
}

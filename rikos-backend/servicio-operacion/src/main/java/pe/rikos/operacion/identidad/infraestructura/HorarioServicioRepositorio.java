package pe.rikos.operacion.identidad.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioServicioRepositorio extends JpaRepository<HorarioServicio, String> {

    List<HorarioServicio> findByLocalIdAndActivoTrue(String localId);

    void deleteByLocalId(String localId);
}

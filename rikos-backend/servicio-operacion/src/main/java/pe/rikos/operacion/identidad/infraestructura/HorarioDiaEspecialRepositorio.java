package pe.rikos.operacion.identidad.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioDiaEspecialRepositorio extends JpaRepository<HorarioDiaEspecial, String> {

    List<HorarioDiaEspecial> findByDiaEspecialIdOrderByHoraInicio(String diaEspecialId);

    List<HorarioDiaEspecial> findByDiaEspecialIdIn(List<String> diaEspecialIds);
}

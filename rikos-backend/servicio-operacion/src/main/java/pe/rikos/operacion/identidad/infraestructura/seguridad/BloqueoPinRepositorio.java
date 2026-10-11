package pe.rikos.operacion.identidad.infraestructura.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BloqueoPinRepositorio extends JpaRepository<BloqueoPin, String> {

    Optional<BloqueoPin> findByLocalIdAndDispositivo(String localId, String dispositivo);
}

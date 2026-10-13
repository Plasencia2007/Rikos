package pe.rikos.operacion.pensionistas.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PensionistaRepositorio extends JpaRepository<Pensionista, String> {

    boolean existsByHuellaPin(String huellaPin);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pensionista p where p.huellaPin = :huella")
    java.util.Optional<Pensionista> buscarPorPinParaActualizar(String huella);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pensionista p where p.dni = :dni")
    java.util.Optional<Pensionista> buscarPorDniParaActualizar(String dni);

    boolean existsByDni(String dni);

    java.util.Optional<Pensionista> findByDni(String dni);

    /** Mirror del indice unico: otro pensionista (no este) con ese dni. */
    boolean existsByDniAndIdNot(String dni, String id);

    List<Pensionista> findAllByOrderByApellidosAscNombresAsc();

    @Query("select p from Pensionista p where lower(p.nombres || ' ' || p.apellidos) like lower(concat('%', :texto, '%')) "
            + "or p.dni like concat('%', :texto, '%') order by p.apellidos, p.nombres")
    List<Pensionista> buscar(String texto);
}

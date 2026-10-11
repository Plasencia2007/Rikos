package pe.rikos.operacion.identidad.api;

import org.junit.jupiter.api.Test;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CA4: "esta tabla no se modifica ni se borra jamas" (13.1) no puede ser
 * solo una convencion que alguien podria romper sin darse cuenta. Dos
 * garantias, ambas verificadas por compilacion/reflexion, no por
 * convencion:
 *
 * 1. AuditoriaRepositorio no extiende JpaRepository/CrudRepository -- los
 *    metodos delete()/deleteById()/deleteAll() ni siquiera EXISTEN en la
 *    interfaz. Nadie puede llamarlos porque el compilador los rechaza.
 * 2. AuditoriaController no tiene ningun metodo de escritura ni de borrado
 *    (POST/PUT/PATCH/DELETE) -- solo GET.
 */
class InmutabilidadDeAuditoriaTest {

    @Test
    void elRepositorioNoExponeNingunMetodoDeBorradoOActualizacion() {
        Method[] metodos = AuditoriaRepositorio.class.getMethods();

        boolean tieneDelete = Arrays.stream(metodos).anyMatch(m -> m.getName().startsWith("delete"));
        boolean tieneUpdate = Arrays.stream(metodos)
                .anyMatch(m -> m.getName().equals("update") || m.getName().startsWith("deleteAll"));

        assertThat(tieneDelete)
                .as("AuditoriaRepositorio no deberia tener ningun metodo delete*() -- si aparece uno, "
                        + "alguien volvio a extender JpaRepository/CrudRepository en vez de Repository<>")
                .isFalse();
        assertThat(tieneUpdate).isFalse();
    }

    @Test
    void elControladorSoloExponeLectura() {
        Method[] metodos = AuditoriaController.class.getDeclaredMethods();

        boolean tieneEscritura = Arrays.stream(metodos).anyMatch(m ->
                m.isAnnotationPresent(PutMapping.class)
                        || m.isAnnotationPresent(PatchMapping.class)
                        || m.isAnnotationPresent(DeleteMapping.class)
                        || m.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class));

        assertThat(tieneEscritura)
                .as("AuditoriaController no deberia tener ningun endpoint de escritura o borrado (CA4)")
                .isFalse();
    }
}

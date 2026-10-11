package pe.rikos.operacion.comun.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * P-26: cuando un controlador revienta, Spring reenvia internamente la
 * peticion a /error para armar la respuesta. Si esa ruta no esta en
 * permitAll, la cadena de seguridad bloquea el REENVIO (no la peticion
 * original) y el cliente ve 403 en vez del 500 real -- el fallo interno
 * queda invisible detras de algo que parece un problema de permisos.
 *
 * No hace falta simular una excepcion real: alcanza con confirmar que la
 * ruta /error en si misma no esta bloqueada por seguridad, sin token.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RutaDeErrorAbiertaTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void laRutaDeErrorNoExigeAutenticacion() throws Exception {
        mvc.perform(get("/error"))
                .andExpect(result -> {
                    int estado = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(estado)
                            .as("la ruta /error no deberia devolver 401/403 -- si los devuelve, "
                                    + "un fallo interno real queda oculto detras de un 403 fantasma")
                            .isNotEqualTo(HttpStatus.FORBIDDEN.value())
                            .isNotEqualTo(HttpStatus.UNAUTHORIZED.value());
                });
    }
}

package pe.rikos.operacion.comun.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Setup "standalone": sin contexto de Spring Security ni base de datos --
 * solo el controlador de prueba y el manejador, para probar unicamente el
 * mapeo de excepciones a respuesta HTTP.
 */
class ManejadorGlobalDeErroresTest {

    @RestController
    static class ControladorDePrueba {
        @GetMapping("/kaboom")
        public void kaboom() {
            throw new NullPointerException("algo que ningun caso de uso preveia");
        }

        @GetMapping("/kaboom-403")
        public void kaboom403() {
            throw new AccessDeniedException("simulado");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new ManejadorGlobalDeErrores())
                .build();
    }

    @Test
    void unaExcepcionNoPrevistaDevuelve500ConReferenciaYNoElStackTrace() throws Exception {
        String cuerpo = mockMvc.perform(get("/kaboom"))
                .andExpect(status().is5xxServerError())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).contains("ERROR_INTERNO");
        assertThat(cuerpo).contains("referencia");
        assertThat(cuerpo).doesNotContain("NullPointerException");
    }

    @Test
    void unAccessDeniedExceptionNoLoConvierteEn500_LoDejaPasar() {
        assertThatThrownBy(() -> mockMvc.perform(get("/kaboom-403")))
                .hasRootCauseInstanceOf(AccessDeniedException.class);
    }
}

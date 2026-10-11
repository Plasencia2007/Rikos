package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pe.rikos.contratos.Resultado;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bug encontrado en vivo (sprint 4): cambiar de local desde el encabezado
 * daba 500 -- LazyInitializationException al leer los roles del usuario,
 * porque CambiarLocalActivo no abria transaccion y open-in-view esta
 * apagado (DT-049). CambiarLocalActivoTest no lo veia: es @Transactional y
 * la prueba misma mantiene la sesion de Hibernate abierta.
 *
 * Esta clase NO es @Transactional a proposito: llama al caso de uso igual
 * que el controlador. Usa el administrador y el local sembrados y no
 * escribe nada (reemitir el token solo lee).
 */
@SpringBootTest
class CambiarLocalActivoSinTransaccionTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    private static final String LOCAL_SEMBRADO_ID = "01M2QZ60D35B90CW00EJA15VFC";

    @Autowired
    private CambiarLocalActivo cambiarLocalActivo;

    @Test
    void reemite_el_token_fuera_de_una_transaccion_como_lo_llama_el_controlador() {
        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                cambiarLocalActivo.ejecutar(ADMIN_SEMBRADO_ID, LOCAL_SEMBRADO_ID);

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().localActivoId()).isEqualTo(LOCAL_SEMBRADO_ID);
    }
}

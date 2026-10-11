package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Local;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CrearLocalTest {

    @Autowired
    private CrearLocal crearLocal;

    @Test
    void creaUnLocalConCodigoValido() {
        Resultado<Local, ErrorLocal> resultado =
                crearLocal.ejecutar("nuevo-local", "Local nuevo", "Direccion de prueba", "01M2QZ606763Q7XC00M8VD7WM1");

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().isActivo()).isTrue();
    }

    @Test
    void rechazaUnCodigoConFormatoInvalido() {
        Resultado<Local, ErrorLocal> resultado =
                crearLocal.ejecutar("Local Con Espacios", "Local nuevo", "Direccion de prueba", "01M2QZ606763Q7XC00M8VD7WM1");

        assertThat(resultado.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<Local, ErrorLocal>) resultado).error())
                .isEqualTo(ErrorLocal.CODIGO_FORMATO_INVALIDO);
    }

    @Test
    void rechazaUnCodigoDuplicado() {
        crearLocal.ejecutar("codigo-repetido", "Primero", "Direccion 1", "01M2QZ606763Q7XC00M8VD7WM1");

        Resultado<Local, ErrorLocal> resultado =
                crearLocal.ejecutar("codigo-repetido", "Segundo", "Direccion 2", "01M2QZ606763Q7XC00M8VD7WM1");

        assertThat(resultado.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<Local, ErrorLocal>) resultado).error()).isEqualTo(ErrorLocal.CODIGO_DUPLICADO);
    }
}

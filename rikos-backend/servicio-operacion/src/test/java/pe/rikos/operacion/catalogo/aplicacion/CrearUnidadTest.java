package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CrearUnidadTest {

    @Autowired
    private CrearUnidad crearUnidad;

    @Test
    void creaUnaUnidadConCodigoNuevo() {
        Resultado<UnidadMedida, ErrorCatalogo> resultado = crearUnidad.ejecutar("UND-TEST", "Unidad de prueba", "masa");

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().isActivo()).isTrue();
    }

    @Test
    void rechazaUnCodigoDuplicado() {
        crearUnidad.ejecutar("UND-DUP", "Primera", "masa");

        Resultado<UnidadMedida, ErrorCatalogo> resultado = crearUnidad.ejecutar("UND-DUP", "Segunda", "volumen");

        assertThat(resultado.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<UnidadMedida, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.CODIGO_DUPLICADO);
    }
}

package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ActualizarMotivoTest {

    @Autowired
    private CrearMotivo crearMotivo;

    @Autowired
    private ActualizarMotivo actualizarMotivo;

    @Test
    void actualiza_descripcion_y_requiere_observacion() {
        Motivo motivo = crearMotivo.ejecutar("sobrante_produccion", "Error de receta", false).valorOLanza();

        Resultado<Motivo, ErrorMotivo> resultado =
                actualizarMotivo.ejecutar(motivo.getId(), "Error de receta corregido", true);

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().getDescripcion()).isEqualTo("Error de receta corregido");
        assertThat(resultado.valorOLanza().isRequiereObservacion()).isTrue();
    }

    @Test
    void rechaza_actualizar_un_motivo_inexistente() {
        Resultado<Motivo, ErrorMotivo> resultado =
                actualizarMotivo.ejecutar("no-existe", "Algo", false);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorMotivo>) resultado).error())
                .isEqualTo(ErrorMotivo.NO_ENCONTRADO);
    }

    @Test
    void rechaza_dejar_la_descripcion_igual_a_otro_motivo_activo_del_mismo_tipo() {
        crearMotivo.ejecutar("reapertura", "Error de caja", false);
        Motivo otro = crearMotivo.ejecutar("reapertura", "Pedido incompleto", false).valorOLanza();

        Resultado<Motivo, ErrorMotivo> resultado =
                actualizarMotivo.ejecutar(otro.getId(), "Error de caja", false);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorMotivo>) resultado).error())
                .isEqualTo(ErrorMotivo.DESCRIPCION_DUPLICADA);
    }
}

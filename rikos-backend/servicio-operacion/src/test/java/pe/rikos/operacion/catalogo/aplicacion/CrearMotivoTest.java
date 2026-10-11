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
class CrearMotivoTest {

    @Autowired
    private CrearMotivo crearMotivo;

    @Test
    void crea_un_motivo_con_su_tipo_y_descripcion() {
        Resultado<Motivo, ErrorMotivo> resultado =
                crearMotivo.ejecutar("ajuste_inventario", "Merma por rotura", true);

        assertThat(resultado.esExito()).isTrue();
        Motivo motivo = resultado.valorOLanza();
        assertThat(motivo.getTipo()).isEqualTo("ajuste_inventario");
        assertThat(motivo.getDescripcion()).isEqualTo("Merma por rotura");
        assertThat(motivo.isRequiereObservacion()).isTrue();
        assertThat(motivo.isActivo()).isTrue();
    }

    @Test
    void rechaza_un_tipo_que_no_es_uno_de_los_seis_validos() {
        Resultado<Motivo, ErrorMotivo> resultado =
                crearMotivo.ejecutar("tipo-inventado", "Algo", false);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorMotivo>) resultado).error())
                .isEqualTo(ErrorMotivo.TIPO_INVALIDO);
    }

    @Test
    void rechaza_descripcion_duplicada_dentro_del_mismo_tipo_activo() {
        crearMotivo.ejecutar("anulacion", "Cliente se arrepintio", false);

        Resultado<Motivo, ErrorMotivo> resultado =
                crearMotivo.ejecutar("anulacion", "Cliente se arrepintio", false);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorMotivo>) resultado).error())
                .isEqualTo(ErrorMotivo.DESCRIPCION_DUPLICADA);
    }

    @Test
    void permite_la_misma_descripcion_en_tipos_distintos() {
        crearMotivo.ejecutar("descuento", "Cliente frecuente", false);

        Resultado<Motivo, ErrorMotivo> resultado =
                crearMotivo.ejecutar("rechazo_abono", "Cliente frecuente", false);

        assertThat(resultado.esExito()).isTrue();
    }
}

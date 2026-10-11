package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Los cinco estados de la mesa (RN-059) y su prioridad: bloqueada, unida, pendiente de cierre, ocupada, libre. */
class CalculadorDeEstadoDeMesaTest {

    private static EstadoDeMesa estado(boolean bloqueada, boolean unida, boolean pendienteCierre, boolean abierto) {
        return CalculadorDeEstadoDeMesa.calcular(bloqueada, unida, pendienteCierre, abierto);
    }

    @Test
    void sin_nada_esta_libre() {
        assertThat(estado(false, false, false, false)).isEqualTo(EstadoDeMesa.LIBRE);
    }

    @Test
    void con_un_pedido_abierto_esta_ocupada() {
        assertThat(estado(false, false, false, true)).isEqualTo(EstadoDeMesa.OCUPADA);
    }

    @Test
    void con_un_pedido_pendiente_de_cierre_esta_pendiente_de_cierre_aunque_tambien_haya_uno_abierto() {
        assertThat(estado(false, false, true, false)).isEqualTo(EstadoDeMesa.PENDIENTE_CIERRE);
        assertThat(estado(false, false, true, true)).isEqualTo(EstadoDeMesa.PENDIENTE_CIERRE);
    }

    @Test
    void unida_gana_sobre_pendiente_de_cierre_y_ocupada() {
        assertThat(estado(false, true, false, false)).isEqualTo(EstadoDeMesa.UNIDA);
        assertThat(estado(false, true, true, true)).isEqualTo(EstadoDeMesa.UNIDA);
    }

    @Test
    void bloqueada_gana_sobre_todo() {
        assertThat(estado(true, false, false, false)).isEqualTo(EstadoDeMesa.BLOQUEADA);
        assertThat(estado(true, true, true, true)).isEqualTo(EstadoDeMesa.BLOQUEADA);
    }

    @Test
    void los_codigos_son_los_del_modelo() {
        assertThat(EstadoDeMesa.values()).extracting(EstadoDeMesa::codigo)
                .containsExactly("libre", "ocupada", "unida", "pendiente_cierre", "bloqueada");
    }
}

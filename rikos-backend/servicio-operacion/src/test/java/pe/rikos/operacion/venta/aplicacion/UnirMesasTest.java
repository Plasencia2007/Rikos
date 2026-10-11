package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;

import static org.assertj.core.api.Assertions.assertThat;

/** US-029: unir mesas para un grupo grande (el pedido se opera en conjunto) y separarlas. */
@SpringBootTest
@Transactional
class UnirMesasTest extends ComandasBase {

    @Autowired
    UnirMesas unir;
    @Autowired
    SepararMesa separar;
    @Autowired
    ConsultaMesas consultaMesas;
    @Autowired
    ConsultaPedidos consultaPedidos;
    @Autowired
    BloquearMesa bloquearMesa;
    @Autowired
    CambiarEstadoMesa cambiarEstadoMesa;
    @Autowired
    MesaRepositorio mesas;

    static ErrorMesa errorDeMesa(Resultado<?, ErrorMesa> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorMesa>) resultado).error();
    }

    private String mesaDelPedido() {
        return pedido.getMesaId();
    }

    private EstadoDeMesa estadoDe(String mesaId) {
        return consultaMesas.mapa().valorOLanza().stream().filter(f -> f.mesa().getId().equals(mesaId))
                .findFirst().orElseThrow().estado();
    }

    @Test
    void una_mesa_libre_se_une_a_la_principal_y_el_mapa_la_muestra_unida() {
        String secundaria = nuevaMesa("2");
        comandarElPedido();

        var mesa = unir.ejecutar(secundaria, mesaDelPedido(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(mesa.getMesaPrincipalId()).isEqualTo(mesaDelPedido());
        assertThat(estadoDe(secundaria)).isEqualTo(EstadoDeMesa.UNIDA);
        assertThat(estadoDe(mesaDelPedido())).isEqualTo(EstadoDeMesa.OCUPADA);
    }

    @Test
    void el_pedido_de_la_principal_se_ve_tambien_desde_la_secundaria() {
        String secundaria = nuevaMesa("2");
        unir.ejecutar(secundaria, mesaDelPedido(), ADMIN_SEMBRADO_ID).valorOLanza();

        var porLaSecundaria = consultaPedidos.abiertoDeMesa(secundaria).valorOLanza();

        assertThat(porLaSecundaria.pedido().getId()).isEqualTo(pedido.getId());
    }

    @Test
    void una_principal_con_pedido_abierto_sin_comandar_puede_recibir_mesas_y_queda_tomando_pedido() {
        String secundaria = nuevaMesa("2");

        unir.ejecutar(secundaria, mesaDelPedido(), ADMIN_SEMBRADO_ID).valorOLanza();

        var filas = consultaMesas.mapa().valorOLanza();
        var principal = filas.stream().filter(f -> f.mesa().getId().equals(mesaDelPedido())).findFirst().orElseThrow();
        var unida = filas.stream().filter(f -> f.mesa().getId().equals(secundaria)).findFirst().orElseThrow();
        assertThat(principal.estado()).isEqualTo(EstadoDeMesa.LIBRE);
        assertThat(principal.tomandoPedido()).isTrue();
        assertThat(unida.estado()).isEqualTo(EstadoDeMesa.UNIDA);
        assertThat(unida.tomandoPedido()).isFalse();
    }

    @Test
    void una_principal_sin_pedido_tambien_puede_recibir_mesas() {
        String principal = nuevaMesa("2");
        String secundaria = nuevaMesa("3");

        unir.ejecutar(secundaria, principal, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(estadoDe(principal)).isEqualTo(EstadoDeMesa.LIBRE);
        assertThat(estadoDe(secundaria)).isEqualTo(EstadoDeMesa.UNIDA);
    }

    @Test
    void la_secundaria_tiene_que_estar_libre() {
        String conPedido = nuevaMesa("2");
        abrirPedido.ejecutar(null, Canal.MESA, conPedido, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        String bloqueada = nuevaMesa("3");
        bloquearMesa.ejecutar(bloqueada, "Rota", ADMIN_SEMBRADO_ID).valorOLanza();
        String inactiva = nuevaMesa("4");
        cambiarEstadoMesa.ejecutar(inactiva, false, ADMIN_SEMBRADO_ID).valorOLanza();
        String principal = nuevaMesa("5");
        String comandada = nuevaMesa("6");
        pedidoComandadoEn(comandada);

        // Con un pedido abierto sin comandar: error propio (primero se termina); comandado: la mesa esta ocupada.
        assertThat(errorDeMesa(unir.ejecutar(conPedido, principal, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.TIENE_PEDIDO_ABIERTO);
        assertThat(errorDeMesa(unir.ejecutar(comandada, principal, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.MESA_NO_LIBRE);
        assertThat(errorDeMesa(unir.ejecutar(bloqueada, principal, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.MESA_NO_LIBRE);
        assertThat(errorDeMesa(unir.ejecutar(inactiva, principal, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.MESA_INACTIVA);
    }

    @Test
    void la_principal_tiene_que_estar_activa_sin_bloqueo_y_no_ser_ella_misma_secundaria() {
        String secundaria = nuevaMesa("2");
        String bloqueada = nuevaMesa("3");
        bloquearMesa.ejecutar(bloqueada, "Rota", ADMIN_SEMBRADO_ID).valorOLanza();
        String inactiva = nuevaMesa("4");
        cambiarEstadoMesa.ejecutar(inactiva, false, ADMIN_SEMBRADO_ID).valorOLanza();
        String unidaAOtra = nuevaMesa("5");
        unir.ejecutar(unidaAOtra, mesaDelPedido(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(errorDeMesa(unir.ejecutar(secundaria, bloqueada, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.PRINCIPAL_INVALIDA);
        assertThat(errorDeMesa(unir.ejecutar(secundaria, inactiva, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.PRINCIPAL_INVALIDA);
        assertThat(errorDeMesa(unir.ejecutar(secundaria, unidaAOtra, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.PRINCIPAL_INVALIDA);
        assertThat(errorDeMesa(unir.ejecutar(secundaria, secundaria, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.PRINCIPAL_INVALIDA);
    }

    @Test
    void una_mesa_ya_unida_o_que_es_principal_de_otras_no_se_une() {
        String secundaria = nuevaMesa("2");
        unir.ejecutar(secundaria, mesaDelPedido(), ADMIN_SEMBRADO_ID).valorOLanza();
        String otra = nuevaMesa("3");

        assertThat(errorDeMesa(unir.ejecutar(secundaria, otra, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.YA_UNIDA);
        // La mesa del pedido ya es principal de la 2: no puede pasar a ser secundaria de otra.
        assertThat(errorDeMesa(unir.ejecutar(mesaDelPedido(), otra, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.TIENE_MESAS_UNIDAS);
    }

    @Test
    void mesas_de_otro_local_o_inexistentes_no_se_encuentran() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        String deOtroLocal = nuevaMesa("9");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        String propia = nuevaMesa("2");

        assertThat(errorDeMesa(unir.ejecutar(deOtroLocal, mesaDelPedido(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NO_ENCONTRADA);
        assertThat(errorDeMesa(unir.ejecutar(propia, deOtroLocal, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NO_ENCONTRADA);
        assertThat(errorDeMesa(unir.ejecutar("no-existe", mesaDelPedido(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NO_ENCONTRADA);
    }

    @Test
    void en_una_mesa_unida_no_se_abre_un_pedido_propio() {
        String secundaria = nuevaMesa("2");
        unir.ejecutar(secundaria, mesaDelPedido(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(abrirPedido.ejecutar(null, Canal.MESA, secundaria, "almuerzo", ADMIN_SEMBRADO_ID).esExito()).isFalse();
    }

    @Test
    void una_principal_con_mesas_unidas_no_se_bloquea_ni_se_desactiva() {
        String principal = nuevaMesa("2");
        unir.ejecutar(nuevaMesa("3"), principal, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(errorDeMesa(bloquearMesa.ejecutar(principal, "Reservada", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.TIENE_MESAS_UNIDAS);
        assertThat(errorDeMesa(cambiarEstadoMesa.ejecutar(principal, false, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.TIENE_MESAS_UNIDAS);
    }

    @Test
    void una_mesa_unida_a_otra_no_se_desactiva() {
        String secundaria = nuevaMesa("2");
        unir.ejecutar(secundaria, mesaDelPedido(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(errorDeMesa(cambiarEstadoMesa.ejecutar(secundaria, false, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.MESA_NO_LIBRE);
    }

    @Test
    void separar_deja_la_secundaria_libre_e_independiente() {
        String secundaria = nuevaMesa("2");
        unir.ejecutar(secundaria, mesaDelPedido(), ADMIN_SEMBRADO_ID).valorOLanza();

        var mesa = separar.ejecutar(secundaria, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(mesa.getMesaPrincipalId()).isNull();
        assertThat(estadoDe(secundaria)).isEqualTo(EstadoDeMesa.LIBRE);
        assertThat(consultaPedidos.abiertoDeMesa(secundaria).esExito()).isFalse();
        assertThat(errorDeMesa(separar.ejecutar(secundaria, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.NO_ESTA_UNIDA);
    }

    @Test
    void unir_y_separar_quedan_en_la_bitacora() {
        String secundaria = nuevaMesa("2");
        unir.ejecutar(secundaria, mesaDelPedido(), ADMIN_SEMBRADO_ID).valorOLanza();
        separar.ejecutar(secundaria, ADMIN_SEMBRADO_ID).valorOLanza();

        var eventos = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("mesa", secundaria);
        var tipos = eventos.stream().filter(a -> "transferencia".equals(a.getEvento())).map(a -> a.getValorNuevo())
                .toList();
        assertThat(tipos).anyMatch(v -> v.contains("\"tipo\":\"union_de_mesas\"") && v.contains("\"principal\":\"1\""));
        assertThat(tipos).anyMatch(v -> v.contains("\"tipo\":\"separacion_de_mesas\""));
    }
}

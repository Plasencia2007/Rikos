package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Las reglas de FORMA que la base impone al pedido, sin pasar por los casos de uso. */
@SpringBootTest
@Transactional
class TablasDePedidoTest extends PedidosBase {

    @Autowired
    PedidoRepositorio pedidos;
    @Autowired
    CuentaPersonaRepositorio personas;
    @Autowired
    DetallePedidoRepositorio lineas;

    String local;
    String mesa;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        local = nuevoLocal("tp");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, local);
        mesa = nuevaMesa("1");
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private Pedido pedido(String numero, String canal, String mesaId) {
        return pedidos.saveAndFlush(new Pedido(Identificador.nuevo().valor(), local, numero, LocalDate.now(), canal,
                "almuerzo", mesaId, ADMIN_SEMBRADO_ID));
    }

    @Test
    void un_pedido_de_mesa_lleva_mesa_y_uno_para_llevar_no() {
        pedido("0001", "mesa", mesa);
        pedido("0002", "para_llevar", null);

        assertThatThrownBy(() -> pedido("0003", "mesa", null)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void un_pedido_para_llevar_no_puede_tener_mesa() {
        assertThatThrownBy(() -> pedido("0004", "para_llevar", mesa)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_numero_es_unico_por_local_y_dia_y_una_mesa_tiene_un_solo_pedido_en_curso() {
        pedido("0001", "mesa", mesa);

        assertThatThrownBy(() -> pedido("0001", "para_llevar", null)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void una_mesa_no_admite_dos_pedidos_en_curso() {
        pedido("0001", "mesa", mesa);
        String otraMesa = nuevaMesa("2");
        assertThat(pedido("0002", "mesa", otraMesa)).isNotNull();

        assertThatThrownBy(() -> pedido("0003", "mesa", mesa)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void un_pensionista_no_se_repite_en_un_pedido() {
        Pedido pedido = pedido("0001", "mesa", mesa);
        var plan = nuevoPlan(500, 800, 800);
        var pensionista = nuevoPensionista(plan, "Tablas");
        CuentaPersona uno = new CuentaPersona(Identificador.nuevo().valor(), pedido.getId(), "Persona 1", 1);
        uno.asignarPensionista(pensionista.getId(), "Luis Tablas");
        personas.saveAndFlush(uno);
        CuentaPersona dos = new CuentaPersona(Identificador.nuevo().valor(), pedido.getId(), "Persona 2", 2);
        dos.asignarPensionista(pensionista.getId(), "Luis Tablas");

        assertThatThrownBy(() -> personas.saveAndFlush(dos)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void la_linea_de_producto_exige_producto_y_el_extra_libre_exige_descripcion_y_precio_positivo() {
        Pedido pedido = pedido("0001", "mesa", mesa);
        CuentaPersona persona = personas.saveAndFlush(
                new CuentaPersona(Identificador.nuevo().valor(), pedido.getId(), "Persona 1", 1));
        String producto = productoDeCarta("Chicha", 500L).getId();

        assertThat(lineas.saveAndFlush(new DetallePedido(Identificador.nuevo().valor(), persona.getId(), "producto",
                producto, null, 1, null, 500L, 0L, "publico", null, false, ADMIN_SEMBRADO_ID))).isNotNull();
        assertThat(lineas.saveAndFlush(new DetallePedido(Identificador.nuevo().valor(), persona.getId(), "extra_libre",
                null, "Huevo frito", 1, null, 300L, 0L, "manual", null, false, ADMIN_SEMBRADO_ID))).isNotNull();

        assertThatThrownBy(() -> lineas.saveAndFlush(new DetallePedido(Identificador.nuevo().valor(), persona.getId(),
                "producto", null, null, 1, null, 500L, 0L, "publico", null, false, ADMIN_SEMBRADO_ID)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private CuentaPersona unaPersona() {
        Pedido pedido = pedido("0001", "mesa", mesa);
        return personas.saveAndFlush(new CuentaPersona(Identificador.nuevo().valor(), pedido.getId(), "Persona 1", 1));
    }

    // Una violacion por prueba: en Postgres la transaccion queda abortada tras la primera.
    @Test
    void el_extra_libre_sin_descripcion_se_rechaza() {
        CuentaPersona persona = unaPersona();

        assertThatThrownBy(() -> lineas.saveAndFlush(new DetallePedido(Identificador.nuevo().valor(), persona.getId(),
                "extra_libre", null, null, 1, null, 300L, 0L, "manual", null, false, ADMIN_SEMBRADO_ID)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void una_cantidad_cero_se_rechaza() {
        CuentaPersona persona = unaPersona();
        String producto = productoDeCarta("Chicha", 500L).getId();

        assertThatThrownBy(() -> lineas.saveAndFlush(new DetallePedido(Identificador.nuevo().valor(), persona.getId(),
                "producto", producto, null, 0, null, 500L, 0L, "publico", null, false, ADMIN_SEMBRADO_ID)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void un_precio_cero_se_rechaza() {
        CuentaPersona persona = unaPersona();
        String producto = productoDeCarta("Chicha", 500L).getId();

        assertThatThrownBy(() -> lineas.saveAndFlush(new DetallePedido(Identificador.nuevo().valor(), persona.getId(),
                "producto", producto, null, 1, null, 0L, 0L, "publico", null, false, ADMIN_SEMBRADO_ID)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

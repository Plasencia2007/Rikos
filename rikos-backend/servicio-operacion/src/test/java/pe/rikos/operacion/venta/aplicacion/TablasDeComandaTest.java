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
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Comanda;
import pe.rikos.operacion.venta.infraestructura.ComandaRepositorio;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetalleComanda;
import pe.rikos.operacion.venta.infraestructura.DetalleComandaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Las reglas de FORMA de la comanda y de la anulacion (V33), sin pasar por los casos de uso. Una violacion por prueba. */
@SpringBootTest
@Transactional
class TablasDeComandaTest extends PedidosBase {

    @Autowired
    PedidoRepositorio pedidos;
    @Autowired
    CuentaPersonaRepositorio personas;
    @Autowired
    DetallePedidoRepositorio lineas;
    @Autowired
    ComandaRepositorio comandas;
    @Autowired
    DetalleComandaRepositorio detalles;
    @Autowired
    RegistradorAuditoria auditoria;

    String local;
    Pedido pedido;
    DetallePedido linea;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        local = nuevoLocal("tc");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, local);
        pedido = pedidos.saveAndFlush(new Pedido(Identificador.nuevo().valor(), local, "0001", LocalDate.now(),
                "para_llevar", "almuerzo", null, ADMIN_SEMBRADO_ID));
        CuentaPersona persona = personas.saveAndFlush(
                new CuentaPersona(Identificador.nuevo().valor(), pedido.getId(), "Persona 1", 1));
        linea = lineas.saveAndFlush(new DetallePedido(Identificador.nuevo().valor(), persona.getId(), "extra_libre",
                null, "Huevo", 1, null, 300, 0, "manual", null, false, ADMIN_SEMBRADO_ID));
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private Comanda comanda(int numero) {
        return comandas.saveAndFlush(new Comanda(Identificador.nuevo().valor(), pedido.getId(), local, numero,
                ADMIN_SEMBRADO_ID));
    }

    @Test
    void una_comanda_nace_sin_impresiones_y_pendiente() {
        Comanda comanda = comanda(1);

        assertThat(comanda.getImpresiones()).isZero();
        assertThat(comanda.getEstadoImpresion()).isEqualTo("pendiente");
        assertThat(comandas.maxNumero(pedido.getId())).isEqualTo(1);
    }

    @Test
    void el_numero_de_comanda_no_se_repite_en_el_pedido() {
        comanda(1);

        assertThatThrownBy(() -> comanda(1)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void una_linea_sale_en_una_sola_comanda() {
        Comanda una = comanda(1);
        Comanda otra = comanda(2);
        detalles.saveAndFlush(new DetalleComanda(Identificador.nuevo().valor(), una.getId(), linea.getId(), 1, "Persona 1"));

        assertThatThrownBy(() -> detalles.saveAndFlush(
                new DetalleComanda(Identificador.nuevo().valor(), otra.getId(), linea.getId(), 1, "Persona 1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_detalle_de_comanda_no_admite_cantidad_cero() {
        Comanda comanda = comanda(1);

        assertThatThrownBy(() -> detalles.saveAndFlush(
                new DetalleComanda(Identificador.nuevo().valor(), comanda.getId(), linea.getId(), 0, "Persona 1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void una_linea_anulada_lleva_motivo_quien_y_cuando() {
        // Sin motivo: la base lo impide.
        linea.anular(null, null, ADMIN_SEMBRADO_ID);

        assertThatThrownBy(() -> lineas.saveAndFlush(linea)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void la_bitacora_acepta_los_eventos_de_comanda() {
        auditoria.registrar("comanda_emitida", "comanda", Identificador.nuevo().valor(), null,
                RegistradorAuditoria.campos("numero", 1));
        auditoria.registrar("reimpresion_comanda", "comanda", Identificador.nuevo().valor(), null,
                RegistradorAuditoria.campos("numero", 1));
    }
}

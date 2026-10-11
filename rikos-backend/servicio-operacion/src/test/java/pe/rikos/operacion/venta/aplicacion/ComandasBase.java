package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.Pedido;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Piezas comunes de las pruebas de comanda y anulacion: un local con un pedido de mesa y su primera persona. */
public abstract class ComandasBase extends PedidosBase {

    @Autowired
    protected AbrirPedido abrirPedido;
    @Autowired
    protected AgregarPersona agregarPersona;
    @Autowired
    protected AgregarLinea agregarLinea;
    @Autowired
    protected CuentaPersonaRepositorio personas;
    @Autowired
    protected ComandarPedido comandarPedido;
    @Autowired
    protected MotivoRepositorio motivos;
    @Autowired
    protected AuditoriaRepositorio auditorias;

    protected String localA;
    protected String localB;
    protected Pedido pedido;
    protected String persona;

    @BeforeEach
    void prepararComandas() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = nuevoLocal("ca");
        localB = nuevoLocal("cb");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        pedido = abrirPedido.ejecutar(null, Canal.MESA, nuevaMesa("1"), "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        persona = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(0).getId();
    }

    @AfterEach
    void limpiarContextoDeComandas() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    /** Agrega a la persona indicada una linea de carta pendiente. */
    protected DetallePedido linea(String personaId, String nombre, int cantidad) {
        var producto = productoDeCarta(nombre, 500L);
        return agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), personaId, TipoDeLinea.PRODUCTO,
                producto.getId(), cantidad, null, List.of(), null, null), ADMIN_SEMBRADO_ID).valorOLanza();
    }

    protected DetallePedido linea(String nombre, int cantidad) {
        return linea(persona, nombre, cantidad);
    }

    /** Manda a cocina el pedido de la prueba (una linea y comandar): la mesa pasa a OCUPADA (regla del 2026-10-01). */
    protected void comandarElPedido() {
        linea("Plato", 1);
        comandarPedido.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
    }

    /** Abre un pedido en esa mesa, le agrega una linea y lo comanda: la mesa queda OCUPADA. */
    protected Pedido pedidoComandadoEn(String mesaId) {
        Pedido otro = abrirPedido.ejecutar(null, Canal.MESA, mesaId, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        String suPersona = personas.findByPedidoIdOrderByOrden(otro.getId()).get(0).getId();
        agregarLinea.ejecutar(new SolicitudDeLinea(null, otro.getId(), suPersona, TipoDeLinea.PRODUCTO,
                productoDeCarta("Otro plato", 700L).getId(), 1, null, List.of(), null, null), ADMIN_SEMBRADO_ID)
                .valorOLanza();
        comandarPedido.ejecutar(otro.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        return otro;
    }

    protected Motivo motivoDeAnulacion(String descripcion, boolean requiereObservacion) {
        return motivos.save(new Motivo(pe.rikos.contratos.Identificador.nuevo().valor(), "anulacion", descripcion + " " + pe.rikos.contratos.Identificador.nuevo().valor().substring(18),
                requiereObservacion));
    }

    static ErrorPedido error(Resultado<?, ErrorPedido> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorPedido>) resultado).error();
    }
}

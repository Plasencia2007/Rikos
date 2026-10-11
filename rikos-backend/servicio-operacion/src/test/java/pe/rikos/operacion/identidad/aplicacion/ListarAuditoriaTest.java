package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.ActivarProducto;
import pe.rikos.operacion.catalogo.aplicacion.CrearCategoria;
import pe.rikos.operacion.catalogo.aplicacion.CrearProducto;
import pe.rikos.operacion.catalogo.aplicacion.CrearUnidad;
import pe.rikos.operacion.catalogo.aplicacion.DesactivarProducto;
import pe.rikos.operacion.catalogo.aplicacion.EliminarProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-006 / ADM-31: una fila real de cada evento nuevo (cambio_estado,
 * eliminacion, cambio_precio), con la descripcion humanizada que CA3 pide.
 */
@SpringBootTest
@Transactional
class ListarAuditoriaTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    private ListarAuditoria listarAuditoria;
    @Autowired
    private CrearCategoria crearCategoria;
    @Autowired
    private CrearUnidad crearUnidad;
    @Autowired
    private CrearProducto crearProducto;
    @Autowired
    private DesactivarProducto desactivarProducto;
    @Autowired
    private ActivarProducto activarProducto;
    @Autowired
    private EliminarProducto eliminarProducto;
    @Autowired
    private pe.rikos.operacion.catalogo.aplicacion.ActualizarProducto actualizarProducto;
    @Autowired
    private pe.rikos.operacion.catalogo.aplicacion.CrearReceta crearReceta;
    @Autowired
    private pe.rikos.operacion.catalogo.aplicacion.ActualizarReceta actualizarReceta;
    @Autowired
    private pe.rikos.operacion.pensionistas.aplicacion.CrearPlanTarifario crearPlan;
    @Autowired
    private pe.rikos.operacion.pensionistas.aplicacion.CambiarPreciosPlan cambiarPrecios;
    @Autowired
    private pe.rikos.operacion.pensionistas.aplicacion.CambiarParametro cambiarParametro;
    @Autowired
    private pe.rikos.operacion.pensionistas.aplicacion.CrearPensionista crearPensionista;
    @Autowired
    private pe.rikos.operacion.pensionistas.aplicacion.CambiarEstadoPensionista cambiarEstadoPensionista;
    @Autowired
    private pe.rikos.operacion.pensionistas.aplicacion.RegenerarPin regenerarPin;
    @Autowired
    private pe.rikos.operacion.pensionistas.aplicacion.CargarSaldoInicial cargarSaldoInicial;
    @Autowired
    private pe.rikos.operacion.pensionistas.aplicacion.RegistrarAjuste registrarAjuste;
    @Autowired
    private pe.rikos.operacion.pensionistas.aplicacion.FijarTopeDeCredito fijarTope;
    @Autowired
    private CrearLocal crearLocalDePrueba;
    @Autowired
    private pe.rikos.operacion.venta.aplicacion.CrearMesa crearMesa;
    @Autowired
    private pe.rikos.operacion.venta.aplicacion.BloquearMesa bloquearMesa;
    @Autowired
    private pe.rikos.operacion.venta.aplicacion.LiberarMesa liberarMesa;
    @Autowired
    private pe.rikos.operacion.venta.aplicacion.CambiarEstadoMesa cambiarEstadoMesa;
    @Autowired
    private pe.rikos.operacion.venta.aplicacion.AbrirPedido abrirPedido;
    @Autowired
    private pe.rikos.operacion.venta.aplicacion.AgregarLinea agregarLinea;
    @Autowired
    private pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio personasDelPedido;
    @Autowired
    private pe.rikos.operacion.venta.aplicacion.ComandarPedido comandarPedido;
    @Autowired
    private pe.rikos.operacion.venta.aplicacion.RegistrarImpresion registrarImpresion;
    @Autowired
    private pe.rikos.operacion.venta.aplicacion.AnularLinea anularLinea;
    @Autowired
    private pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio motivosDePrueba;
    @Autowired
    private pe.rikos.operacion.venta.aplicacion.TrasladarLineas trasladarLineas;
    @Autowired
    private pe.rikos.operacion.venta.aplicacion.TerminarPedido terminarPedido;
    @Autowired
    private pe.rikos.operacion.venta.aplicacion.MoverPedidoDeMesa moverPedidoDeMesa;
    @Autowired
    private pe.rikos.operacion.venta.aplicacion.UnirMesas unirMesas;
    @Autowired
    private pe.rikos.operacion.venta.aplicacion.SepararMesa separarMesa;

    @BeforeEach
    void autenticar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private Producto productoDePrueba(String sufijo) {
        CategoriaProducto raiz = crearCategoria.ejecutar("Raiz bitacora " + sufijo, "insumo", null, null, 1, null)
                .valorOLanza();
        CategoriaProducto hoja = crearCategoria.ejecutar("Hoja bitacora " + sufijo, null, null, raiz.getId(), 1, null)
                .valorOLanza();
        UnidadMedida unidad = crearUnidad.ejecutar("und-bit-" + sufijo, "Unidad bitacora " + sufijo, "masa")
                .valorOLanza();
        Resultado<Producto, pe.rikos.operacion.catalogo.aplicacion.ErrorCatalogo> resultado = crearProducto.ejecutar(
                "BIT-" + sufijo, "Arroz bitacora " + sufijo, hoja.getId(), unidad.getId(), false, true, false, null,
                true, ADMIN_SEMBRADO_ID);
        return resultado.valorOLanza();
    }

    @Test
    void unCambioDeEstadoQuedaAuditadoYSeVeHumanizado() {
        Producto producto = productoDePrueba("A");

        desactivarProducto.ejecutar(producto.getId(), ADMIN_SEMBRADO_ID);

        Page<AuditoriaRespuesta> pagina = listarAuditoria.ejecutar(
                new ListarAuditoria.Filtro(null, null, "cambio_estado", null, null), PageRequest.of(0, 50));
        AuditoriaRespuesta fila = pagina.getContent().stream()
                .filter(a -> a.entidadId().equals(producto.getId())).findFirst().orElseThrow();

        assertThat(fila.entidad()).isEqualTo("producto");
        assertThat(fila.usuarioNombre()).isEqualTo("admin");
        assertThat(fila.descripcion()).contains("desactivó").contains("Arroz bitacora A");
    }

    @Test
    void unaEliminacionQuedaAuditadaConElNombreDelRegistroBorrado() {
        Producto producto = productoDePrueba("B");
        String idBorrado = producto.getId();

        eliminarProducto.ejecutar(idBorrado);

        Page<AuditoriaRespuesta> pagina = listarAuditoria.ejecutar(
                new ListarAuditoria.Filtro(null, null, "eliminacion", null, null), PageRequest.of(0, 50));
        AuditoriaRespuesta fila = pagina.getContent().stream()
                .filter(a -> a.entidadId().equals(idBorrado)).findFirst().orElseThrow();

        assertThat(fila.descripcion()).contains("eliminó").contains("Arroz bitacora B");
        assertThat(fila.valorNuevo()).isNull();
        assertThat(fila.valorAnterior()).contains("Arroz bitacora B");
    }

    @Test
    void unCambioDePrecioQuedaAuditadoConLosDosMontosEnSoles() {
        CategoriaProducto raiz = crearCategoria.ejecutar("Raiz bitacora C", "insumo", null, null, 1, null)
                .valorOLanza();
        CategoriaProducto hoja = crearCategoria.ejecutar("Hoja bitacora C", null, null, raiz.getId(), 1, null)
                .valorOLanza();
        UnidadMedida unidad = crearUnidad.ejecutar("und-bit-c", "Unidad bitacora C", "masa").valorOLanza();
        Producto producto = crearProducto.ejecutar("BIT-C", "Arroz bitacora C", hoja.getId(), unidad.getId(), true,
                false, false, 1200L, true, ADMIN_SEMBRADO_ID).valorOLanza();

        actualizarProducto.ejecutar(producto.getId(),
                new Producto.DatosActualizacion("Arroz bitacora C", hoja, unidad, true, false, false, 1350L, true),
                ADMIN_SEMBRADO_ID);

        Page<AuditoriaRespuesta> pagina = listarAuditoria.ejecutar(
                new ListarAuditoria.Filtro(null, null, "cambio_precio", null, null), PageRequest.of(0, 50));
        AuditoriaRespuesta fila = pagina.getContent().stream()
                .filter(a -> a.entidadId().equals(producto.getId())).findFirst().orElseThrow();

        assertThat(fila.descripcion()).contains("S/ 12.00").contains("S/ 13.50").contains("Arroz bitacora C");
    }

    @Test
    void crearYModificarUnaRecetaSeDistinguenEnLaBitacora() {
        Producto insumo = productoDePrueba("D");
        Producto aderezo = crearProducto.ejecutar("BIT-D2", "Aderezo bitacora D", insumo.getCategoria().getId(),
                insumo.getUnidadBase().getId(), false, false, true, null, false, ADMIN_SEMBRADO_ID).valorOLanza();
        var ingredientes = java.util.List.of(new pe.rikos.operacion.catalogo.aplicacion.ComposicionReceta.IngredienteEntrada(
                insumo.getId(), java.math.BigDecimal.ONE, insumo.getUnidadBase().getId()));
        String recetaId = crearReceta.ejecutar(aderezo.getId(), java.math.BigDecimal.ONE, 1, ingredientes,
                ADMIN_SEMBRADO_ID).valorOLanza().getId();
        actualizarReceta.ejecutar(recetaId, java.math.BigDecimal.TEN, 1, ingredientes, ADMIN_SEMBRADO_ID);

        Page<AuditoriaRespuesta> pagina = listarAuditoria.ejecutar(
                new ListarAuditoria.Filtro(null, null, "cambio_receta", null, null), PageRequest.of(0, 50));
        var descripciones = pagina.getContent().stream().filter(a -> a.entidadId().equals(recetaId))
                .map(AuditoriaRespuesta::descripcion).toList();

        assertThat(descripciones).containsExactlyInAnyOrder(
                "admin creó la receta de \"Aderezo bitacora D\"",
                "admin modificó la receta de \"Aderezo bitacora D\"");
    }

    @Test
    void elPlanTarifarioYElTaperSeVenEnEspanolEnLaBitacora() {
        var precios = new pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan(500L, 800L, 800L);
        String nombre = "Plan bitacora " + pe.rikos.contratos.Identificador.nuevo().valor().substring(16);
        var plan = crearPlan.ejecutar(nombre, null, precios, null, ADMIN_SEMBRADO_ID).valorOLanza();
        cambiarPrecios.ejecutar(plan.getId(),
                new pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan(500L, 900L, 800L), null, ADMIN_SEMBRADO_ID);
        var taper = pe.rikos.operacion.pensionistas.aplicacion.ClaveParametro.TAPER_PRECIO;
        cambiarParametro.ejecutar(taper, null, ADMIN_SEMBRADO_ID);
        cambiarParametro.ejecutar(taper, "100", ADMIN_SEMBRADO_ID);

        var descripciones = listarAuditoria.ejecutar(new ListarAuditoria.Filtro(null, null, null, null, null),
                PageRequest.of(0, 100)).getContent().stream().map(AuditoriaRespuesta::descripcion).toList();

        assertThat(descripciones).contains("admin creó el plan \"" + nombre + "\"",
                "admin cambió los precios del plan \"" + nombre + "\"",
                "admin cambió el precio del táper de sin monto a S/ 1.00");
    }

    @Test
    void laFichaDelPensionistaSeVeEnEspanolYElPinNuncaAparece() {
        var plan = crearPlan.ejecutar("Plan bit pens " + pe.rikos.contratos.Identificador.nuevo().valor().substring(16),
                null, new pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan(500L, 800L, 800L), null,
                ADMIN_SEMBRADO_ID).valorOLanza();
        String dni = String.valueOf(java.util.concurrent.ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        var datos = new pe.rikos.operacion.pensionistas.aplicacion.DatosPensionista(dni, "Luis", "Bitacora Quispe", null,
                null, null, null, null, null, plan.getId(),
                java.util.Set.of(pe.rikos.operacion.pensionistas.aplicacion.Comida.ALMUERZO), null,
                new pe.rikos.operacion.pensionistas.aplicacion.DatosResponsable("Rosa Quispe", "12345678", "999",
                        "r@c.pe", "Madre", "Av. 1"));
        var creado = crearPensionista.ejecutar(datos, ADMIN_SEMBRADO_ID).valorOLanza();
        cambiarEstadoPensionista.ejecutar(creado.pensionista().getId(), false, ADMIN_SEMBRADO_ID);
        String pinNuevo = regenerarPin.ejecutar(creado.pensionista().getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        var filas = listarAuditoria.ejecutar(new ListarAuditoria.Filtro(null, null, null, null, null),
                PageRequest.of(0, 100)).getContent().stream()
                .filter(a -> creado.pensionista().getId().equals(a.entidadId())).toList();

        assertThat(filas).extracting(AuditoriaRespuesta::descripcion).contains(
                "admin desactivó el pensionista \"Luis Bitacora Quispe\"",
                "admin generó un PIN nuevo para \"Luis Bitacora Quispe\"");
        assertThat(filas).allMatch(f -> !String.valueOf(f.valorNuevo()).contains(pinNuevo)
                && !String.valueOf(f.valorAnterior()).contains(pinNuevo));
    }

    @Test
    void laCuentaDelPensionistaSeVeEnEspanolEnLaBitacora() {
        var plan = crearPlan.ejecutar("Plan bit cuenta " + pe.rikos.contratos.Identificador.nuevo().valor().substring(16),
                null, new pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan(500L, 800L, 800L), null,
                ADMIN_SEMBRADO_ID).valorOLanza();
        String dni = String.valueOf(java.util.concurrent.ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        var datos = new pe.rikos.operacion.pensionistas.aplicacion.DatosPensionista(dni, "Eva", "Cuenta Bitacora", null,
                null, null, null, null, null, plan.getId(),
                java.util.Set.of(pe.rikos.operacion.pensionistas.aplicacion.Comida.ALMUERZO), null,
                new pe.rikos.operacion.pensionistas.aplicacion.DatosResponsable("Rosa Cuenta", "12345678", "999",
                        "r@c.pe", "Madre", "Av. 1"));
        String id = crearPensionista.ejecutar(datos, ADMIN_SEMBRADO_ID).valorOLanza().pensionista().getId();
        String codigo = "bc" + pe.rikos.contratos.Identificador.nuevo().valor().substring(20).toLowerCase();
        String localId = crearLocalDePrueba.ejecutar(codigo, "Local " + codigo, "Dirección", ADMIN_SEMBRADO_ID)
                .valorOLanza().getId();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localId);

        cargarSaldoInicial.ejecutar(id, 5000L, pe.rikos.operacion.pensionistas.aplicacion.Signo.CARGO, null, null,
                ADMIN_SEMBRADO_ID);
        registrarAjuste.ejecutar(id, 300L, pe.rikos.operacion.pensionistas.aplicacion.Signo.CREDITO,
                "01M39A0MT0000000000000D002", null, ADMIN_SEMBRADO_ID);
        fijarTope.ejecutar(id, 10000L, ADMIN_SEMBRADO_ID);
        fijarTope.ejecutar(id, null, ADMIN_SEMBRADO_ID);

        var descripciones = listarAuditoria.ejecutar(new ListarAuditoria.Filtro(null, null, null, null, null),
                PageRequest.of(0, 100)).getContent().stream().filter(a -> id.equals(a.entidadId()))
                .map(AuditoriaRespuesta::descripcion).toList();

        assertThat(descripciones).contains(
                "admin cargó el saldo inicial de \"Eva Cuenta Bitacora\": debe S/ 50.00",
                "admin ajustó la cuenta de \"Eva Cuenta Bitacora\": crédito de S/ 3.00 (Corrección de cobro)",
                "admin fijó el tope de crédito de \"Eva Cuenta Bitacora\" en S/ 100.00",
                "admin dejó que se calcule el tope de crédito de \"Eva Cuenta Bitacora\"");
    }

    @Test
    void lasMesasSeVenEnEspanolEnLaBitacora() {
        String codigo = "bm" + pe.rikos.contratos.Identificador.nuevo().valor().substring(20).toLowerCase();
        String localId = crearLocalDePrueba.ejecutar(codigo, "Local " + codigo, "Dirección", ADMIN_SEMBRADO_ID)
                .valorOLanza().getId();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localId);

        var mesa = crearMesa.ejecutar(new pe.rikos.operacion.venta.aplicacion.DatosMesa("7", "Terraza", 4),
                ADMIN_SEMBRADO_ID).valorOLanza();
        bloquearMesa.ejecutar(mesa.getId(), "Mesa rota", ADMIN_SEMBRADO_ID);
        liberarMesa.ejecutar(mesa.getId(), ADMIN_SEMBRADO_ID);
        cambiarEstadoMesa.ejecutar(mesa.getId(), false, ADMIN_SEMBRADO_ID);

        var descripciones = listarAuditoria.ejecutar(new ListarAuditoria.Filtro(null, null, null, null, null),
                PageRequest.of(0, 100)).getContent().stream().filter(a -> mesa.getId().equals(a.entidadId()))
                .map(AuditoriaRespuesta::descripcion).toList();

        assertThat(descripciones).contains(
                "admin creó la mesa \"7\"",
                "admin bloqueó la mesa \"Mesa 7\": Mesa rota",
                "admin liberó la mesa \"Mesa 7\"",
                "admin desactivó la mesa \"Mesa 7\"");
    }

    @Test
    void elPrecioManualDeUnExtraSeVeEnEspanolEnLaBitacora() {
        String codigo = "bp" + pe.rikos.contratos.Identificador.nuevo().valor().substring(20).toLowerCase();
        String localId = crearLocalDePrueba.ejecutar(codigo, "Local " + codigo, "Dirección", ADMIN_SEMBRADO_ID)
                .valorOLanza().getId();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localId);
        var pedido = abrirPedido.ejecutar(null, pe.rikos.operacion.venta.aplicacion.Canal.PARA_LLEVAR, null, "almuerzo",
                ADMIN_SEMBRADO_ID).valorOLanza();
        String persona = personasDelPedido.findByPedidoIdOrderByOrden(pedido.getId()).get(0).getId();

        var linea = agregarLinea.ejecutar(new pe.rikos.operacion.venta.aplicacion.SolicitudDeLinea(null, pedido.getId(),
                persona, pe.rikos.operacion.venta.aplicacion.TipoDeLinea.EXTRA_LIBRE, null, 1, null,
                java.util.List.of(), "Huevo frito", 300L), ADMIN_SEMBRADO_ID).valorOLanza();

        var descripciones = listarAuditoria.ejecutar(new ListarAuditoria.Filtro(null, null, null, null, null),
                PageRequest.of(0, 100)).getContent().stream().filter(a -> linea.getId().equals(a.entidadId()))
                .map(AuditoriaRespuesta::descripcion).toList();

        assertThat(descripciones).contains(
                "admin puso el precio de un extra a mano: \"Huevo frito\" a S/ 3.00 (pedido \"0001\")");
    }

    @Test
    void laComandaLaReimpresionYLaAnulacionSeVenEnEspanolEnLaBitacora() {
        String codigo = "bc" + pe.rikos.contratos.Identificador.nuevo().valor().substring(20).toLowerCase();
        String localId = crearLocalDePrueba.ejecutar(codigo, "Local " + codigo, "Dirección", ADMIN_SEMBRADO_ID)
                .valorOLanza().getId();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localId);
        var pedido = abrirPedido.ejecutar(null, pe.rikos.operacion.venta.aplicacion.Canal.PARA_LLEVAR, null, "almuerzo",
                ADMIN_SEMBRADO_ID).valorOLanza();
        String persona = personasDelPedido.findByPedidoIdOrderByOrden(pedido.getId()).get(0).getId();
        var linea = agregarLinea.ejecutar(new pe.rikos.operacion.venta.aplicacion.SolicitudDeLinea(null, pedido.getId(),
                persona, pe.rikos.operacion.venta.aplicacion.TipoDeLinea.EXTRA_LIBRE, null, 1, null,
                java.util.List.of(), "Huevo frito", 300L), ADMIN_SEMBRADO_ID).valorOLanza();
        var comanda = comandarPedido.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        registrarImpresion.ejecutar(comanda.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        registrarImpresion.ejecutar(comanda.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        var motivo = motivosDePrueba.save(new pe.rikos.operacion.catalogo.infraestructura.Motivo(
                pe.rikos.contratos.Identificador.nuevo().valor(), "anulacion", "Error del mozo " + pe.rikos.contratos.Identificador.nuevo().valor().substring(18), false));
        anularLinea.ejecutar(pedido.getId(), linea.getId(), motivo.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var descripciones = listarAuditoria.ejecutar(new ListarAuditoria.Filtro(null, null, null, null, null),
                PageRequest.of(0, 100)).getContent().stream()
                .filter(a -> comanda.getId().equals(a.entidadId()) || linea.getId().equals(a.entidadId()))
                .map(AuditoriaRespuesta::descripcion).toList();

        assertThat(descripciones).contains(
                "admin comandó el pedido \"0001\": 1 productos (comanda 1)",
                "admin reimprimió la comanda 1 del pedido \"0001\" (copia 2)",
                "admin anuló \"Huevo frito\" del pedido \"0001\" (después de comandar): " + motivo.getDescripcion());
    }

    @Test
    void reorganizarSeVeEnEspanolEnLaBitacora() {
        String codigo = "br" + pe.rikos.contratos.Identificador.nuevo().valor().substring(20).toLowerCase();
        String localId = crearLocalDePrueba.ejecutar(codigo, "Local " + codigo, "Dirección", ADMIN_SEMBRADO_ID)
                .valorOLanza().getId();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localId);
        var mesa3 = crearMesa.ejecutar(new pe.rikos.operacion.venta.aplicacion.DatosMesa("3", null, 4),
                ADMIN_SEMBRADO_ID).valorOLanza();
        var mesa4 = crearMesa.ejecutar(new pe.rikos.operacion.venta.aplicacion.DatosMesa("4", null, 4),
                ADMIN_SEMBRADO_ID).valorOLanza();
        var mesa5 = crearMesa.ejecutar(new pe.rikos.operacion.venta.aplicacion.DatosMesa("5", null, 4),
                ADMIN_SEMBRADO_ID).valorOLanza();
        var pedido = abrirPedido.ejecutar(null, pe.rikos.operacion.venta.aplicacion.Canal.MESA, mesa3.getId(),
                "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        String persona = personasDelPedido.findByPedidoIdOrderByOrden(pedido.getId()).get(0).getId();
        var linea = agregarLinea.ejecutar(new pe.rikos.operacion.venta.aplicacion.SolicitudDeLinea(null, pedido.getId(),
                persona, pe.rikos.operacion.venta.aplicacion.TipoDeLinea.EXTRA_LIBRE, null, 1, null,
                java.util.List.of(), "Huevo frito", 300L), ADMIN_SEMBRADO_ID).valorOLanza();

        trasladarLineas.ejecutar(pedido.getId(), java.util.List.of(linea.getId()), null, ADMIN_SEMBRADO_ID).valorOLanza();
        unirMesas.ejecutar(mesa4.getId(), mesa3.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        separarMesa.ejecutar(mesa4.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        moverPedidoDeMesa.ejecutar(pedido.getId(), mesa5.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        var descripciones = listarAuditoria.ejecutar(new ListarAuditoria.Filtro(null, null, null, null, null),
                PageRequest.of(0, 100)).getContent().stream()
                .filter(a -> pedido.getId().equals(a.entidadId()) || mesa4.getId().equals(a.entidadId()))
                .map(AuditoriaRespuesta::descripcion).toList();

        assertThat(descripciones).contains(
                "admin pasó 1 productos de \"Persona 1\" a \"Persona 2\" en el pedido \"0001\"",
                "admin unió \"Mesa 4\" a la mesa 3",
                "admin separó \"Mesa 4\" de la mesa 3",
                "admin movió el pedido \"0001\" de la mesa 3 a la mesa 5");
    }

    @Test
    void terminarUnPedidoSeVeComoCierreSinCobrarEnLaBitacora() {
        String codigo = "bt" + pe.rikos.contratos.Identificador.nuevo().valor().substring(20).toLowerCase();
        String localId = crearLocalDePrueba.ejecutar(codigo, "Local " + codigo, "Dirección", ADMIN_SEMBRADO_ID)
                .valorOLanza().getId();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localId);
        var mesa = crearMesa.ejecutar(new pe.rikos.operacion.venta.aplicacion.DatosMesa("8", null, 4),
                ADMIN_SEMBRADO_ID).valorOLanza();
        var pedido = abrirPedido.ejecutar(null, pe.rikos.operacion.venta.aplicacion.Canal.MESA, mesa.getId(),
                "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        terminarPedido.ejecutar(pedido.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        var descripciones = listarAuditoria.ejecutar(new ListarAuditoria.Filtro(null, null, null, null, null),
                PageRequest.of(0, 100)).getContent().stream().filter(a -> pedido.getId().equals(a.entidadId()))
                .map(AuditoriaRespuesta::descripcion).toList();

        assertThat(descripciones).contains("admin terminó el pedido \"0001\" sin cobrar (mesa 8 libre)");
    }
}

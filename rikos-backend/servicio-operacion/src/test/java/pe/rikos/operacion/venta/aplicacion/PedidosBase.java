package pe.rikos.operacion.venta.aplicacion;

import org.springframework.beans.factory.annotation.Autowired;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.pensionistas.aplicacion.Comida;
import pe.rikos.operacion.pensionistas.aplicacion.CrearPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.CrearPlanTarifario;
import pe.rikos.operacion.pensionistas.aplicacion.DatosPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.DatosResponsable;
import pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Piezas comunes de las pruebas de pedidos: locales, mesas, productos y pensionistas de ejemplo. */
public abstract class PedidosBase {

    public static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    /** Categorias sembradas (V15/V16): entrada, segundo. */
    public static final String CATEGORIA_ENTRADA = "01M39A0RZ0000000000000S001";
    public static final String CATEGORIA_SEGUNDO = "01M39A0RZ0000000000000S005";

    @Autowired
    protected CrearLocal crearLocal;
    @Autowired
    protected CrearMesa crearMesa;
    @Autowired
    protected ProductoRepositorio productos;
    @Autowired
    protected CategoriaProductoRepositorio categorias;
    @Autowired
    protected UnidadMedidaRepositorio unidades;
    @Autowired
    protected CrearPlanTarifario crearPlan;
    @Autowired
    protected CrearPensionista crearPensionista;

    protected String nuevoLocal(String prefijo) {
        String codigo = prefijo + Identificador.nuevo().valor().substring(20).toLowerCase();
        return crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID).valorOLanza().getId();
    }

    /** Requiere el local activo en el contexto de seguridad. */
    protected String nuevaMesa(String numero) {
        return crearMesa.ejecutar(new DatosMesa(numero, null, 4), ADMIN_SEMBRADO_ID).valorOLanza().getId();
    }

    /** Un producto vendible de la carta (sin marca de menu) con su precio en centimos; null = sin precio. */
    protected Producto productoDeCarta(String nombre, Long precio) {
        return producto(nombre, categoriaDeCarta(), precio, true);
    }

    protected Producto plato(String nombre, String categoriaId) {
        return producto(nombre, categorias.findById(categoriaId).orElseThrow(), 1500L, true);
    }

    private CategoriaProducto categoriaDeCarta() {
        return categorias.findAll().stream()
                .filter(c -> "producto_fijo".equals(c.getNaturaleza()) || "extra".equals(c.getNaturaleza()))
                .findFirst().orElseThrow();
    }

    private Producto producto(String nombre, CategoriaProducto categoria, Long precio, boolean vendible) {
        UnidadMedida und = unidades.findAll().stream().filter(u -> u.getCodigo().equals("und")).findFirst()
                .orElseThrow();
        String codigo = "PV-" + Identificador.nuevo().valor().substring(16);
        return productos.save(new Producto(Identificador.nuevo().valor(), codigo, nombre + " " + codigo, categoria,
                und, vendible, false, false, precio, false, null));
    }

    protected PlanTarifario nuevoPlan(long desayuno, long almuerzo, long cena) {
        return crearPlan.ejecutar("Plan pedido " + Identificador.nuevo().valor().substring(16), null,
                new PreciosPlan(desayuno, almuerzo, cena), null, ADMIN_SEMBRADO_ID).valorOLanza();
    }

    protected Pensionista nuevoPensionista(PlanTarifario plan, String apellido) {
        String dni = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        var datos = new DatosPensionista(dni, "Luis", apellido, null, null, null, null, null, null, plan.getId(),
                Set.of(Comida.ALMUERZO), null,
                new DatosResponsable("Rosa " + apellido, "12345678", "999", "r@c.pe", "Madre", "Av. 1"));
        return crearPensionista.ejecutar(datos, ADMIN_SEMBRADO_ID).valorOLanza().pensionista();
    }
}

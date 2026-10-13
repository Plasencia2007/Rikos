package pe.rikos.operacion.inventario.aplicacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.catalogo.aplicacion.CalculadoraCostoReceta;
import pe.rikos.operacion.catalogo.infraestructura.IngredienteReceta;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Receta;
import pe.rikos.operacion.catalogo.infraestructura.RecetaRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;
import pe.rikos.operacion.produccion.dominio.Produccion;
import pe.rikos.operacion.produccion.infraestructura.ProduccionRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * US-048 (CA2 / Caso Crítico 11 / RN-065 / RN-057):
 * Descuenta del inventario cuando las líneas del pedido se comandan a cocina.
 * Si el plato tiene receta:
 *   - Si un ingrediente es subreceta, se descuenta directamente de su stock sin duplicar
 *     el consumo de los insumos que ya se gastaron al prepararla.
 *   - Si el ingrediente es insumo regular, se descuenta proporcionalmente a las porciones de la receta.
 * Si el producto no tiene receta (bebidas o productos directos), se descuenta 1 a 1 de su existencia.
 * Registra movimientos en Kardex con origen "venta" y referencia a la comanda.
 * Incrementa el contador de porciones vendidas en la producción del día si existe.
 */
@Service
public class DescontarInventarioPorComanda {

    private final ProductoRepositorio productos;
    private final RecetaRepositorio recetas;
    private final ExistenciaRepositorio existencias;
    private final MovimientoInventarioRepositorio movimientos;
    private final LocalRepositorio locales;
    private final UsuarioRepositorio usuarios;
    private final ProduccionRepositorio producciones;
    private final CalculadoraCostoReceta calculadoraCostoReceta;

    @Autowired
    public DescontarInventarioPorComanda(ProductoRepositorio productos,
                                         RecetaRepositorio recetas,
                                         ExistenciaRepositorio existencias,
                                         MovimientoInventarioRepositorio movimientos,
                                         LocalRepositorio locales,
                                         UsuarioRepositorio usuarios,
                                         ProduccionRepositorio producciones,
                                         CalculadoraCostoReceta calculadoraCostoReceta) {
        this.productos = productos;
        this.recetas = recetas;
        this.existencias = existencias;
        this.movimientos = movimientos;
        this.locales = locales;
        this.usuarios = usuarios;
        this.producciones = producciones;
        this.calculadoraCostoReceta = calculadoraCostoReceta;
    }

    @Transactional
    public void descontar(String localId, String comandaId, List<DetallePedido> lineas, String actorId) {
        if (localId == null || lineas == null || lineas.isEmpty()) {
            return;
        }

        Local local = locales.findById(localId).orElse(null);
        Usuario usuario = actorId != null ? usuarios.findById(actorId).orElse(null) : null;
        if (local == null) {
            return;
        }

        LocalDate hoy = LocalDate.now();

        for (DetallePedido linea : lineas) {
            if (linea.getProductoId() == null || linea.getCantidad() <= 0) {
                continue;
            }

            Producto plato = productos.findById(linea.getProductoId()).orElse(null);
            if (plato == null) {
                continue;
            }

            // 1. Seguimiento de porciones vendidas en la producción del día si existe (CA3)
            Optional<Produccion> prodOpt = producciones.findFirstByLocalIdAndProductoIdAndFechaOrderByCreadoEnDesc(
                    localId, plato.getId(), hoy
            );
            if (prodOpt.isPresent()) {
                Produccion prod = prodOpt.get();
                prod.registrarVentaPorciones(linea.getCantidad());
                producciones.save(prod);
            }

            // 2. Descuento de inventario según receta o producto directo
            Optional<Receta> recetaOpt = recetas.findByProductoIdAndActivoTrue(plato.getId());
            if (recetaOpt.isPresent()) {
                Receta receta = recetaOpt.get();
                int porciones = receta.getPorciones() > 0 ? receta.getPorciones() : 1;

                List<IngredienteReceta> ingredientes = receta.getIngredientes();
                if (ingredientes != null) {
                    for (IngredienteReceta ing : ingredientes) {
                        Producto itemIngrediente = ing.getProducto();
                        if (itemIngrediente == null) continue;

                        BigDecimal cantBaseReceta = calculadoraCostoReceta.aUnidadBase(
                                itemIngrediente, ing.getCantidad(), ing.getUnidad()
                        );
                        if (cantBaseReceta == null) {
                            cantBaseReceta = ing.getCantidad();
                        }

                        BigDecimal porPorcion = cantBaseReceta.divide(BigDecimal.valueOf(porciones), 8, RoundingMode.HALF_UP);
                        BigDecimal cantidadTotalConsumida = porPorcion.multiply(BigDecimal.valueOf(linea.getCantidad()))
                                .setScale(6, RoundingMode.HALF_UP);

                        descontarStock(local, itemIngrediente, cantidadTotalConsumida, comandaId,
                                "Consumo por venta: " + plato.getNombre() + " (x" + linea.getCantidad() + ")", usuario);
                    }
                }
            } else if (plato.isEsInsumo() || plato.isEsVendible()) {
                // Producto directo sin receta (bebida, postre empacado, etc.)
                BigDecimal cantidadVendida = BigDecimal.valueOf(linea.getCantidad()).setScale(6, RoundingMode.HALF_UP);
                descontarStock(local, plato, cantidadVendida, comandaId,
                        "Venta directa: " + plato.getNombre() + " (x" + linea.getCantidad() + ")", usuario);
            }
        }
    }

    private void descontarStock(Local local, Producto item, BigDecimal cantidad, String comandaId,
                                String observacion, Usuario usuario) {
        Existencia ex = existencias.findByLocalIdAndProductoId(local.getId(), item.getId())
                .orElseGet(() -> new Existencia(local.getId(), item.getId(), null));

        BigDecimal stockAnterior = ex.getStockActual() != null ? ex.getStockActual() : BigDecimal.ZERO;
        BigDecimal stockNuevo = stockAnterior.subtract(cantidad);
        BigDecimal costoUnitario = item.getCostoUltimaCompra() != null ? item.getCostoUltimaCompra() : BigDecimal.ZERO;

        MovimientoInventario mov = new MovimientoInventario(
                Identificador.nuevo().valor(),
                local,
                item,
                "venta",
                "salida",
                cantidad,
                item.getUnidadBase(),
                cantidad,
                costoUnitario,
                stockAnterior,
                stockNuevo,
                "comanda",
                comandaId,
                null,
                observacion,
                OffsetDateTime.now(ZoneOffset.UTC),
                usuario
        );
        MovimientoInventario movGuardado = movimientos.saveAndFlush(mov);

        ex.actualizarStock(stockNuevo, movGuardado);
        existencias.saveAndFlush(ex);
    }
}

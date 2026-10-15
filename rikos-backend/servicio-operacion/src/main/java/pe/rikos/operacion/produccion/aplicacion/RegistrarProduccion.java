package pe.rikos.operacion.produccion.aplicacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.catalogo.aplicacion.CalculadoraCostoReceta;
import pe.rikos.operacion.catalogo.infraestructura.IngredienteReceta;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Receta;
import pe.rikos.operacion.catalogo.infraestructura.RecetaRepositorio;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;
import pe.rikos.operacion.produccion.aplicacion.dto.DetalleProduccionDTO;
import pe.rikos.operacion.produccion.aplicacion.dto.RegistroProduccionComando;
import pe.rikos.operacion.produccion.dominio.ErrorProduccion;
import pe.rikos.operacion.produccion.dominio.Produccion;
import pe.rikos.operacion.produccion.infraestructura.ProduccionRepositorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class RegistrarProduccion {

    private final ProduccionRepositorio producciones;
    private final ProductoRepositorio productos;
    private final RecetaRepositorio recetas;
    private final LocalRepositorio locales;
    private final UsuarioRepositorio usuarios;
    private final ExistenciaRepositorio existencias;
    private final MovimientoInventarioRepositorio movimientos;
    private final CalculadoraCostoReceta calculadoraCostoReceta;

    @Autowired
    public RegistrarProduccion(ProduccionRepositorio producciones,
                               ProductoRepositorio productos,
                               RecetaRepositorio recetas,
                               LocalRepositorio locales,
                               UsuarioRepositorio usuarios,
                               ExistenciaRepositorio existencias,
                               MovimientoInventarioRepositorio movimientos,
                               CalculadoraCostoReceta calculadoraCostoReceta) {
        this.producciones = producciones;
        this.productos = productos;
        this.recetas = recetas;
        this.locales = locales;
        this.usuarios = usuarios;
        this.existencias = existencias;
        this.movimientos = movimientos;
        this.calculadoraCostoReceta = calculadoraCostoReceta;
    }

    public RegistrarProduccion(ProduccionRepositorio producciones,
                               ProductoRepositorio productos,
                               RecetaRepositorio recetas,
                               LocalRepositorio locales,
                               UsuarioRepositorio usuarios,
                               ExistenciaRepositorio existencias,
                               MovimientoInventarioRepositorio movimientos) {
        this(producciones, productos, recetas, locales, usuarios, existencias, movimientos, null);
    }

    @Transactional
    public Resultado<DetalleProduccionDTO, ErrorProduccion> ejecutar(RegistroProduccionComando comando) {
        if (comando.localId() == null || comando.localId().isBlank()) {
            return Resultado.fracaso(ErrorProduccion.LOCAL_NO_ENCONTRADO);
        }
        Local local = locales.findById(comando.localId()).orElse(null);
        if (local == null) {
            return Resultado.fracaso(ErrorProduccion.LOCAL_NO_ENCONTRADO);
        }
        if (!local.isActivo()) {
            return Resultado.fracaso(ErrorProduccion.LOCAL_INACTIVO);
        }

        if (comando.usuarioId() == null || comando.usuarioId().isBlank()) {
            return Resultado.fracaso(ErrorProduccion.USUARIO_NO_ENCONTRADO);
        }
        Usuario usuario = usuarios.findById(comando.usuarioId()).orElse(null);
        if (usuario == null) {
            return Resultado.fracaso(ErrorProduccion.USUARIO_NO_ENCONTRADO);
        }

        if (comando.fecha() == null) {
            return Resultado.fracaso(ErrorProduccion.FECHA_REQUERIDA);
        }

        if (comando.productoId() == null || comando.productoId().isBlank()) {
            return Resultado.fracaso(ErrorProduccion.PRODUCTO_NO_ENCONTRADO);
        }
        Producto producto = productos.findById(comando.productoId()).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorProduccion.PRODUCTO_NO_ENCONTRADO);
        }
        if (!producto.isActivo()) {
            return Resultado.fracaso(ErrorProduccion.PRODUCTO_INACTIVO);
        }

        if (comando.rendimientoObtenido() == null || comando.rendimientoObtenido().compareTo(BigDecimal.ZERO) <= 0) {
            return Resultado.fracaso(ErrorProduccion.RENDIMIENTO_INVALIDO);
        }
        if (comando.porcionesPreparadas() <= 0) {
            return Resultado.fracaso(ErrorProduccion.PORCIONES_INVALIDAS);
        }

        // Obtener receta
        Receta receta;
        if (comando.recetaId() != null && !comando.recetaId().isBlank()) {
            receta = recetas.findById(comando.recetaId()).orElse(null);
        } else {
            receta = recetas.findByProductoIdAndActivoTrue(producto.getId()).orElse(null);
        }

        if (receta == null) {
            return Resultado.fracaso(ErrorProduccion.RECETA_NO_ENCONTRADA);
        }
        if (!receta.isActivo()) {
            return Resultado.fracaso(ErrorProduccion.RECETA_INACTIVA);
        }
        if (!receta.getProducto().getId().equals(producto.getId())) {
            return Resultado.fracaso(ErrorProduccion.RECETA_NO_CORRESPONDE_A_PRODUCTO);
        }

        String produccionId = Identificador.nuevo().valor();
        BigDecimal rendimientoBase = receta.getRendimiento();
        if (rendimientoBase == null || rendimientoBase.compareTo(BigDecimal.ZERO) <= 0) {
            return Resultado.fracaso(ErrorProduccion.RENDIMIENTO_INVALIDO);
        }

        // Factor de escala: rendimientoObtenido / rendimientoBase (D3)
        BigDecimal factor = comando.rendimientoObtenido().divide(rendimientoBase, 8, RoundingMode.HALF_UP);

        BigDecimal costoTotalLote = BigDecimal.ZERO;
        List<IngredienteReceta> ingredientes = receta.getIngredientes();

        // 1. Salida de cada insumo consumido
        if (ingredientes != null) {
            for (IngredienteReceta ing : ingredientes) {
                Producto insumo = ing.getProducto();
                BigDecimal cantidadBaseConsumida = ing.getCantidad().multiply(factor).setScale(6, RoundingMode.HALF_UP);

                Existencia exInsumo = existencias.findByLocalIdAndProductoId(local.getId(), insumo.getId())
                        .orElseGet(() -> new Existencia(local.getId(), insumo.getId(), null));

                BigDecimal stockAnterior = exInsumo.getStockActual() != null ? exInsumo.getStockActual() : BigDecimal.ZERO;
                BigDecimal stockNuevo = stockAnterior.subtract(cantidadBaseConsumida);

                BigDecimal costoInsumoUnitario = insumo.getCostoUltimaCompra() != null ? insumo.getCostoUltimaCompra() : BigDecimal.ZERO;
                BigDecimal costoInsumoTotal = cantidadBaseConsumida.multiply(costoInsumoUnitario).setScale(6, RoundingMode.HALF_UP);
                costoTotalLote = costoTotalLote.add(costoInsumoTotal);

                OffsetDateTime ocurridoEn = comando.fecha() != null
                        ? comando.fecha().atStartOfDay().atOffset(ZoneOffset.UTC)
                        : OffsetDateTime.now(ZoneOffset.UTC);

                MovimientoInventario movInsumo = new MovimientoInventario(
                        Identificador.nuevo().valor(),
                        local,
                        insumo,
                        "produccion",
                        "salida",
                        cantidadBaseConsumida,
                        insumo.getUnidadBase(),
                        cantidadBaseConsumida,
                        costoInsumoUnitario,
                        stockAnterior,
                        stockNuevo,
                        "produccion",
                        produccionId,
                        null,
                        "Consumo para producción de " + producto.getNombre(),
                        ocurridoEn,
                        usuario
                );
                MovimientoInventario movInsumoGuardado = movimientos.saveAndFlush(movInsumo);

                exInsumo.actualizarStock(stockNuevo, movInsumoGuardado);
                existencias.saveAndFlush(exInsumo);
            }
        }

        // 2. Entrada de la subreceta o plato producido
        Existencia exProd = existencias.findByLocalIdAndProductoId(local.getId(), producto.getId())
                .orElseGet(() -> new Existencia(local.getId(), producto.getId(), null));

        BigDecimal stockAntProd = exProd.getStockActual() != null ? exProd.getStockActual() : BigDecimal.ZERO;
        BigDecimal stockNuevoProd = stockAntProd.add(comando.rendimientoObtenido());

        BigDecimal costoUnitarioReal = costoTotalLote.compareTo(BigDecimal.ZERO) > 0
                ? costoTotalLote.divide(comando.rendimientoObtenido(), 6, RoundingMode.HALF_UP)
                : (producto.getCostoUltimaCompra() != null ? producto.getCostoUltimaCompra() : BigDecimal.ZERO);

        OffsetDateTime ocurridoEnEntrada = comando.fecha() != null
                ? comando.fecha().atStartOfDay().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now(ZoneOffset.UTC);

        MovimientoInventario movEntrada = new MovimientoInventario(
                Identificador.nuevo().valor(),
                local,
                producto,
                "produccion",
                "entrada",
                comando.rendimientoObtenido(),
                producto.getUnidadBase(),
                comando.rendimientoObtenido(),
                costoUnitarioReal,
                stockAntProd,
                stockNuevoProd,
                "produccion",
                produccionId,
                null,
                "Ingreso por producción (" + comando.porcionesPreparadas() + " porciones)",
                ocurridoEnEntrada,
                usuario
        );
        MovimientoInventario movEntradaGuardado = movimientos.saveAndFlush(movEntrada);

        exProd.actualizarStock(stockNuevoProd, movEntradaGuardado);
        existencias.saveAndFlush(exProd);

        // Actualizar costo de referencia del producto en catálogo (D5 / RN-047)
        producto.registrarCostoDeProduccion(costoUnitarioReal, comando.fecha(), usuario.getId());
        productos.save(producto);

        if (calculadoraCostoReceta != null) {
            calculadoraCostoReceta.recalcularEnCascada(producto.getId());
        }

        // 3. Crear entidad Produccion
        Produccion produccion = new Produccion(
                produccionId,
                local,
                producto,
                receta,
                comando.fecha(),
                comando.servicio(),
                comando.porcionesPlanificadas(),
                comando.porcionesPreparadas(),
                comando.rendimientoObtenido(),
                usuario
        );
        Produccion guardada = producciones.save(produccion);

        return Resultado.exito(mapearDetalleDTO(guardada, costoUnitarioReal, costoTotalLote));
    }

    public static DetalleProduccionDTO mapearDetalle(Produccion p) {
        BigDecimal costoUnitario = p.getProducto().getCostoUltimaCompra() != null ? p.getProducto().getCostoUltimaCompra() : BigDecimal.ZERO;
        BigDecimal costoTotal = costoUnitario.multiply(p.getRendimientoObtenido() != null ? p.getRendimientoObtenido() : BigDecimal.ZERO);
        return mapearDetalleDTO(p, costoUnitario, costoTotal);
    }

    public static DetalleProduccionDTO mapearDetalleDTO(Produccion p, BigDecimal costoUnitario, BigDecimal costoTotal) {
        return new DetalleProduccionDTO(
                p.getId(),
                p.getLocal().getId(),
                p.getProducto().getId(),
                p.getProducto().getCodigo(),
                p.getProducto().getNombre(),
                p.getProducto().getUnidadBase() != null ? p.getProducto().getUnidadBase().getCodigo() : "und",
                p.getProducto().isEsSubreceta(),
                p.getReceta().getId(),
                p.getFecha(),
                p.getServicio(),
                p.getPorcionesPlanificadas(),
                p.getPorcionesPreparadas(),
                p.getPorcionesVendidas(),
                p.getRendimientoObtenido(),
                costoUnitario,
                costoTotal,
                p.getEstado(),
                p.getRegistradoPor() != null ? p.getRegistradoPor().getNombreCompleto() : "Sistema",
                p.getCreadoEn(),
                p.getSobrante(),
                p.getDestinoSobrante(),
                p.getMotivo() != null ? p.getMotivo().getId() : null,
                p.getMotivo() != null ? p.getMotivo().getDescripcion() : null
        );
    }
}

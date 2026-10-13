package pe.rikos.operacion.compras.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.CalculadoraCostoReceta;
import pe.rikos.operacion.catalogo.infraestructura.PresentacionCompra;
import pe.rikos.operacion.catalogo.infraestructura.PresentacionCompraRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.catalogo.infraestructura.ProveedorRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.compras.aplicacion.dto.FilaCompraDto;
import pe.rikos.operacion.compras.aplicacion.dto.FilaDetalleCompraDto;
import pe.rikos.operacion.compras.aplicacion.dto.ItemDetalleCompraDto;
import pe.rikos.operacion.compras.aplicacion.dto.SolicitudRegistroCompra;
import pe.rikos.operacion.compras.dominio.Compra;
import pe.rikos.operacion.compras.dominio.CompraRepositorio;
import pe.rikos.operacion.compras.dominio.DetalleCompra;
import pe.rikos.operacion.compras.dominio.DetalleCompraRepositorio;
import pe.rikos.operacion.compras.dominio.EstadoCompra;
import pe.rikos.operacion.compras.dominio.TipoDocumentoCompra;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Permite registrar una compra en estado borrador o confirmada directamente (US-050 CA1, CA2).
 */
@Service
public class RegistrarCompra {

    private final CompraRepositorio compras;
    private final DetalleCompraRepositorio detalles;
    private final LocalRepositorio locales;
    private final ProveedorRepositorio proveedores;
    private final ProductoRepositorio productos;
    private final PresentacionCompraRepositorio presentaciones;
    private final ExistenciaRepositorio existencias;
    private final MovimientoInventarioRepositorio movimientos;
    private final UsuarioRepositorio usuarios;
    private final CalculadoraCostoReceta calculadoraCostoReceta;
    private final RegistradorAuditoria auditoria;

    public RegistrarCompra(CompraRepositorio compras, DetalleCompraRepositorio detalles,
                           LocalRepositorio locales, ProveedorRepositorio proveedores,
                           ProductoRepositorio productos, PresentacionCompraRepositorio presentaciones,
                           ExistenciaRepositorio existencias, MovimientoInventarioRepositorio movimientos,
                           UsuarioRepositorio usuarios,
                           CalculadoraCostoReceta calculadoraCostoReceta, RegistradorAuditoria auditoria) {
        this.compras = compras;
        this.detalles = detalles;
        this.locales = locales;
        this.proveedores = proveedores;
        this.productos = productos;
        this.presentaciones = presentaciones;
        this.existencias = existencias;
        this.movimientos = movimientos;
        this.usuarios = usuarios;
        this.calculadoraCostoReceta = calculadoraCostoReceta;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<FilaCompraDto, ErrorCompra> ejecutar(String localId, SolicitudRegistroCompra solicitud, String usuarioId) {
        Local local = locales.findById(localId).orElse(null);
        if (local == null) {
            return Resultado.fracaso(ErrorCompra.LOCAL_NO_ENCONTRADO);
        }
        if (!local.isActivo()) {
            return Resultado.fracaso(ErrorCompra.LOCAL_INACTIVO);
        }

        if (solicitud.proveedorId() == null) {
            return Resultado.fracaso(ErrorCompra.PROVEEDOR_NO_ENCONTRADO);
        }
        Proveedor proveedor = proveedores.findById(solicitud.proveedorId()).orElse(null);
        if (proveedor == null) {
            return Resultado.fracaso(ErrorCompra.PROVEEDOR_NO_ENCONTRADO);
        }
        if (!proveedor.isActivo()) {
            return Resultado.fracaso(ErrorCompra.PROVEEDOR_INACTIVO);
        }

        if (solicitud.tipoDocumento() == null) {
            return Resultado.fracaso(ErrorCompra.TIPO_DOCUMENTO_REQUERIDO);
        }

        if (solicitud.tipoDocumento() != TipoDocumentoCompra.SIN_DOCUMENTO) {
            if (solicitud.serie() == null || solicitud.serie().isBlank() ||
                solicitud.numero() == null || solicitud.numero().isBlank()) {
                return Resultado.fracaso(ErrorCompra.SERIE_NUMERO_REQUERIDOS);
            }
        }

        if (solicitud.fechaDocumento() == null) {
            return Resultado.fracaso(ErrorCompra.FECHA_DOCUMENTO_REQUERIDA);
        }

        if (solicitud.detalles() == null || solicitud.detalles().isEmpty()) {
            return Resultado.fracaso(ErrorCompra.DETALLES_VACIOS);
        }

        if (solicitud.tipoDocumento() != TipoDocumentoCompra.SIN_DOCUMENTO) {
            boolean duplicado = compras.existeComprobanteDuplicado(
                    proveedor.getId(), solicitud.tipoDocumento(), solicitud.serie(), solicitud.numero(),
                    EstadoCompra.ANULADA, null);
            if (duplicado) {
                return Resultado.fracaso(ErrorCompra.COMPROBANTE_DUPLICADO);
            }
        }

        String compraId = Identificador.nuevo().valor();
        Compra compra = new Compra(
                compraId, local, proveedor, solicitud.tipoDocumento(),
                solicitud.serie(), solicitud.numero(), solicitud.fechaDocumento(),
                0L, solicitud.observacion(), usuarioId
        );

        long totalCalculado = 0L;
        List<DetalleCompra> listaDetalles = new ArrayList<>();

        for (ItemDetalleCompraDto item : solicitud.detalles()) {
            if (item.productoId() == null) {
                return Resultado.fracaso(ErrorCompra.PRODUCTO_NO_ENCONTRADO);
            }
            Producto prod = productos.findById(item.productoId()).orElse(null);
            if (prod == null) {
                return Resultado.fracaso(ErrorCompra.PRODUCTO_NO_ENCONTRADO);
            }
            if (!prod.isActivo()) {
                return Resultado.fracaso(ErrorCompra.PRODUCTO_INACTIVO);
            }

            if (item.cantidad() == null || item.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
                return Resultado.fracaso(ErrorCompra.CANTIDAD_INVALIDA);
            }
            if (item.precioUnitario() == null || item.precioUnitario() < 0) {
                return Resultado.fracaso(ErrorCompra.PRECIO_INVALIDO);
            }

            PresentacionCompra pres = null;
            BigDecimal cantidadBase = item.cantidad();
            if (item.presentacionId() != null && !item.presentacionId().isBlank()) {
                pres = presentaciones.findById(item.presentacionId()).orElse(null);
                if (pres == null || !pres.getProducto().getId().equals(prod.getId())) {
                    return Resultado.fracaso(ErrorCompra.PRESENTACION_NO_PERTENECE);
                }
                cantidadBase = item.cantidad().multiply(pres.getFactorAUnidadBase());
            }

            long subtotal = BigDecimal.valueOf(item.precioUnitario())
                    .multiply(item.cantidad())
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            DetalleCompra d = new DetalleCompra(
                    Identificador.nuevo().valor(), prod, pres, item.cantidad(), cantidadBase,
                    item.precioUnitario(), subtotal
            );
            compra.agregarDetalle(d);
            listaDetalles.add(d);
            totalCalculado += subtotal;
        }

        compra.recalcularTotal();
        Compra guardada = compras.saveAndFlush(compra);

        if (solicitud.confirmarInmediatamente()) {
            aplicarConfirmacion(guardada, local, usuarioId);
            guardada = compras.saveAndFlush(guardada);
        }

        auditoria.registrar("registro_compra", "compra", guardada.getId(),
                null,
                Map.of("total", guardada.getTotal(), "estado", guardada.getEstado().getCodigo()));

        return Resultado.exito(mapearFilaCompra(guardada));
    }

    void aplicarConfirmacion(Compra compra, Local local, String usuarioId) {
        compra.confirmar(usuarioId);
        OffsetDateTime ahora = OffsetDateTime.now();
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);

        for (DetalleCompra d : compra.getDetalles()) {
            Producto prod = d.getProducto();
            BigDecimal cantidadBase = d.getCantidadBase();

            Existencia existencia = existencias.findByLocalIdAndProductoId(local.getId(), prod.getId()).orElse(null);
            BigDecimal stockAnterior = existencia != null ? existencia.getStockActual() : BigDecimal.ZERO;
            BigDecimal stockNuevo = stockAnterior.add(cantidadBase);

            BigDecimal costoUnitarioBase = BigDecimal.valueOf(d.getSubtotal())
                    .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
                    .divide(cantidadBase, 6, RoundingMode.HALF_UP);

            UnidadMedida unidadMovimiento = d.getPresentacion() != null
                    ? d.getPresentacion().getUnidad()
                    : prod.getUnidadBase();

            String movId = Identificador.nuevo().valor();
            MovimientoInventario mov = new MovimientoInventario(
                    movId, local, prod,
                    "compra", "entrada",
                    d.getCantidad(),
                    unidadMovimiento,
                    cantidadBase,
                    costoUnitarioBase,
                    stockAnterior, stockNuevo,
                    "compra", compra.getId(),
                    null,
                    "Ingreso por compra " + (compra.getSerie() != null ? compra.getSerie() + "-" + compra.getNumero() : compra.getId()),
                    compra.getFechaDocumento().atStartOfDay().atOffset(ahora.getOffset()),
                    usuario
            );
            MovimientoInventario movGuardado = movimientos.saveAndFlush(mov);
            d.asignarMovimiento(movGuardado);

            if (existencia == null) {
                existencia = new Existencia(local.getId(), prod.getId(), null);
            }
            existencia.actualizarStock(stockNuevo, movGuardado);
            existencias.saveAndFlush(existencia);

            // RN-047: Actualizar costo de última compra en producto
            prod.registrarCostoDeCompra(costoUnitarioBase, compra.getFechaDocumento(), usuarioId);
            productos.saveAndFlush(prod);

            // CA3: Recalcular en cascada las recetas futuras que usan este producto
            calculadoraCostoReceta.recalcularEnCascada(prod.getId());
        }
    }

    public static FilaCompraDto mapearFilaCompra(Compra compra) {
        List<FilaDetalleCompraDto> filasDetalle = compra.getDetalles().stream().map(d -> new FilaDetalleCompraDto(
                d.getId(),
                d.getProducto().getId(),
                d.getProducto().getCodigo(),
                d.getProducto().getNombre(),
                d.getProducto().getUnidadBase() != null ? d.getProducto().getUnidadBase().getCodigo() : "und",
                d.getPresentacion() != null ? d.getPresentacion().getId() : null,
                d.getPresentacion() != null ? d.getPresentacion().getNombre() : null,
                d.getCantidad(),
                d.getCantidadBase(),
                d.getPrecioUnitario(),
                d.getSubtotal()
        )).toList();

        return new FilaCompraDto(
                compra.getId(),
                compra.getLocal().getId(),
                compra.getProveedor().getId(),
                compra.getProveedor().getRazonSocial(),
                compra.getProveedor().getRuc(),
                compra.getTipoDocumento(),
                compra.getSerie(),
                compra.getNumero(),
                compra.getFechaDocumento(),
                compra.getTotal(),
                compra.getRutaAdjunto(),
                compra.getEstado(),
                compra.getConfirmadaEn(),
                compra.getObservacion(),
                compra.getCreadoEn(),
                filasDetalle
        );
    }
}

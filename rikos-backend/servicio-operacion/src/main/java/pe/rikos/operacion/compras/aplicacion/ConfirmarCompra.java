package pe.rikos.operacion.compras.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.CalculadoraCostoReceta;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.compras.aplicacion.dto.FilaCompraDto;
import pe.rikos.operacion.compras.dominio.Compra;
import pe.rikos.operacion.compras.dominio.CompraRepositorio;
import pe.rikos.operacion.compras.dominio.DetalleCompra;
import pe.rikos.operacion.compras.dominio.DetalleCompraRepositorio;
import pe.rikos.operacion.compras.dominio.EstadoCompra;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Confirma una compra en estado borrador aplicando movimientos de inventario,
 * existencias y recálculo de recetas (US-050 CA2, CA3).
 */
@Service
public class ConfirmarCompra {

    private final CompraRepositorio compras;
    private final DetalleCompraRepositorio detalles;
    private final ProductoRepositorio productos;
    private final ExistenciaRepositorio existencias;
    private final MovimientoInventarioRepositorio movimientos;
    private final UsuarioRepositorio usuarios;
    private final CalculadoraCostoReceta calculadoraCostoReceta;
    private final RegistradorAuditoria auditoria;

    public ConfirmarCompra(CompraRepositorio compras, DetalleCompraRepositorio detalles,
                           ProductoRepositorio productos, ExistenciaRepositorio existencias,
                           MovimientoInventarioRepositorio movimientos, UsuarioRepositorio usuarios,
                           CalculadoraCostoReceta calculadoraCostoReceta, RegistradorAuditoria auditoria) {
        this.compras = compras;
        this.detalles = detalles;
        this.productos = productos;
        this.existencias = existencias;
        this.movimientos = movimientos;
        this.usuarios = usuarios;
        this.calculadoraCostoReceta = calculadoraCostoReceta;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<FilaCompraDto, ErrorCompra> ejecutar(String localId, String compraId, String usuarioId) {
        Compra compra = compras.findByIdAndLocalId(compraId, localId).orElse(null);
        if (compra == null) {
            return Resultado.fracaso(ErrorCompra.COMPRA_NO_ENCONTRADA);
        }

        if (compra.getEstado() != EstadoCompra.BORRADOR) {
            return Resultado.fracaso(ErrorCompra.ESTADO_INVALIDO);
        }

        if (compra.getDetalles().isEmpty()) {
            return Resultado.fracaso(ErrorCompra.DETALLES_VACIOS);
        }

        compra.confirmar(usuarioId);
        OffsetDateTime ahora = OffsetDateTime.now();
        Local local = compra.getLocal();
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);

        for (DetalleCompra d : compra.getDetalles()) {
            Producto prod = d.getProducto();
            BigDecimal cantidadBase = d.getCantidadBase();

            Existencia existencia = existencias.findByLocalIdAndProductoId(localId, prod.getId()).orElse(null);
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
                existencia = new Existencia(localId, prod.getId(), null);
            }
            existencia.actualizarStock(stockNuevo, movGuardado);
            existencias.saveAndFlush(existencia);

            // RN-047: Actualizar costo de última compra en producto
            prod.registrarCostoDeCompra(costoUnitarioBase, compra.getFechaDocumento(), usuarioId);
            productos.saveAndFlush(prod);

            // CA3: Recalcular en cascada las recetas futuras que usan este producto
            calculadoraCostoReceta.recalcularEnCascada(prod.getId());
        }

        Compra guardada = compras.saveAndFlush(compra);

        auditoria.registrar("confirmacion_compra", "compra", guardada.getId(),
                Map.of("estado", "borrador"),
                Map.of("estado", "confirmada"));

        return Resultado.exito(RegistrarCompra.mapearFilaCompra(guardada));
    }
}

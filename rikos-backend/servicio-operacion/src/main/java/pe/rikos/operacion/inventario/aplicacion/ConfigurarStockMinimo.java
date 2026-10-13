package pe.rikos.operacion.inventario.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.inventario.aplicacion.dto.FilaExistencia;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Permite al administrador configurar o actualizar el stock mínimo de un producto en un local (RN-051, US-043 CA2).
 */
@Service
public class ConfigurarStockMinimo {

    private final LocalRepositorio locales;
    private final ProductoRepositorio productos;
    private final ExistenciaRepositorio existencias;
    private final RegistradorAuditoria auditoria;

    public ConfigurarStockMinimo(LocalRepositorio locales, ProductoRepositorio productos,
                                 ExistenciaRepositorio existencias, RegistradorAuditoria auditoria) {
        this.locales = locales;
        this.productos = productos;
        this.existencias = existencias;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<FilaExistencia, ErrorInventario> ejecutar(String localId, String productoId,
                                                               BigDecimal nuevoMinimo, String actorId) {
        Local local = locales.findById(localId).orElse(null);
        if (local == null) {
            return Resultado.fracaso(ErrorInventario.LOCAL_NO_ENCONTRADO);
        }
        if (!local.isActivo()) {
            return Resultado.fracaso(ErrorInventario.LOCAL_INACTIVO);
        }

        Producto producto = productos.findById(productoId).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorInventario.PRODUCTO_NO_ENCONTRADO);
        }
        if (!producto.isActivo()) {
            return Resultado.fracaso(ErrorInventario.PRODUCTO_INACTIVO);
        }

        if (nuevoMinimo != null && nuevoMinimo.compareTo(BigDecimal.ZERO) < 0) {
            return Resultado.fracaso(ErrorInventario.STOCK_MINIMO_NEGATIVO);
        }

        Existencia existencia = existencias.findByLocalIdAndProductoId(localId, productoId).orElse(null);
        Existencia guardada;

        if (existencia != null) {
            BigDecimal anterior = existencia.getStockMinimo();
            existencia.actualizarStockMinimo(nuevoMinimo);
            guardada = existencias.save(existencia);

            auditoria.registrar("cambio_stock_minimo", "existencia", productoId,
                    anterior != null ? Map.of("stockMinimo", anterior) : null,
                    nuevoMinimo != null ? Map.of("stockMinimo", nuevoMinimo) : null);
        } else {
            Existencia nueva = new Existencia(localId, productoId, nuevoMinimo);
            guardada = existencias.save(nueva);

            auditoria.registrar("cambio_stock_minimo", "existencia", productoId,
                    null,
                    nuevoMinimo != null ? Map.of("stockMinimo", nuevoMinimo) : null);
        }

        FilaExistencia fila = new FilaExistencia(
                localId,
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre(),
                producto.getUnidadBase().getId(),
                producto.getUnidadBase().getCodigo(),
                producto.getUnidadBase().getNombre(),
                producto.isControlaStock(),
                guardada.getStockActual(),
                guardada.getStockMinimo(),
                guardada.getEstadoAlerta(),
                guardada.getRecalculadoEn()
        );

        return Resultado.exito(fila);
    }
}

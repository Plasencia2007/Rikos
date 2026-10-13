package pe.rikos.operacion.inventario.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.inventario.aplicacion.dto.FilaExistencia;
import pe.rikos.operacion.inventario.aplicacion.dto.ResumenAlertasInventario;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consulta de existencias proyectadas y alertas por local (ADM-16, US-043 CA1-CA4).
 * Cruza el catálogo activo con las existencias persistidas para inicialización on-demand.
 */
@Service
public class ConsultarExistencias {

    private final LocalRepositorio locales;
    private final ProductoRepositorio productos;
    private final ExistenciaRepositorio existencias;

    public ConsultarExistencias(LocalRepositorio locales, ProductoRepositorio productos,
                                ExistenciaRepositorio existencias) {
        this.locales = locales;
        this.productos = productos;
        this.existencias = existencias;
    }

    @Transactional(readOnly = true)
    public Resultado<List<FilaExistencia>, ErrorInventario> ejecutar(String localId, String filtro) {
        Local local = locales.findById(localId).orElse(null);
        if (local == null) {
            return Resultado.fracaso(ErrorInventario.LOCAL_NO_ENCONTRADO);
        }
        if (!local.isActivo()) {
            return Resultado.fracaso(ErrorInventario.LOCAL_INACTIVO);
        }

        List<Producto> listaProductos = productos.findByActivoTrueOrderByNombreAsc();
        Map<String, Existencia> mapaExistencias = existencias.findByLocalId(localId).stream()
                .collect(Collectors.toMap(Existencia::getProductoId, e -> e));

        List<FilaExistencia> todas = listaProductos.stream()
                .map(prod -> {
                    Existencia e = mapaExistencias.get(prod.getId());
                    BigDecimal stockActual = e != null ? e.getStockActual() : BigDecimal.ZERO;
                    BigDecimal stockMinimo = e != null ? e.getStockMinimo() : null;
                    String estadoAlerta = e != null ? e.getEstadoAlerta() : "sin_stock";
                    var recalculadoEn = e != null ? e.getRecalculadoEn() : null;

                    return new FilaExistencia(
                            localId,
                            prod.getId(),
                            prod.getCodigo(),
                            prod.getNombre(),
                            prod.getCategoria().getId(),
                            prod.getCategoria().getNombre(),
                            prod.getUnidadBase().getId(),
                            prod.getUnidadBase().getCodigo(),
                            prod.getUnidadBase().getNombre(),
                            prod.isControlaStock(),
                            stockActual,
                            stockMinimo,
                            estadoAlerta,
                            recalculadoEn
                    );
                })
                .toList();

        List<FilaExistencia> resultado;
        if ("bajo_minimo".equalsIgnoreCase(filtro)) {
            resultado = todas.stream().filter(f -> "bajo_minimo".equals(f.estadoAlerta())).toList();
        } else if ("negativo".equalsIgnoreCase(filtro)) {
            resultado = todas.stream().filter(f -> "negativo".equals(f.estadoAlerta())).toList();
        } else {
            resultado = todas;
        }

        return Resultado.exito(resultado);
    }

    @Transactional(readOnly = true)
    public Resultado<ResumenAlertasInventario, ErrorInventario> resumen(String localId) {
        return ejecutar(localId, null).map(filas -> {
            long totalProductos = filas.size();
            long totalBajoMinimo = filas.stream().filter(f -> "bajo_minimo".equals(f.estadoAlerta())).count();
            long totalNegativos = filas.stream().filter(f -> "negativo".equals(f.estadoAlerta())).count();
            long totalSinStock = filas.stream().filter(f -> "sin_stock".equals(f.estadoAlerta())).count();

            return new ResumenAlertasInventario(
                    totalProductos,
                    totalBajoMinimo,
                    totalNegativos,
                    totalSinStock
            );
        });
    }
}

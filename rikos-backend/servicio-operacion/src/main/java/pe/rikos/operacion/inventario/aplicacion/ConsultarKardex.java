package pe.rikos.operacion.inventario.aplicacion;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.inventario.aplicacion.dto.FilaKardex;
import pe.rikos.operacion.inventario.aplicacion.dto.FiltroKardex;
import pe.rikos.operacion.inventario.aplicacion.dto.PaginaKardex;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Caso de uso: Consultar el Kardex con origen rastreable (US-044, ADM-17).
 * RN-057: Movimientos inmutables ordenados cronológicamente con balance y trazabilidad.
 * RN-012: Ocultamiento de costos si el usuario no tiene permiso inventario.ver_costos.
 */
@Service
public class ConsultarKardex {

    private final LocalRepositorio locales;
    private final MovimientoInventarioRepositorio movimientos;

    public ConsultarKardex(LocalRepositorio locales,
                           MovimientoInventarioRepositorio movimientos) {
        this.locales = locales;
        this.movimientos = movimientos;
    }

    @Transactional(readOnly = true)
    public Resultado<PaginaKardex, ErrorInventario> ejecutar(String localId,
                                                            FiltroKardex filtro,
                                                            boolean puedeVerCostos) {
        Local local = locales.findById(localId).orElse(null);
        if (local == null) {
            return Resultado.fracaso(ErrorInventario.LOCAL_NO_ENCONTRADO);
        }
        if (!local.isActivo()) {
            return Resultado.fracaso(ErrorInventario.LOCAL_INACTIVO);
        }

        Specification<MovimientoInventario> spec = (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("local").get("id"), localId));

            if (filtro != null) {
                if (filtro.productoId() != null && !filtro.productoId().isBlank()) {
                    predicados.add(cb.equal(root.get("producto").get("id"), filtro.productoId().trim()));
                }
                if (filtro.origen() != null && !filtro.origen().isBlank()) {
                    predicados.add(cb.equal(root.get("origen"), filtro.origen().trim().toLowerCase()));
                }
                if (filtro.desde() != null) {
                    predicados.add(cb.greaterThanOrEqualTo(root.get("ocurridoEn"),
                            filtro.desde().atStartOfDay().atOffset(ZoneOffset.UTC)));
                }
                if (filtro.hasta() != null) {
                    predicados.add(cb.lessThanOrEqualTo(root.get("ocurridoEn"),
                            filtro.hasta().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC)));
                }
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };

        boolean asc = filtro != null && filtro.esAscendente();
        Sort sort = asc
                ? Sort.by("ocurridoEn").ascending().and(Sort.by("id").ascending())
                : Sort.by("ocurridoEn").descending().and(Sort.by("id").descending());

        int pagina = filtro != null ? filtro.paginaSegura() : 0;
        int tamano = filtro != null ? filtro.tamanoSeguro() : 25;
        PageRequest pageRequest = PageRequest.of(pagina, tamano, sort);

        Page<MovimientoInventario> paginaMovs = movimientos.findAll(spec, pageRequest);

        List<FilaKardex> filas = paginaMovs.getContent().stream()
                .map(m -> mapearAFila(m, puedeVerCostos))
                .toList();

        return Resultado.exito(new PaginaKardex(
                filas,
                paginaMovs.getTotalElements(),
                paginaMovs.getTotalPages(),
                paginaMovs.getNumber(),
                paginaMovs.getSize()
        ));
    }

    private FilaKardex mapearAFila(MovimientoInventario m, boolean puedeVerCostos) {
        BigDecimal costoUnitario = null;
        BigDecimal costoTotal = null;

        if (puedeVerCostos && m.getCostoUnitario() != null) {
            costoUnitario = m.getCostoUnitario();
            if (m.getCantidadBase() != null) {
                costoTotal = costoUnitario.multiply(m.getCantidadBase()).setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal diferencia = "entrada".equalsIgnoreCase(m.getSigno())
                ? m.getCantidadBase()
                : m.getCantidadBase().negate();

        String usuarioNombre = m.getRegistradoPor() != null
                ? (m.getRegistradoPor().getNombres() + " " + m.getRegistradoPor().getApellidos()).trim()
                : "";

        return new FilaKardex(
                m.getId(),
                m.getLocal().getId(),
                m.getProducto().getId(),
                m.getProducto().getCodigo(),
                m.getProducto().getNombre(),
                m.getProducto().getCategoria() != null ? m.getProducto().getCategoria().getNombre() : null,
                m.getOrigen(),
                m.getSigno(),
                m.getCantidad(),
                m.getUnidad().getId(),
                m.getUnidad().getNombre(),
                m.getUnidad().getCodigo(),
                m.getCantidadBase(),
                costoUnitario,
                costoTotal,
                m.getStockAnterior(),
                m.getStockNuevo(),
                diferencia,
                m.getReferenciaTipo(),
                m.getReferenciaId(),
                m.getMotivo() != null ? m.getMotivo().getDescripcion() : null,
                m.getObservacion(),
                m.getOcurridoEn(),
                m.getRegistradoEn(),
                m.getRegistradoPor() != null ? m.getRegistradoPor().getId() : null,
                usuarioNombre
        );
    }
}

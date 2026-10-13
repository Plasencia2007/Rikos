package pe.rikos.operacion.inventario.aplicacion;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.inventario.aplicacion.dto.DetalleAjusteDTO;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Caso de uso: Consultar historial de ajustes de inventario de un local (US-045 CA3).
 */
@Service
public class ConsultarAjustesInventario {

    private final LocalRepositorio locales;
    private final MovimientoInventarioRepositorio movimientos;

    public ConsultarAjustesInventario(LocalRepositorio locales,
                                     MovimientoInventarioRepositorio movimientos) {
        this.locales = locales;
        this.movimientos = movimientos;
    }

    @Transactional(readOnly = true)
    public Resultado<List<DetalleAjusteDTO>, ErrorInventario> ejecutar(String localId,
                                                                      String productoId,
                                                                      LocalDate fechaInicio,
                                                                      LocalDate fechaFin,
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
            predicados.add(cb.equal(root.get("origen"), "ajuste"));

            if (productoId != null && !productoId.isBlank()) {
                predicados.add(cb.equal(root.get("producto").get("id"), productoId));
            }

            if (fechaInicio != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("ocurridoEn"),
                        fechaInicio.atStartOfDay().atOffset(ZoneOffset.UTC)));
            }

            if (fechaFin != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("ocurridoEn"),
                        fechaFin.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC)));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };

        List<MovimientoInventario> lista = movimientos.findAll(spec, Sort.by(Sort.Direction.DESC, "ocurridoEn"));

        List<DetalleAjusteDTO> resultado = lista.stream().map(m -> {
            BigDecimal costoUnit = puedeVerCostos ? m.getCostoUnitario() : null;
            BigDecimal costoTot = null;
            if (puedeVerCostos && m.getCostoUnitario() != null && m.getCantidad() != null) {
                costoTot = m.getCantidad().multiply(m.getCostoUnitario()).setScale(6, RoundingMode.HALF_UP);
            }

            String motivoId = m.getMotivo() != null ? m.getMotivo().getId() : null;
            String motivoDesc = m.getMotivo() != null ? m.getMotivo().getDescripcion() : null;
            String usuarioId = m.getRegistradoPor() != null ? m.getRegistradoPor().getId() : null;
            String usuarioNombre = null;
            if (m.getRegistradoPor() != null) {
                var u = m.getRegistradoPor();
                usuarioNombre = u.getNombres() != null ? (u.getNombres() + " " + u.getApellidos()).trim() : u.getNombreUsuario();
            }

            return new DetalleAjusteDTO(
                    m.getId(),
                    localId,
                    m.getProducto().getId(),
                    m.getProducto().getCodigo(),
                    m.getProducto().getNombre(),
                    m.getUnidad() != null ? m.getUnidad().getCodigo() : "und",
                    m.getSigno(),
                    m.getCantidad(),
                    m.getStockAnterior(),
                    m.getStockNuevo(),
                    costoUnit,
                    costoTot,
                    motivoId,
                    motivoDesc,
                    m.getObservacion(),
                    m.getOcurridoEn(),
                    usuarioId,
                    usuarioNombre
            );
        }).toList();

        return Resultado.exito(resultado);
    }
}

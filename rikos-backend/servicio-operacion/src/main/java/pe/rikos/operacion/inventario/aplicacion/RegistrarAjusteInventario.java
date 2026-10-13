package pe.rikos.operacion.inventario.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.inventario.aplicacion.dto.DetalleAjusteDTO;
import pe.rikos.operacion.inventario.aplicacion.dto.RegistroAjusteComando;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Caso de uso: Registrar ajuste de inventario con motivo (US-045, RN-052).
 * Permite ajustes positivos y negativos indicando motivo obligatorio de lista configurable,
 * guarda stock anterior, nuevo, diferencia, costo unitario, usuario, fecha y local,
 * y emite evento de auditoria transversal (RN-004). Tolera saldos negativos per RN-057.
 */
@Service
public class RegistrarAjusteInventario {

    private final LocalRepositorio locales;
    private final ProductoRepositorio productos;
    private final MotivoRepositorio motivos;
    private final UsuarioRepositorio usuarios;
    private final ExistenciaRepositorio existencias;
    private final MovimientoInventarioRepositorio movimientos;
    private final RegistradorAuditoria auditoria;

    public RegistrarAjusteInventario(LocalRepositorio locales,
                                     ProductoRepositorio productos,
                                     MotivoRepositorio motivos,
                                     UsuarioRepositorio usuarios,
                                     ExistenciaRepositorio existencias,
                                     MovimientoInventarioRepositorio movimientos,
                                     RegistradorAuditoria auditoria) {
        this.locales = locales;
        this.productos = productos;
        this.motivos = motivos;
        this.usuarios = usuarios;
        this.existencias = existencias;
        this.movimientos = movimientos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<DetalleAjusteDTO, ErrorInventario> ejecutar(RegistroAjusteComando comando) {
        if (comando == null || comando.localId() == null || comando.productoId() == null) {
            return Resultado.fracaso(ErrorInventario.PRODUCTO_NO_ENCONTRADO);
        }

        Local local = locales.findById(comando.localId()).orElse(null);
        if (local == null) {
            return Resultado.fracaso(ErrorInventario.LOCAL_NO_ENCONTRADO);
        }
        if (!local.isActivo()) {
            return Resultado.fracaso(ErrorInventario.LOCAL_INACTIVO);
        }

        Usuario usuario = usuarios.findById(comando.usuarioId()).orElse(null);
        if (usuario == null) {
            return Resultado.fracaso(ErrorInventario.USUARIO_NO_ENCONTRADO);
        }

        Producto producto = productos.findById(comando.productoId()).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorInventario.PRODUCTO_NO_ENCONTRADO);
        }
        if (!producto.isActivo()) {
            return Resultado.fracaso(ErrorInventario.PRODUCTO_INACTIVO);
        }

        // Validación de motivo (CA2)
        if (comando.motivoId() == null || comando.motivoId().isBlank()) {
            return Resultado.fracaso(ErrorInventario.MOTIVO_NO_ENCONTRADO);
        }
        Motivo motivo = motivos.findById(comando.motivoId()).orElse(null);
        if (motivo == null) {
            return Resultado.fracaso(ErrorInventario.MOTIVO_NO_ENCONTRADO);
        }
        if (!motivo.isActivo()) {
            return Resultado.fracaso(ErrorInventario.MOTIVO_INACTIVO);
        }
        if (!"ajuste_inventario".equals(motivo.getTipo())) {
            return Resultado.fracaso(ErrorInventario.MOTIVO_TIPO_INVALIDO);
        }

        // Validación de observación obligatoria según el motivo
        String obs = comando.observacion() != null ? comando.observacion().trim() : "";
        if (motivo.isRequiereObservacion() && obs.isEmpty()) {
            return Resultado.fracaso(ErrorInventario.OBSERVACION_REQUERIDA);
        }

        // Existencia y cálculo de stock
        Existencia existencia = existencias.findByLocalIdAndProductoId(local.getId(), producto.getId())
                .orElseGet(() -> new Existencia(local.getId(), producto.getId(), null));

        BigDecimal stockAnterior = existencia.getStockActual() != null ? existencia.getStockActual() : BigDecimal.ZERO;
        BigDecimal stockNuevo;
        BigDecimal cantidad;
        String signo;

        String tipoAjuste = comando.tipoAjuste() != null ? comando.tipoAjuste().toUpperCase().trim() : "INCREMENTO";

        if ("REEMPLAZO_STOCK".equals(tipoAjuste) || "STOCK_FINAL".equals(tipoAjuste)) {
            if (comando.stockFinal() == null) {
                return Resultado.fracaso(ErrorInventario.CANTIDAD_INVALIDA);
            }
            stockNuevo = comando.stockFinal().setScale(6, RoundingMode.HALF_UP);
            BigDecimal diferencia = stockNuevo.subtract(stockAnterior);
            int comparacion = diferencia.compareTo(BigDecimal.ZERO);
            if (comparacion == 0) {
                return Resultado.fracaso(ErrorInventario.SIN_VARIACION);
            }
            if (comparacion > 0) {
                signo = "entrada";
                cantidad = diferencia;
            } else {
                signo = "salida";
                cantidad = diferencia.abs();
            }
        } else if ("DISMINUCION".equals(tipoAjuste)) {
            if (comando.cantidad() == null || comando.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
                return Resultado.fracaso(ErrorInventario.CANTIDAD_INVALIDA);
            }
            cantidad = comando.cantidad().setScale(6, RoundingMode.HALF_UP);
            signo = "salida";
            stockNuevo = stockAnterior.subtract(cantidad);
        } else if ("VARIACION".equals(tipoAjuste)) {
            if (comando.cantidad() == null || comando.cantidad().compareTo(BigDecimal.ZERO) == 0) {
                return Resultado.fracaso(ErrorInventario.CANTIDAD_INVALIDA);
            }
            BigDecimal delta = comando.cantidad().setScale(6, RoundingMode.HALF_UP);
            if (delta.compareTo(BigDecimal.ZERO) > 0) {
                signo = "entrada";
                cantidad = delta;
                stockNuevo = stockAnterior.add(delta);
            } else {
                signo = "salida";
                cantidad = delta.abs();
                stockNuevo = stockAnterior.subtract(cantidad);
            }
        } else {
            // INCREMENTO por defecto
            if (comando.cantidad() == null || comando.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
                return Resultado.fracaso(ErrorInventario.CANTIDAD_INVALIDA);
            }
            cantidad = comando.cantidad().setScale(6, RoundingMode.HALF_UP);
            signo = "entrada";
            stockNuevo = stockAnterior.add(cantidad);
        }

        BigDecimal costoUnitario = producto.getCostoUltimaCompra() != null ? producto.getCostoUltimaCompra() : BigDecimal.ZERO;
        BigDecimal costoTotal = cantidad.multiply(costoUnitario).setScale(6, RoundingMode.HALF_UP);

        String movimientoId = Identificador.nuevo().valor();
        OffsetDateTime ocurridoEn = comando.fecha() != null ? comando.fecha() : OffsetDateTime.now(ZoneOffset.UTC);

        // Registro de MovimientoInventario (CA3 / RN-052)
        MovimientoInventario movimiento = new MovimientoInventario(
                movimientoId,
                local,
                producto,
                "ajuste",
                signo,
                cantidad,
                producto.getUnidadBase(),
                cantidad,
                costoUnitario,
                stockAnterior,
                stockNuevo,
                "ajuste",
                movimientoId,
                motivo,
                obs.isEmpty() ? null : obs,
                ocurridoEn,
                usuario
        );

        MovimientoInventario movGuardado = movimientos.saveAndFlush(movimiento);

        // Actualización de Existencia (CA1)
        existencia.actualizarStock(stockNuevo, movGuardado);
        existencias.saveAndFlush(existencia);

        // Registro de auditoría transversal (RN-004)
        if (auditoria != null) {
            auditoria.registrar(
                    "ajuste_inventario",
                    "existencia",
                    producto.getId(),
                    Map.of("stockActual", stockAnterior),
                    Map.of("stockActual", stockNuevo, "signo", signo, "cantidad", cantidad),
                    motivo.getId(),
                    obs.isEmpty() ? null : obs
            );
        }

        String nombreUsuario = usuario.getNombres() != null
                ? (usuario.getNombres() + " " + usuario.getApellidos()).trim()
                : usuario.getNombreUsuario();

        DetalleAjusteDTO detalle = new DetalleAjusteDTO(
                movGuardado.getId(),
                local.getId(),
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getUnidadBase() != null ? producto.getUnidadBase().getCodigo() : "und",
                signo,
                cantidad,
                stockAnterior,
                stockNuevo,
                costoUnitario,
                costoTotal,
                motivo.getId(),
                motivo.getDescripcion(),
                obs.isEmpty() ? null : obs,
                ocurridoEn,
                usuario.getId(),
                nombreUsuario
        );

        return Resultado.exito(detalle);
    }
}

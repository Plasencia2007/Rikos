package pe.rikos.operacion.inventario.aplicacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.aplicacion.ObtenerPermisosUsuario;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRol;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;
import pe.rikos.operacion.inventario.aplicacion.dto.FilaConsumoInternoDTO;
import pe.rikos.operacion.inventario.aplicacion.dto.RegistroConsumoInternoComando;
import pe.rikos.operacion.inventario.infraestructura.ConsumoInterno;
import pe.rikos.operacion.inventario.infraestructura.ConsumoInternoRepositorio;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RegistrarConsumoInterno {

    private final ConsumoInternoRepositorio consumos;
    private final LocalRepositorio locales;
    private final UsuarioRepositorio usuarios;
    private final ProductoRepositorio productos;
    private final ExistenciaRepositorio existencias;
    private final MovimientoInventarioRepositorio movimientos;
    private final UsuarioRolRepositorio usuarioRoles;
    private final ObtenerPermisosUsuario obtenerPermisosUsuario;
    private final PasswordEncoder passwordEncoder;
    private final RegistradorAuditoria auditoria;

    private static final Set<String> SERVICIOS_VALIDOS = Set.of("desayuno", "almuerzo", "cena");

    @Autowired
    public RegistrarConsumoInterno(ConsumoInternoRepositorio consumos,
                                   LocalRepositorio locales,
                                   UsuarioRepositorio usuarios,
                                   ProductoRepositorio productos,
                                   ExistenciaRepositorio existencias,
                                   MovimientoInventarioRepositorio movimientos,
                                   UsuarioRolRepositorio usuarioRoles,
                                   ObtenerPermisosUsuario obtenerPermisosUsuario,
                                   PasswordEncoder passwordEncoder,
                                   @Autowired(required = false) RegistradorAuditoria auditoria) {
        this.consumos = consumos;
        this.locales = locales;
        this.usuarios = usuarios;
        this.productos = productos;
        this.existencias = existencias;
        this.movimientos = movimientos;
        this.usuarioRoles = usuarioRoles;
        this.obtenerPermisosUsuario = obtenerPermisosUsuario;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<FilaConsumoInternoDTO, ErrorInventario> ejecutar(RegistroConsumoInternoComando comando) {
        if (comando == null || comando.localId() == null || comando.localId().isBlank()) {
            return Resultado.fracaso(ErrorInventario.LOCAL_NO_ENCONTRADO);
        }

        Local local = locales.findById(comando.localId()).orElse(null);
        if (local == null) {
            return Resultado.fracaso(ErrorInventario.LOCAL_NO_ENCONTRADO);
        }
        if (!local.isActivo()) {
            return Resultado.fracaso(ErrorInventario.LOCAL_INACTIVO);
        }

        if (comando.usuarioId() == null || comando.usuarioId().isBlank()) {
            return Resultado.fracaso(ErrorInventario.USUARIO_NO_ENCONTRADO);
        }
        Usuario trabajador = usuarios.findById(comando.usuarioId()).orElse(null);
        if (trabajador == null || !trabajador.isActivo()) {
            return Resultado.fracaso(ErrorInventario.USUARIO_NO_ENCONTRADO);
        }

        if (comando.productoId() == null || comando.productoId().isBlank()) {
            return Resultado.fracaso(ErrorInventario.PRODUCTO_NO_ENCONTRADO);
        }
        Producto producto = productos.findById(comando.productoId()).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorInventario.PRODUCTO_NO_ENCONTRADO);
        }
        if (!producto.isActivo()) {
            return Resultado.fracaso(ErrorInventario.PRODUCTO_INACTIVO);
        }

        // Regla: no registrar insumos crudos que no sean comida/plato
        if (producto.isEsInsumo() && !producto.isEsSubreceta()) {
            return Resultado.fracaso(ErrorInventario.PRODUCTO_NO_ES_COMIDA);
        }

        LocalDate fecha = comando.fecha() != null ? comando.fecha() : LocalDate.now();

        if (comando.servicio() == null || comando.servicio().isBlank()) {
            return Resultado.fracaso(ErrorInventario.SERVICIO_REQUERIDO);
        }
        String servicio = comando.servicio().trim().toLowerCase();
        if (!SERVICIOS_VALIDOS.contains(servicio)) {
            return Resultado.fracaso(ErrorInventario.SERVICIO_REQUERIDO);
        }

        BigDecimal cantidad = comando.cantidad() != null && comando.cantidad().compareTo(BigDecimal.ZERO) > 0
                ? comando.cantidad().setScale(6, RoundingMode.HALF_UP)
                : BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);

        // Control de 1ra comida vs 2da comida (RN-056)
        long consumosPrevios = consumos.countByLocalIdAndUsuarioIdAndFechaAndServicio(local.getId(), trabajador.getId(), fecha, servicio);
        boolean requirioAutorizacion = false;
        Usuario autorizadoPor = null;

        if (consumosPrevios >= 1) {
            requirioAutorizacion = true;
            Usuario operador = comando.operadorId() != null ? usuarios.findById(comando.operadorId()).orElse(null) : null;

            if (operador != null && tienePermisoAutorizar(operador.getId())) {
                autorizadoPor = operador;
            } else if (comando.supervisorPin() != null && !comando.supervisorPin().isBlank()) {
                autorizadoPor = validarPinSupervisor(comando.supervisorPin());
                if (autorizadoPor == null) {
                    return Resultado.fracaso(ErrorInventario.PIN_SUPERVISOR_INVALIDO);
                }
            } else {
                return Resultado.fracaso(ErrorInventario.SEGUNDA_COMIDA_REQUIERE_AUTORIZACION);
            }
        }

        // Costeo
        BigDecimal costoUnitario = producto.getCostoUltimaCompra() != null ? producto.getCostoUltimaCompra() : BigDecimal.ZERO;
        BigDecimal costoTotal = costoUnitario.multiply(cantidad).setScale(4, RoundingMode.HALF_UP);

        // Descuento de inventario (Kardex y Existencia)
        Existencia existencia = existencias.findByLocalIdAndProductoId(local.getId(), producto.getId())
                .orElseGet(() -> new Existencia(local.getId(), producto.getId(), null));

        BigDecimal stockAnterior = existencia.getStockActual() != null ? existencia.getStockActual() : BigDecimal.ZERO;
        BigDecimal stockNuevo = stockAnterior.subtract(cantidad);

        String consumoId = Identificador.nuevo().valor();
        String movId = Identificador.nuevo().valor();
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);

        String obs = comando.observacion() != null && !comando.observacion().isBlank()
                ? comando.observacion().trim()
                : "Comida del personal (" + servicio + ") - " + trabajador.getNombreCompleto();

        Usuario usuarioRegistro = comando.operadorId() != null
                ? usuarios.findById(comando.operadorId()).orElse(trabajador)
                : trabajador;

        MovimientoInventario movSalida = new MovimientoInventario(
                movId,
                local,
                producto,
                "consumo_interno",
                "salida",
                cantidad,
                producto.getUnidadBase(),
                cantidad,
                costoUnitario,
                stockAnterior,
                stockNuevo,
                "consumo_interno",
                consumoId,
                null,
                obs,
                ahora,
                usuarioRegistro
        );

        MovimientoInventario movGuardado = movimientos.saveAndFlush(movSalida);
        existencia.actualizarStock(stockNuevo, movGuardado);
        existencias.saveAndFlush(existencia);

        ConsumoInterno consumo = new ConsumoInterno(
                consumoId,
                local,
                trabajador,
                fecha,
                servicio,
                comando.turno(),
                producto,
                cantidad,
                costoTotal,
                requirioAutorizacion,
                autorizadoPor,
                comando.observacion(),
                movGuardado
        );

        ConsumoInterno guardado = consumos.saveAndFlush(consumo);

        if (auditoria != null) {
            auditoria.registrar(
                    "consumo_interno",
                    "consumo_interno",
                    guardado.getId(),
                    Map.of("trabajador", trabajador.getId(), "producto", producto.getId()),
                    Map.of("costo", costoTotal, "servicio", servicio, "requirioAutorizacion", requirioAutorizacion),
                    null,
                    comando.observacion()
            );
        }

        return Resultado.exito(mapearDTO(guardado));
    }

    private boolean tienePermisoAutorizar(String usuarioId) {
        if (usuarioId == null) return false;
        List<String> rolesIds = usuarioRoles.findByUsuarioId(usuarioId).stream()
                .map(UsuarioRol::getRol).map(r -> r.getNombre().toLowerCase()).toList();

        if (rolesIds.stream().anyMatch(r -> r.contains("administrador"))) {
            return true;
        }

        return obtenerPermisosUsuario.ejecutar(usuarioId).stream()
                .anyMatch(p -> "consumo_interno.autorizar".equals(p.codigo()) && p.efectivo());
    }

    private Usuario validarPinSupervisor(String pin) {
        if (pin == null || pin.isBlank()) return null;
        List<Usuario> activos = usuarios.findAll().stream().filter(Usuario::isActivo).toList();
        for (Usuario u : activos) {
            if (u.getHashPin() != null && passwordEncoder.matches(pin, u.getHashPin())) {
                if (tienePermisoAutorizar(u.getId())) {
                    return u;
                }
            }
        }
        return null;
    }

    public static FilaConsumoInternoDTO mapearDTO(ConsumoInterno c) {
        BigDecimal costoUnit = c.getCantidad() != null && c.getCantidad().compareTo(BigDecimal.ZERO) > 0
                ? c.getCosto().divide(c.getCantidad(), 4, RoundingMode.HALF_UP)
                : c.getCosto();

        return new FilaConsumoInternoDTO(
                c.getId(),
                c.getLocal().getId(),
                c.getUsuario().getId(),
                c.getUsuario().getNombreCompleto(),
                c.getFecha(),
                c.getServicio(),
                c.getTurno(),
                c.getProducto().getId(),
                c.getProducto().getCodigo(),
                c.getProducto().getNombre(),
                c.getProducto().getUnidadBase() != null ? c.getProducto().getUnidadBase().getCodigo() : "und",
                c.getCantidad(),
                costoUnit,
                c.getCosto(),
                c.isRequirioAutorizacion(),
                c.getAutorizadoPor() != null ? c.getAutorizadoPor().getId() : null,
                c.getAutorizadoPor() != null ? c.getAutorizadoPor().getNombreCompleto() : null,
                c.getObservacion(),
                c.getMovimiento() != null ? c.getMovimiento().getId() : null,
                c.getCreadoEn()
        );
    }
}

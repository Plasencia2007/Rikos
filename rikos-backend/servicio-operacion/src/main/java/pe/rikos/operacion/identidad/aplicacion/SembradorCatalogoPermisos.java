package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.identidad.infraestructura.Permiso;
import pe.rikos.operacion.identidad.infraestructura.PermisoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.RolPermiso;
import pe.rikos.operacion.identidad.infraestructura.RolPermisoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.RolRepositorio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catalogo de permisos y su asignacion a los seis roles del negocio.
 * Idempotente: se ejecuta en cada arranque, pero solo inserta lo que
 * todavia no existe (por codigo de permiso / por rol+permiso). No es una
 * migracion de Flyway porque el catalogo se razona mejor como datos
 * estructurados en codigo que como 100+ INSERT escritos a mano, y porque
 * los ids se generan en el origen (2.1), no se pueden hardcodear aqui sin
 * inventar ULIDs a mano.
 *
 * Seccion 2 de la ampliacion de US-004/US-006: "no todas las acciones
 * aplican a todos los modulos". Las combinaciones que siguen son una
 * decision, no una enumeracion mecanica -- ver el resumen en el reporte
 * de la tarea para lo que se dejo fuera y por que.
 */
@Component
public class SembradorCatalogoPermisos implements ApplicationRunner {

    private record DefPermiso(String modulo, String accion, String descripcion) {
        String codigo() {
            return modulo + "." + accion;
        }
    }

    private static final List<DefPermiso> CATALOGO = List.of(
        // ver -- los doce modulos
        new DefPermiso("identidad", "ver", "Ver usuarios, roles y locales"),
        new DefPermiso("catalogo", "ver", "Ver productos y categorías del catálogo"),
        new DefPermiso("recetas", "ver", "Ver recetas"),
        new DefPermiso("menu", "ver", "Ver el menú programado"),
        new DefPermiso("pensionistas", "ver", "Ver pensionistas y su cuenta"),
        new DefPermiso("venta", "ver", "Ver pedidos y cuentas"),
        new DefPermiso("caja", "ver", "Ver movimientos y turnos de caja"),
        new DefPermiso("inventario", "ver", "Ver existencias e inventario"),
        new DefPermiso("compras", "ver", "Ver compras y proveedores"),
        new DefPermiso("delivery", "ver", "Ver pedidos de delivery"),
        new DefPermiso("facturacion", "ver", "Ver comprobantes electrónicos"),
        new DefPermiso("reportes", "ver", "Ver reportes"),
        // US-006: pantalla ADM-31 (bitacora de auditoria). "auditoria.ver" en
        // vez de reusar "identidad.ver" -- la bitacora cruza todos los
        // modulos (no solo identidad), asi que necesita su propio permiso.
        new DefPermiso("auditoria", "ver", "Ver la bitácora de auditoría"),
        // crear
        new DefPermiso("identidad", "crear", "Crear usuarios, roles y locales"),
        new DefPermiso("catalogo", "crear", "Crear productos y categorías"),
        new DefPermiso("recetas", "crear", "Crear recetas"),
        new DefPermiso("menu", "crear", "Programar el menú semanal"),
        new DefPermiso("pensionistas", "crear", "Dar de alta pensionistas"),
        new DefPermiso("venta", "crear", "Registrar pedidos"),
        new DefPermiso("caja", "crear", "Abrir turno de caja"),
        new DefPermiso("caja", "administrar", "Crear y editar las cajas de un local"),
        new DefPermiso("caja", "descontar", "Aplicar descuentos al cobrar"),
        new DefPermiso("compras", "crear", "Registrar compras"),
        new DefPermiso("delivery", "crear", "Registrar pedidos de delivery"),
        new DefPermiso("facturacion", "crear", "Emitir comprobantes electrónicos"),
        // modificar
        new DefPermiso("identidad", "modificar", "Editar usuarios, roles y locales"),
        new DefPermiso("catalogo", "modificar", "Editar productos y categorías"),
        new DefPermiso("recetas", "modificar", "Editar recetas"),
        new DefPermiso("menu", "modificar", "Editar el menú programado"),
        new DefPermiso("pensionistas", "modificar", "Editar datos de pensionistas"),
        new DefPermiso("venta", "modificar", "Editar un pedido en curso"),
        new DefPermiso("compras", "modificar", "Editar una compra registrada"),
        // desactivar
        new DefPermiso("identidad", "desactivar", "Desactivar usuarios y locales"),
        new DefPermiso("catalogo", "desactivar", "Desactivar productos"),
        new DefPermiso("recetas", "desactivar", "Desactivar recetas"),
        new DefPermiso("menu", "desactivar", "Quitar un ítem del menú programado"),
        new DefPermiso("pensionistas", "desactivar", "Desactivar pensionistas"),
        new DefPermiso("compras", "desactivar", "Desactivar proveedores"),
        // aprobar
        new DefPermiso("pensionistas", "aprobar", "Aprobar o rechazar abonos"),
        new DefPermiso("compras", "aprobar", "Aprobar órdenes de compra"),
        // anular
        new DefPermiso("venta", "anular", "Anular un pedido o cuenta"),
        new DefPermiso("caja", "anular", "Anular un movimiento de caja"),
        // descontar
        new DefPermiso("inventario", "descontar", "Descontar o ajustar existencias"),
        // reabrir
        new DefPermiso("venta", "reabrir", "Reabrir una cuenta cerrada"),
        new DefPermiso("venta", "bloquear_mesa", "Bloquear y liberar mesas"),
        new DefPermiso("venta", "administrar", "Crear y editar las mesas de un local"),
        new DefPermiso("caja", "reabrir", "Reabrir una caja cerrada"),
        // ver_costos
        new DefPermiso("recetas", "ver_costos", "Ver el costo de las recetas"),
        new DefPermiso("inventario", "ver_costos", "Ver el costo de los insumos"),
        new DefPermiso("reportes", "ver_costos", "Ver reportes de margen y costos"),
        // exportar
        new DefPermiso("reportes", "exportar", "Exportar reportes"),
        new DefPermiso("facturacion", "exportar", "Exportar comprobantes y libro de ventas"),
        new DefPermiso("inventario", "exportar", "Exportar el kardex de inventario"),
        // marcar_agotado -- US-015: disponibilidad por local. Ninguno de los
        // anteriores encaja (menu.desactivar es quitar del menu programado,
        // y es central).
        new DefPermiso("menu", "marcar_agotado", "Marcar y reactivar productos agotados en su local"),
        // administrar -- sprint 5: planes tarifarios y parametros de cobro
        // (taper, segundo consumo). Solo el administrador central: el de local
        // ya tiene pensionistas.modificar para las fichas, y no debe tocar precios.
        new DefPermiso("pensionistas", "administrar", "Administrar planes tarifarios y parámetros de cobro"),
        // administrar_permisos
        new DefPermiso("identidad", "administrar_permisos", "Editar roles, permisos y excepciones de usuario")
    );

    private static final Map<String, String> COLOR_POR_ROL = Map.of(
        "administrador central", "#2563eb",
        "administrador de local", "#7c3aed",
        "cajero", "#059669",
        "mozo", "#d97706",
        "cocina", "#dc2626",
        "motorizado", "#0891b2"
    );

    /** administrador central: todo. Los demas, solo lo que necesitan para lo que hacen (seccion 3 de la tarea). */
    private static final Map<String, Set<String>> PERMISOS_POR_ROL = new LinkedHashMap<>();
    static {
        PERMISOS_POR_ROL.put("administrador central",
            CATALOGO.stream().map(DefPermiso::codigo).collect(java.util.stream.Collectors.toSet()));

        PERMISOS_POR_ROL.put("administrador de local", Set.of(
            "identidad.ver", "catalogo.ver", "recetas.ver", "menu.ver", "pensionistas.ver", "venta.ver",
            "caja.ver", "inventario.ver", "compras.ver", "delivery.ver", "facturacion.ver", "reportes.ver",
            "auditoria.ver",
            "identidad.crear", "pensionistas.crear", "venta.crear", "caja.crear", "compras.crear",
            "delivery.crear", "facturacion.crear",
            "identidad.modificar", "pensionistas.modificar", "venta.modificar", "compras.modificar",
            "identidad.desactivar", "pensionistas.desactivar", "compras.desactivar",
            "pensionistas.aprobar", "compras.aprobar",
            "venta.anular", "caja.anular",
            "inventario.descontar",
            "venta.reabrir", "caja.reabrir",
            "recetas.ver_costos", "inventario.ver_costos", "reportes.ver_costos",
            "reportes.exportar", "facturacion.exportar", "inventario.exportar",
            "menu.marcar_agotado",
            "venta.bloquear_mesa", "venta.administrar", "caja.administrar", "caja.descontar"
        ));

        PERMISOS_POR_ROL.put("cajero", Set.of(
            "catalogo.ver", "menu.ver", "venta.ver", "caja.ver", "pensionistas.ver", "facturacion.ver",
            "venta.crear", "caja.crear", "facturacion.crear",
            "caja.descontar",
            "venta.modificar",
            "venta.anular", "caja.anular",
            "venta.reabrir", "caja.reabrir",
            "venta.bloquear_mesa"
        ));

        PERMISOS_POR_ROL.put("mozo", Set.of(
            "catalogo.ver", "menu.ver", "venta.ver", "pensionistas.ver",
            "venta.crear", "venta.modificar", "venta.bloquear_mesa"
        ));

        PERMISOS_POR_ROL.put("cocina", Set.of(
            "catalogo.ver", "recetas.ver", "menu.ver", "venta.ver",
            "catalogo.modificar", "menu.marcar_agotado"
        ));

        PERMISOS_POR_ROL.put("motorizado", Set.of(
            "delivery.ver", "venta.ver",
            "delivery.modificar"
        ));
    }

    /** Codigos de permiso con los que el sistema entrega un rol de fabrica; vacio si no es uno de los seis. */
    public static Set<String> codigosPredeterminados(String nombreRol) {
        return PERMISOS_POR_ROL.getOrDefault(nombreRol, Set.of());
    }

    private final PermisoRepositorio permisos;
    private final RolRepositorio roles;
    private final RolPermisoRepositorio rolPermisos;

    public SembradorCatalogoPermisos(PermisoRepositorio permisos, RolRepositorio roles,
                                      RolPermisoRepositorio rolPermisos) {
        this.permisos = permisos;
        this.roles = roles;
        this.rolPermisos = rolPermisos;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, Permiso> permisoPorCodigo = new LinkedHashMap<>();
        Set<String> codigosNuevos = new java.util.HashSet<>();
        for (DefPermiso def : CATALOGO) {
            Permiso permiso = permisos.findByCodigo(def.codigo()).orElseGet(() -> {
                codigosNuevos.add(def.codigo());
                return permisos.save(new Permiso(Identificador.nuevo().valor(), def.modulo(), def.accion(),
                        def.codigo(), def.descripcion()));
            });
            permisoPorCodigo.put(def.codigo(), permiso);
        }

        for (var entrada : PERMISOS_POR_ROL.entrySet()) {
            String nombreRol = entrada.getKey();
            Rol rol = roles.findByNombre(nombreRol).orElse(null);
            if (rol == null) continue;

            if (rol.getColor() == null && COLOR_POR_ROL.containsKey(nombreRol)) {
                rol.actualizar(rol.getNombre(), rol.getDescripcion(), COLOR_POR_ROL.get(nombreRol));
            }

            Set<String> yaAsignados = rolPermisos.findByRolId(rol.getId()).stream()
                    .map(rp -> rp.getPermiso().getCodigo()).collect(java.util.stream.Collectors.toSet());

            // Los roles del sistema se pueden editar: en un rol que ya tiene permisos solo se
            // agregan los permisos que acaban de nacer en el catalogo. Volver a poner uno que el
            // administrador quito a proposito desharia su cambio en cada arranque.
            boolean rolNuevo = yaAsignados.isEmpty();
            for (String codigo : entrada.getValue()) {
                if (yaAsignados.contains(codigo)) continue;
                if (!rolNuevo && !codigosNuevos.contains(codigo)) continue;
                Permiso permiso = permisoPorCodigo.get(codigo);
                if (permiso == null) continue;
                // creado_por va nulo: "sistema" no es un id de usuario real y
                // violaria la FK contra usuario(id) (creado_por es opcional).
                rolPermisos.save(new RolPermiso(rol, permiso, null));
            }
        }
    }
}

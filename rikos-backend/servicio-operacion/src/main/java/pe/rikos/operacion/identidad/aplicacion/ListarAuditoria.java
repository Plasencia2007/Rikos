package pe.rikos.operacion.identidad.aplicacion;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pe.rikos.operacion.comun.auditoria.Auditoria;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ADM-31: lectura de la bitacora, con los filtros que pide CA3 (local,
 * usuario, rango de fechas, tipo de evento) y la traduccion a una frase
 * legible. Vive en identidad (no en comun.auditoria) porque necesita
 * resolver nombres de usuario y de local, que identidad ya es dueño --
 * comun.auditoria solo conoce el evento crudo, no a quien pertenece cada id.
 *
 * El filtro se arma a mano con CriteriaBuilder, no con
 * JpaSpecificationExecutor (ver el comentario en AuditoriaRepositorio: esa
 * interfaz trae su propio metodo de borrado, incompatible con CA4).
 */
@Service
public class ListarAuditoria {

    @PersistenceContext
    private EntityManager entityManager;

    private final UsuarioRepositorio usuarios;
    private final LocalRepositorio locales;

    public ListarAuditoria(UsuarioRepositorio usuarios, LocalRepositorio locales) {
        this.usuarios = usuarios;
        this.locales = locales;
    }

    public record Filtro(String localId, String usuarioId, String evento, OffsetDateTime desde,
                          OffsetDateTime hasta) {
    }

    public Page<AuditoriaRespuesta> ejecutar(Filtro filtro, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Auditoria> consulta = cb.createQuery(Auditoria.class);
        Root<Auditoria> raiz = consulta.from(Auditoria.class);
        consulta.select(raiz).where(predicados(filtro, cb, raiz));
        consulta.orderBy(cb.desc(raiz.get("ocurridoEn")));

        List<Auditoria> filas = entityManager.createQuery(consulta)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> consultaConteo = cb.createQuery(Long.class);
        Root<Auditoria> raizConteo = consultaConteo.from(Auditoria.class);
        consultaConteo.select(cb.count(raizConteo)).where(predicados(filtro, cb, raizConteo));
        long total = entityManager.createQuery(consultaConteo).getSingleResult();

        return new PageImpl<>(aRespuestas(filas), pageable, total);
    }

    private Predicate[] predicados(Filtro filtro, CriteriaBuilder cb, Root<Auditoria> raiz) {
        List<Predicate> predicados = new ArrayList<>();
        if (filtro.localId() != null) predicados.add(cb.equal(raiz.get("localId"), filtro.localId()));
        if (filtro.usuarioId() != null) predicados.add(cb.equal(raiz.get("usuarioId"), filtro.usuarioId()));
        if (filtro.evento() != null) predicados.add(cb.equal(raiz.get("evento"), filtro.evento()));
        if (filtro.desde() != null) predicados.add(cb.greaterThanOrEqualTo(raiz.get("ocurridoEn"), filtro.desde()));
        if (filtro.hasta() != null) predicados.add(cb.lessThanOrEqualTo(raiz.get("ocurridoEn"), filtro.hasta()));
        return predicados.toArray(new Predicate[0]);
    }

    // Los nombres se resuelven en lote (no fila por fila) para no hacer
    // N+1 -- una pagina de bitacora tiene como mucho unas pocas decenas de
    // usuarios/locales distintos.
    private List<AuditoriaRespuesta> aRespuestas(List<Auditoria> filas) {
        Map<String, String> nombrePorUsuario = new HashMap<>();
        Map<String, String> nombrePorLocal = new HashMap<>();
        List<AuditoriaRespuesta> respuestas = new ArrayList<>(filas.size());

        for (Auditoria fila : filas) {
            String usuarioNombre = nombrePorUsuario.computeIfAbsent(fila.getUsuarioId(),
                    id -> usuarios.findById(id).map(Usuario::getNombreUsuario).orElse(null));
            String localNombre = fila.getLocalId() == null ? null
                    : nombrePorLocal.computeIfAbsent(fila.getLocalId(),
                            id -> locales.findById(id).map(Local::getNombre).orElse(null));

            respuestas.add(new AuditoriaRespuesta(fila.getId(), fila.getEvento(), fila.getEntidad(),
                    fila.getEntidadId(), fila.getUsuarioId(), usuarioNombre, fila.getLocalId(), localNombre,
                    humanizar(fila, usuarioNombre), fila.getValorAnterior(), fila.getValorNuevo(),
                    fila.getObservacion(), fila.getOcurridoEn()));
        }
        return respuestas;
    }

    private String humanizar(Auditoria fila, String usuarioNombre) {
        String actor = usuarioNombre != null ? usuarioNombre : "Alguien";
        String nombreEntidad = RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "nombre");
        if (nombreEntidad == null) {
            nombreEntidad = RegistradorAuditoria.extraerCampo(fila.getValorAnterior(), "nombre");
        }
        String etiqueta = etiquetaEntidad(fila.getEntidad());
        String referencia = nombreEntidad != null ? "\"" + nombreEntidad + "\"" : etiqueta;

        // US-013: confirmar la semana de menu es un cambio de estado sin "activo".
        if ("cambio_estado".equals(fila.getEvento()) && "semana_menu".equals(fila.getEntidad())) {
            return actor + " confirmó la semana de menú del " + (nombreEntidad != null ? nombreEntidad : fila.getEntidadId());
        }
        // Sprint 5: el mismo evento cambio_plan sirve para un plan tarifario y para el plan de un pensionista.
        if ("cambio_plan".equals(fila.getEvento()) && "pensionista".equals(fila.getEntidad())) {
            return actor + " cambió el plan de " + referencia + " de "
                    + RegistradorAuditoria.extraerCampo(fila.getValorAnterior(), "plan") + " a "
                    + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "plan");
        }
        return switch (fila.getEvento()) {
            case "cambio_estado" -> {
                boolean quedoActivo = "true".equals(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "activo"));
                yield actor + (quedoActivo ? " activó " : " desactivó ") + etiqueta + " " + referencia;
            }
            case "eliminacion" -> actor + " eliminó " + etiqueta + " " + referencia;
            case "cambio_precio" -> {
                String anterior = formatearSoles(RegistradorAuditoria.extraerCampo(fila.getValorAnterior(), "precioVenta"));
                String nuevo = formatearSoles(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "precioVenta"));
                yield actor + " cambió el precio de " + referencia + " de " + anterior + " a " + nuevo;
            }
            case "cambio_permiso" -> actor + " cambió los permisos de " + etiqueta + " " + referencia;
            // US-015: el local lo muestra la bitacora en su propia columna.
            // Sprint 5: el nombre del plan va en el campo "nombre"; sin valor anterior es el alta.
            case "cambio_plan" -> actor + (fila.getValorAnterior() == null ? " creó el plan " : " cambió los precios del plan ")
                    + referencia;
            case "cambio_parametro" -> descripcionDeParametro(fila, actor);
            case "movimiento_cuenta" -> descripcionDeMovimiento(fila, actor, referencia);
            case "precio_manual" -> actor + " puso el precio de un extra a mano: \"" + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "descripcion") + "\" a "
                    + formatearSoles(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "precio")) + " (pedido \"" + nombreDePedido(fila) + "\")";
            case "turno_abierto" -> actor + " abrió el turno de la \"" + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "caja")
                    + "\" con un fondo de " + formatearSoles(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "fondoInicial"));
            case "descuento" -> actor + " aplicó un descuento de " + formatearSoles(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "descuento"))
                    + " a \"" + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "producto") + "\" (pedido \""
                    + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "nombre") + "\"): "
                    + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "motivo");
            case "cobro_registrado" -> actor + " cobró el pedido \"" + nombreDePedido(fila) + "\" por "
                    + formatearSoles(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "total"))
                    + ("true".equals(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "pedidoCerrado")) ? " (pedido cerrado)" : "");
            case "caja_creada" -> actor + " creó la caja \"" + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "nombre") + "\"";
            case "pedido_terminado" -> actor + " terminó el pedido \"" + nombreDePedido(fila) + "\" sin cobrar"
                    + (RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "mesa") == null
                            || RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "mesa").isEmpty() ? ""
                            : " (mesa " + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "mesa") + " libre)");
            case "transferencia" -> descripcionDeTransferencia(fila, actor, referencia);
            case "comanda_emitida" -> actor + " comandó el pedido \"" + nombreDePedido(fila) + "\": "
                    + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "productos") + " productos (comanda "
                    + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "numero") + ")";
            case "reimpresion_comanda" -> actor + " reimprimió la comanda "
                    + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "numero") + " del pedido \""
                    + nombreDePedido(fila) + "\" (copia " + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "impresiones") + ")";
            case "anulacion" -> "detalle_pedido".equals(fila.getEntidad())
                    ? actor + " anuló \"" + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "producto")
                            + "\" del pedido \"" + nombreDePedido(fila) + "\""
                            + ("true".equals(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "despuesDeComandar"))
                                    ? " (después de comandar)" : " (antes de comandar)")
                            + ": " + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "motivo")
                    : actor + " anuló " + referencia;
            case "cambio_mesa" -> actor + (fila.getValorAnterior() == null ? " creó la mesa " : " modificó la mesa ")
                    + "\"" + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "numero") + "\"";
            case "bloqueo_mesa" -> "true".equals(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "bloqueada"))
                    ? actor + " bloqueó la mesa " + referencia + ": "
                            + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "motivo")
                    : actor + " liberó la mesa " + referencia;
            case "cambio_limite" -> RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "limite") == null
                    ? actor + " dejó que se calcule el tope de crédito de " + referencia
                    : actor + " fijó el tope de crédito de " + referencia + " en "
                            + formatearSoles(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "limite"));
            // Nunca dice cual es el PIN: la bitacora solo cuenta que se cambio.
            case "cambio_credencial" -> actor + " generó un PIN nuevo para " + referencia;
            case "marcado_agotado" -> actor + " marcó " + referencia + " como agotado";
            case "reactivacion" -> actor + " volvió a ofrecer " + referencia;
            // US-010: sin valor anterior es el alta; el "nombre" es el del producto que produce.
            case "cambio_receta" -> actor + (fila.getValorAnterior() == null ? " creó la receta de " : " modificó la receta de ")
                    + referencia;
            default -> actor + " registró un evento de " + fila.getEvento() + " sobre " + etiqueta + " " + referencia;
        };
    }

    /** El "nombre" del evento de un extra es "Pedido 0001": aqui solo el numero. */
    /** Sprint 6, US-029: reorganizar. El campo "tipo" distingue pasar productos, mover el pedido y unir o separar mesas. */
    private String descripcionDeTransferencia(Auditoria fila, String actor, String referencia) {
        String tipo = RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "tipo");
        String v = fila.getValorNuevo();
        return switch (tipo == null ? "" : tipo) {
            case "traslado_de_lineas" -> actor + " pasó " + RegistradorAuditoria.extraerCampo(v, "productos")
                    + " productos de \"" + RegistradorAuditoria.extraerCampo(v, "desde") + "\" a \""
                    + RegistradorAuditoria.extraerCampo(v, "hacia") + "\" en el pedido \"" + nombreDePedido(fila) + "\""
                    + ajusteDeTraslado(v);
            case "cambio_de_mesa" -> actor + " movió el pedido \"" + nombreDePedido(fila) + "\" de la mesa "
                    + RegistradorAuditoria.extraerCampo(v, "desde") + " a la mesa "
                    + RegistradorAuditoria.extraerCampo(v, "hacia");
            case "union_de_mesas" -> actor + " unió " + referencia + " a la mesa "
                    + RegistradorAuditoria.extraerCampo(v, "principal");
            case "separacion_de_mesas" -> actor + " separó " + referencia + " de la mesa "
                    + RegistradorAuditoria.extraerCampo(v, "principal");
            default -> actor + " hizo una transferencia en " + referencia;
        };
    }

    /** Si el traslado recalculo precios, dice cuanto cambio el total del pedido. */
    private String ajusteDeTraslado(String valorNuevo) {
        String ajuste = RegistradorAuditoria.extraerCampo(valorNuevo, "ajuste");
        if (ajuste == null || ajuste.equals("0")) {
            return "";
        }
        boolean baja = ajuste.startsWith("-");
        return " (el total " + (baja ? "bajó " : "subió ") + formatearSoles(baja ? ajuste.substring(1) : ajuste) + ")";
    }

    private String nombreDePedido(Auditoria fila) {
        String nombre = RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "nombre");
        return nombre == null ? "" : nombre.replaceFirst("^Pedido ", "");
    }

    /** Saldo inicial y ajustes de la cuenta de un pensionista (sprint 5, US-018). */
    private String descripcionDeMovimiento(Auditoria fila, String actor, String referencia) {
        String importe = formatearSoles(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "importe"));
        boolean cargo = "cargo".equals(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "signo"));
        if ("saldo_inicial".equals(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "tipo"))) {
            return actor + " cargó el saldo inicial de " + referencia + ": "
                    + (cargo ? "debe " : "tiene a favor ") + importe;
        }
        String motivo = RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "motivo");
        return actor + " ajustó la cuenta de " + referencia + ": " + (cargo ? "cargo" : "crédito") + " de " + importe
                + (motivo != null ? " (" + motivo + ")" : "");
    }

    /** "admin cambió el precio del táper de sin monto a S/ 1.00" -- el valor viene en centimos o como codigo de regla. */
    private String descripcionDeParametro(Auditoria fila, String actor) {
        String clave = RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "clave");
        String anterior = RegistradorAuditoria.extraerCampo(fila.getValorAnterior(), "valor");
        String nuevo = RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "valor");
        if ("taper_precio".equals(clave)) {
            return actor + " cambió el precio del táper de " + montoDeTaper(anterior) + " a " + montoDeTaper(nuevo);
        }
        if ("segundo_consumo_regla".equals(clave)) {
            return actor + " cambió la regla del segundo consumo del día de " + reglaLegible(anterior) + " a "
                    + reglaLegible(nuevo);
        }
        return actor + " cambió el parámetro " + (clave != null ? "\"" + clave + "\"" : "de cobro");
    }

    /** El taper sin valor es "sin monto" (no se cobra), distinto de "sin precio" de un producto. */
    private String montoDeTaper(String centimos) {
        return centimos == null ? "sin monto" : formatearSoles(centimos);
    }

    private String reglaLegible(String codigo) {
        return "precio_normal".equals(codigo) ? "precio normal" : "precio del plan";
    }

    private String etiquetaEntidad(String entidad) {
        return switch (entidad) {
            case "local" -> "el local";
            case "producto" -> "el producto";
            case "plan_tarifario" -> "el plan";
            case "pensionista" -> "el pensionista";
            case "mesa" -> "la mesa";
            case "proveedor" -> "el proveedor";
            case "categoria" -> "la categoría";
            case "usuario" -> "el usuario";
            case "rol" -> "el rol";
            default -> "\"" + entidad + "\"";
        };
    }

    private String formatearSoles(String centimosTexto) {
        if (centimosTexto == null) return "sin precio";
        try {
            long centimos = Long.parseLong(centimosTexto);
            return String.format("S/ %.2f", centimos / 100.0);
        } catch (NumberFormatException e) {
            return centimosTexto;
        }
    }
}

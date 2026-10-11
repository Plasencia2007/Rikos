package pe.rikos.operacion.venta.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.web.ErrorRespuesta;
import pe.rikos.operacion.venta.aplicacion.AbrirPedido;
import pe.rikos.operacion.venta.aplicacion.AgregarLinea;
import pe.rikos.operacion.venta.aplicacion.AgregarPersona;
import pe.rikos.operacion.venta.aplicacion.AnularLinea;
import pe.rikos.operacion.venta.aplicacion.AsignarPensionista;
import pe.rikos.operacion.venta.aplicacion.Canal;
import pe.rikos.operacion.venta.aplicacion.ComandaDetallada;
import pe.rikos.operacion.venta.aplicacion.ComandarPedido;
import pe.rikos.operacion.venta.aplicacion.ComponenteElegido;
import pe.rikos.operacion.venta.aplicacion.ConsultaComandas;
import pe.rikos.operacion.venta.aplicacion.ConsultaPedidos;
import pe.rikos.operacion.venta.aplicacion.FiltroDePedidos;
import pe.rikos.operacion.venta.aplicacion.ListaDePedidos;
import pe.rikos.operacion.venta.aplicacion.MoverPedidoDeMesa;
import pe.rikos.operacion.venta.aplicacion.RegistrarImpresion;
import pe.rikos.operacion.venta.aplicacion.TerminarPedido;
import pe.rikos.operacion.venta.aplicacion.TrasladarLineas;
import pe.rikos.operacion.venta.infraestructura.Comanda;
import pe.rikos.operacion.venta.aplicacion.ErrorPedido;
import pe.rikos.operacion.venta.aplicacion.LineaDetallada;
import pe.rikos.operacion.venta.aplicacion.ModificarLinea;
import pe.rikos.operacion.venta.aplicacion.OfertaDelDia;
import pe.rikos.operacion.venta.aplicacion.PedidoDetallado;
import pe.rikos.operacion.venta.aplicacion.PersonaDetallada;
import pe.rikos.operacion.venta.aplicacion.QuitarLinea;
import pe.rikos.operacion.venta.aplicacion.QuitarPersona;
import pe.rikos.operacion.venta.aplicacion.SolicitudDeLinea;
import pe.rikos.operacion.venta.aplicacion.TipoDeLinea;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * US-028: tomar el pedido. Ver, venta.ver; abrir, repartir entre personas y agregar, modificar o quitar lineas,
 * venta.crear. Todo del local de la sesion. Cada escritura devuelve el pedido completo con sus totales, para que
 * el POS repinte sin otra consulta. Las escrituras no van en una transaccion del controlador (igual que las
 * mesas): un choque de concurrencia sale ya cerrado del caso de uso y se contesta con un 409 claro.
 */
@RestController
public class PedidoController {

    private static final int INTENTOS_ANTE_UN_CHOQUE = 3;

    private final AbrirPedido abrirPedido;
    private final AgregarPersona agregarPersona;
    private final AsignarPensionista asignarPensionista;
    private final QuitarPersona quitarPersona;
    private final AgregarLinea agregarLinea;
    private final ModificarLinea modificarLinea;
    private final QuitarLinea quitarLinea;
    private final ConsultaPedidos consulta;
    private final ComandarPedido comandarPedido;
    private final RegistrarImpresion registrarImpresion;
    private final ConsultaComandas consultaComandas;
    private final AnularLinea anularLinea;
    private final ListaDePedidos listaDePedidos;
    private final TerminarPedido terminarPedido;
    private final TrasladarLineas trasladarLineas;
    private final MoverPedidoDeMesa moverPedidoDeMesa;

    public PedidoController(AbrirPedido abrirPedido, AgregarPersona agregarPersona,
                            AsignarPensionista asignarPensionista, QuitarPersona quitarPersona,
                            AgregarLinea agregarLinea, ModificarLinea modificarLinea, QuitarLinea quitarLinea,
                            ConsultaPedidos consulta, ComandarPedido comandarPedido,
                            RegistrarImpresion registrarImpresion, ConsultaComandas consultaComandas,
                            AnularLinea anularLinea, TrasladarLineas trasladarLineas,
                            MoverPedidoDeMesa moverPedidoDeMesa, ListaDePedidos listaDePedidos,
                            TerminarPedido terminarPedido) {
        this.listaDePedidos = listaDePedidos;
        this.terminarPedido = terminarPedido;
        this.trasladarLineas = trasladarLineas;
        this.moverPedidoDeMesa = moverPedidoDeMesa;
        this.comandarPedido = comandarPedido;
        this.registrarImpresion = registrarImpresion;
        this.consultaComandas = consultaComandas;
        this.anularLinea = anularLinea;
        this.abrirPedido = abrirPedido;
        this.agregarPersona = agregarPersona;
        this.asignarPensionista = asignarPensionista;
        this.quitarPersona = quitarPersona;
        this.agregarLinea = agregarLinea;
        this.modificarLinea = modificarLinea;
        this.quitarLinea = quitarLinea;
        this.consulta = consulta;
    }

    /**
     * canal: mesa | para_llevar. servicio (desayuno | almuerzo | cena) es opcional: sin el, el horario del local.
     * comensales (1 a 30, por defecto 1) crea esas personas; nombreCliente (hasta 120) es opcional.
     */
    public record PedidoRequest(String id, String canal, String mesaId, String servicio, Integer comensales,
                                String nombreCliente) {
    }

    public record PersonaRequest(String id, String etiqueta) {
    }

    public record PensionistaRequest(String pensionistaId) {
    }

    public record ComponenteRequest(String componente, String opcionMenuId) {
    }

    /**
     * tipo: menu_normal | menu_extra | plan_desayuno | plan_cena | producto | extra_libre. productoId solo en
     * producto; componentes solo en el menu; descripcion y precio (centimos) solo en extra_libre.
     */
    public record LineaRequest(String id, String personaId, String tipo, String productoId, int cantidad,
                               String observacion, List<ComponenteRequest> componentes, String descripcion,
                               Long precio) {
    }

    public record ModificarLineaRequest(int cantidad, String observacion) {
    }

    public record ComponenteRespuesta(String componente, String productoNombre) {
    }

    /** anuladaPostComanda y motivoAnulacion: solo con estado "anulado" (US-031); una anulada no suma. */
    public record LineaRespuesta(String id, String tipoLinea, String productoId, String productoNombre,
                                 String descripcion, int cantidad, String observacion, String estado,
                                 long precioUnitario, long taperUnitario, String tarifaOrigen, boolean segundoDelDia,
                                 List<ComponenteRespuesta> componentes, boolean anuladaPostComanda,
                                 String motivoAnulacion) {
    }

    public record ComandaRequest(String id) {
    }

    public record AnularRequest(String motivoId, String observacion) {
    }

    /** personaDestinoId null = una persona nueva (dividir la cuenta). */
    public record TrasladarRequest(List<String> lineaIds, String personaDestinoId) {
    }

    public record MoverRequest(String mesaId) {
    }

    public record ComandaResumen(String id, int numero, java.time.OffsetDateTime emitidaEn, int impresiones,
                                 java.time.OffsetDateTime ultimaImpresionEn, String estadoImpresion) {
    }

    public record LineaDeComandaRespuesta(int cantidad, String nombre, List<String> componentes, String observacion) {
    }

    public record PersonaDeComandaRespuesta(String etiqueta, List<LineaDeComandaRespuesta> lineas) {
    }

    /** Lo que la vista imprimible muestra: mesa es null si es para llevar. */
    public record ComandaRespuesta(String id, int numero, String pedidoNumero, String canal, String mesa,
                                   String servicio, String mozo, java.time.OffsetDateTime emitidaEn, int impresiones,
                                   List<PersonaDeComandaRespuesta> personas) {
    }

    /** copia: esta impresion es la segunda o posterior y debe salir con la leyenda COPIA (RN-063). */
    public record ImpresionRespuesta(String id, int numero, int impresiones, boolean copia) {
    }

    private static ComandaResumen resumen(Comanda c) {
        return new ComandaResumen(c.getId(), c.getNumero(), c.getEmitidaEn(), c.getImpresiones(),
                c.getUltimaImpresionEn(), c.getEstadoImpresion());
    }

    private static ComandaRespuesta de(ComandaDetallada d) {
        var c = d.comanda();
        return new ComandaRespuesta(c.getId(), c.getNumero(), d.pedidoNumero(), d.canal(), d.mesa(), d.servicio(),
                d.mozo(), c.getEmitidaEn(), c.getImpresiones(),
                d.personas().stream().map(p -> new PersonaDeComandaRespuesta(p.etiqueta(), p.lineas().stream()
                        .map(l -> new LineaDeComandaRespuesta(l.cantidad(), l.nombre(), l.componentes(),
                                l.observacion())).toList())).toList());
    }

    public record PersonaRespuesta(String id, String etiqueta, String tipoCliente, String pensionistaId,
                                   List<LineaRespuesta> lineas, long subtotal, long taper, long total) {
    }

    /** Todo en centimos; subtotal + taper = total. avisos: por ahora TAPER_SIN_MONTO (para llevar sin monto de taper). */
    public record PedidoRespuesta(String id, String numero, String canal, String servicio, String mesaId,
                                  String estado, List<PersonaRespuesta> personas, long subtotal, long taper,
                                  long total, List<String> avisos, String nombreCliente, String mozoNombre,
                                  java.time.OffsetDateTime abiertoEn) {
    }

    private static PedidoRespuesta de(PedidoDetallado d) {
        var p = d.pedido();
        return new PedidoRespuesta(p.getId(), p.getNumero(), p.getCanal(), p.getServicio(), p.getMesaId(),
                p.getEstado(), d.personas().stream().map(PedidoController::de).toList(), d.subtotal(), d.taper(),
                d.total(), d.avisos(), p.getNombreCliente(), d.mozoNombre(), p.getAbiertoEn());
    }

    private static PersonaRespuesta de(PersonaDetallada d) {
        var c = d.persona();
        return new PersonaRespuesta(c.getId(), c.getEtiqueta(), c.getTipoCliente(), c.getPensionistaId(),
                d.lineas().stream().map(PedidoController::de).toList(), d.subtotal(), d.taper(), d.total());
    }

    private static LineaRespuesta de(LineaDetallada d) {
        var l = d.linea();
        return new LineaRespuesta(l.getId(), l.getTipoLinea(), l.getProductoId(), d.productoNombre(),
                l.getDescripcion(), l.getCantidad(), l.getObservacion(), l.getEstado(), l.getPrecioUnitario(),
                l.getTaperUnitario(), l.getTarifaOrigen(), l.isSegundoDelDia(),
                d.componentes().stream().map(c -> new ComponenteRespuesta(c.componente(), c.productoNombre())).toList(),
                l.isAnuladoPostComanda(), d.motivoAnulacion());
    }

    @Operation(summary = "Abrir un pedido en una mesa libre o para llevar")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = PedidoRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/pedidos")
    public ResponseEntity<?> abrir(@RequestBody PedidoRequest cuerpo, @AuthenticationPrincipal String actorId) {
        var canal = Canal.desde(cuerpo.canal());
        if (canal.isEmpty()) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY, ErrorPedido.CANAL_INVALIDO.name());
        }
        return protegido(() -> {
            // Si la mesa ya tiene un pedido abierto sin comandar, abrir devuelve ESE pedido: 200 en vez de 201.
            String yaAbierto = cuerpo.mesaId() == null ? null : consulta.abiertoDeMesa(cuerpo.mesaId())
                    .esExito() ? consulta.abiertoDeMesa(cuerpo.mesaId()).valorOLanza().pedido().getId() : null;
            var resultado = abrirPedido.ejecutar(cuerpo.id(), canal.get(), cuerpo.mesaId(), cuerpo.servicio(),
                    cuerpo.comensales() == null ? 1 : cuerpo.comensales(), cuerpo.nombreCliente(), actorId);
            if (!resultado.esExito()) {
                return errorDe(resultado);
            }
            String id = resultado.valorOLanza().getId();
            return completo(id, id.equals(yaAbierto) ? HttpStatus.OK : HttpStatus.CREATED);
        });
    }

    @Operation(summary = "La lista de pedidos del local: en curso (con lineas) o cerrados, con filtros y paginacion")
    @GetMapping("/pedidos")
    public ResponseEntity<?> listar(@RequestParam(required = false) String situacion,
                                    @RequestParam(required = false) String canal,
                                    @RequestParam(required = false) String estado,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
                                    @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    @RequestParam(defaultValue = "20") int tamano) {
        var resultado = listaDePedidos.listar(new FiltroDePedidos(situacion, canal, estado, q, desde, hasta, pagina, tamano));
        return resultado.esExito() ? ResponseEntity.ok(resultado.valorOLanza()) : errorDe(resultado);
    }

    @Operation(summary = "Los conteos del menu lateral: pedidos en curso y por cobrar")
    @GetMapping("/pedidos/resumen")
    public ResponseEntity<?> resumen() {
        var resultado = listaDePedidos.resumen();
        return resultado.esExito() ? ResponseEntity.ok(resultado.valorOLanza()) : errorDe(resultado);
    }

    @Operation(summary = "Un pedido con sus personas, lineas y totales")
    @GetMapping("/pedidos/{id}")
    public ResponseEntity<?> obtener(@PathVariable String id) {
        return respuesta(consulta.pedido(id), HttpStatus.OK);
    }

    @Operation(summary = "El pedido en curso de una mesa")
    @GetMapping("/mesas/{mesaId}/pedido")
    public ResponseEntity<?> deLaMesa(@PathVariable String mesaId) {
        return respuesta(consulta.abiertoDeMesa(mesaId), HttpStatus.OK);
    }

    @Operation(summary = "Lo que se ofrece en este pedido: el menu de hoy y la carta sin los agotados")
    @GetMapping("/pedidos/{id}/oferta")
    public ResponseEntity<?> oferta(@PathVariable String id) {
        Resultado<OfertaDelDia, ErrorPedido> resultado = consulta.oferta(id);
        return resultado.esExito() ? ResponseEntity.ok(resultado.valorOLanza()) : errorDe(resultado);
    }

    @Operation(summary = "Sumar una persona (cliente normal) a la mesa")
    @PostMapping("/pedidos/{id}/personas")
    public ResponseEntity<?> agregarPersona(@PathVariable String id, @RequestBody PersonaRequest cuerpo,
                                            @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            var resultado = agregarPersona.ejecutar(id, cuerpo.id(), cuerpo.etiqueta(), actorId);
            return resultado.esExito() ? completo(id, HttpStatus.CREATED) : errorDe(resultado);
        });
    }

    @Operation(summary = "Asignar un pensionista (buscado por nombre o DNI) a una persona de la mesa")
    @PutMapping("/pedidos/{id}/personas/{personaId}/pensionista")
    public ResponseEntity<?> asignarPensionista(@PathVariable String id, @PathVariable String personaId,
                                                @RequestBody PensionistaRequest cuerpo,
                                                @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            var resultado = asignarPensionista.ejecutar(id, personaId, cuerpo.pensionistaId(), actorId);
            return resultado.esExito() ? completo(id, HttpStatus.OK) : errorDe(resultado);
        });
    }

    @Operation(summary = "Quitar una persona sin lineas")
    @DeleteMapping("/pedidos/{id}/personas/{personaId}")
    public ResponseEntity<?> quitarPersona(@PathVariable String id, @PathVariable String personaId) {
        return protegido(() -> {
            var resultado = quitarPersona.ejecutar(id, personaId);
            return resultado.esExito() ? completo(id, HttpStatus.OK) : errorDe(resultado);
        });
    }

    @Operation(summary = "Agregar una linea (menu, carta, comida del plan o extra) a una persona")
    @PostMapping("/pedidos/{id}/lineas")
    public ResponseEntity<?> agregarLinea(@PathVariable String id, @RequestBody LineaRequest cuerpo,
                                          @AuthenticationPrincipal String actorId) {
        var tipo = TipoDeLinea.desde(cuerpo.tipo());
        if (tipo.isEmpty()) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY, "TIPO_INVALIDO");
        }
        List<ComponenteElegido> elegidos = cuerpo.componentes() == null ? List.of()
                : cuerpo.componentes().stream().map(c -> new ComponenteElegido(c.componente(), c.opcionMenuId())).toList();
        return protegido(() -> {
            var resultado = agregarLinea.ejecutar(new SolicitudDeLinea(cuerpo.id(), id, cuerpo.personaId(), tipo.get(),
                    cuerpo.productoId(), cuerpo.cantidad(), cuerpo.observacion(), elegidos, cuerpo.descripcion(),
                    cuerpo.precio()), actorId);
            return resultado.esExito() ? completo(id, HttpStatus.CREATED) : errorDe(resultado);
        });
    }

    @Operation(summary = "Cambiar la cantidad y la observacion de una linea pendiente")
    @PutMapping("/pedidos/{id}/lineas/{lineaId}")
    public ResponseEntity<?> modificarLinea(@PathVariable String id, @PathVariable String lineaId,
                                            @RequestBody ModificarLineaRequest cuerpo,
                                            @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            var resultado = modificarLinea.ejecutar(id, lineaId, cuerpo.cantidad(), cuerpo.observacion(), actorId);
            return resultado.esExito() ? completo(id, HttpStatus.OK) : errorDe(resultado);
        });
    }

    @Operation(summary = "Quitar una linea pendiente (una ya comandada se anula: entrega 3)")
    @DeleteMapping("/pedidos/{id}/lineas/{lineaId}")
    public ResponseEntity<?> quitarLinea(@PathVariable String id, @PathVariable String lineaId,
                                         @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            var resultado = quitarLinea.ejecutar(id, lineaId, actorId);
            return resultado.esExito() ? completo(id, HttpStatus.OK) : errorDe(resultado);
        });
    }

    @Operation(summary = "Mandar a cocina lo pendiente de todo el pedido (una comanda nueva)")
    @PostMapping("/pedidos/{id}/comandas")
    public ResponseEntity<?> comandar(@PathVariable String id, @RequestBody(required = false) ComandaRequest cuerpo,
                                      @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            var resultado = comandarPedido.ejecutar(id, cuerpo == null ? null : cuerpo.id(), actorId);
            return resultado.esExito()
                    ? ResponseEntity.status(HttpStatus.CREATED).body(resumen(resultado.valorOLanza()))
                    : errorDe(resultado);
        });
    }

    @Operation(summary = "Las comandas de un pedido, por numero")
    @GetMapping("/pedidos/{id}/comandas")
    public ResponseEntity<?> comandasDelPedido(@PathVariable String id) {
        var resultado = consultaComandas.delPedido(id);
        return resultado.esExito()
                ? ResponseEntity.ok(resultado.valorOLanza().stream().map(PedidoController::resumen).toList())
                : errorDe(resultado);
    }

    @Operation(summary = "Una comanda con lo que se mando a cocina, lista para imprimir")
    @GetMapping("/comandas/{id}")
    public ResponseEntity<?> comanda(@PathVariable String id) {
        var resultado = consultaComandas.detalle(id);
        return resultado.esExito() ? ResponseEntity.ok(de(resultado.valorOLanza())) : errorDe(resultado);
    }

    @Operation(summary = "Registrar una impresion de la comanda; desde la segunda es COPIA")
    @PostMapping("/comandas/{id}/impresiones")
    public ResponseEntity<?> imprimir(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        var resultado = registrarImpresion.ejecutar(id, actorId);
        if (!resultado.esExito()) {
            return errorDe(resultado);
        }
        var impresion = resultado.valorOLanza();
        var c = impresion.comanda();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ImpresionRespuesta(c.getId(), c.getNumero(), c.getImpresiones(), impresion.copia()));
    }

    @Operation(summary = "Anular una linea con motivo (venta.anular); antes o despues de comandar")
    @PostMapping("/pedidos/{id}/lineas/{lineaId}/anular")
    public ResponseEntity<?> anular(@PathVariable String id, @PathVariable String lineaId,
                                    @RequestBody AnularRequest cuerpo, @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            var resultado = anularLinea.ejecutar(id, lineaId, cuerpo.motivoId(), cuerpo.observacion(), actorId);
            return resultado.esExito() ? completo(id, HttpStatus.OK) : errorDe(resultado);
        });
    }

    @Operation(summary = "Pasar lineas a otra persona del pedido o a una nueva (venta.modificar); recalcula el precio si cambia el tipo de cliente")
    @PostMapping("/pedidos/{id}/lineas/trasladar")
    public ResponseEntity<?> trasladar(@PathVariable String id, @RequestBody TrasladarRequest cuerpo,
                                       @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            var resultado = trasladarLineas.ejecutar(id, cuerpo.lineaIds(), cuerpo.personaDestinoId(), actorId);
            return resultado.esExito() ? completo(id, HttpStatus.OK) : errorDe(resultado);
        });
    }

    @Operation(summary = "Terminar el pedido SIN cobrar (venta.modificar): queda cerrado y la mesa libre; provisional hasta el sprint 7")
    @PostMapping("/pedidos/{id}/terminar")
    public ResponseEntity<?> terminar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            var resultado = terminarPedido.ejecutar(id, actorId);
            return resultado.esExito() ? completo(id, HttpStatus.OK) : errorDe(resultado);
        });
    }

    @Operation(summary = "Mover el pedido a otra mesa libre (venta.modificar)")
    @PutMapping("/pedidos/{id}/mesa")
    public ResponseEntity<?> moverDeMesa(@PathVariable String id, @RequestBody MoverRequest cuerpo,
                                         @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            var resultado = moverPedidoDeMesa.ejecutar(id, cuerpo.mesaId(), actorId);
            return resultado.esExito() ? completo(id, HttpStatus.OK) : errorDe(resultado);
        });
    }

    private ResponseEntity<?> completo(String pedidoId, HttpStatus estado) {
        return respuesta(consulta.pedido(pedidoId), estado);
    }

    private ResponseEntity<?> respuesta(Resultado<PedidoDetallado, ErrorPedido> resultado, HttpStatus estado) {
        return resultado.esExito() ? ResponseEntity.status(estado).body(de(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    /**
     * Las escrituras no van dentro de una transaccion del controlador: cada caso de uso confirma la suya, asi un
     * choque de concurrencia sale AQUI, ya cerrado, y se contesta con un 409. Una violacion de integridad que no
     * sea una de las conocidas no se disfraza: sigue su camino.
     */
    private ResponseEntity<?> protegido(Supplier<ResponseEntity<?>> accion) {
        for (int intento = 1; ; intento++) {
            try {
                return accion.get();
            } catch (ObjectOptimisticLockingFailureException e) {
                return error(HttpStatus.CONFLICT, "PEDIDO_MODIFICADO");
            } catch (DataIntegrityViolationException e) {
                String mensaje = String.valueOf(e.getMessage());
                // Dos mozos abriendo a la vez toman el mismo numero, o el cliente reenvia un id mientras el primer
                // envio sigue en curso: los casos de uso son idempotentes por id, asi que se reintenta (el segundo
                // intento asigna otro numero, o encuentra lo que el primero ya guardo).
                boolean numeroRepetido = mensaje.contains("pedido_numero_unico");
                boolean idRepetido = mensaje.contains("pedido_pkey") || mensaje.contains("cuenta_persona_pkey")
                        || mensaje.contains("detalle_pedido_pkey") || mensaje.contains("comanda_pkey");
                // Dos comandar a la vez: el segundo intento ya no encuentra lineas pendientes (o la misma comanda).
                boolean comandaRepetida = mensaje.contains("comanda_numero_unico")
                        || mensaje.contains("detalle_comanda_linea_unica");
                if (comandaRepetida && intento < INTENTOS_ANTE_UN_CHOQUE) {
                    continue;
                }
                if (comandaRepetida) {
                    return error(HttpStatus.CONFLICT, "NUMERO_DE_COMANDA_REPETIDO");
                }
                if ((numeroRepetido || idRepetido) && intento < INTENTOS_ANTE_UN_CHOQUE) {
                    continue;
                }
                if (numeroRepetido) {
                    return error(HttpStatus.CONFLICT, "NUMERO_DE_PEDIDO_REPETIDO");
                }
                if (idRepetido) {
                    return error(HttpStatus.CONFLICT, "PEDIDO_MODIFICADO");
                }
                if (mensaje.contains("ux_pedido_mesa_en_curso")) {
                    // Dos aperturas a la vez sobre la misma mesa: la que perdio se reintenta y encuentra el pedido que ya
                    // abrio la otra (abierto sin comandar: lo devuelve; comandado: MESA_OCUPADA).
                    if (intento < INTENTOS_ANTE_UN_CHOQUE) {
                        continue;
                    }
                    return error(HttpStatus.CONFLICT, ErrorPedido.MESA_OCUPADA.name());
                }
                if (mensaje.contains("ux_cuenta_persona_pensionista")) {
                    return error(HttpStatus.CONFLICT, ErrorPedido.PENSIONISTA_YA_EN_EL_PEDIDO.name());
                }
                throw e;
            }
        }
    }

    private static ResponseEntity<?> errorDe(Resultado<?, ErrorPedido> resultado) {
        ErrorPedido e = ((Resultado.Fracaso<?, ErrorPedido>) resultado).error();
        HttpStatus estado = switch (e) {
            case NO_ENCONTRADO, MESA_NO_ENCONTRADA, PERSONA_NO_ENCONTRADA, LINEA_NO_ENCONTRADA,
                 PRODUCTO_NO_ENCONTRADO, PENSIONISTA_NO_ENCONTRADO, COMANDA_NO_ENCONTRADA -> HttpStatus.NOT_FOUND;
            case MESA_OCUPADA, PENSIONISTA_YA_EN_EL_PEDIDO, PERSONA_CON_LINEAS, LINEA_YA_COMANDADA,
                 PRODUCTO_AGOTADO, SIN_LINEAS_PENDIENTES, PEDIDO_NO_COMANDABLE, LINEA_ANULADA,
                 PEDIDO_NO_REORGANIZABLE, TIENE_MESAS_UNIDAS, PEDIDO_NO_TERMINABLE, PEDIDO_CERRADO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return error(estado, e.name());
    }

    private static ResponseEntity<?> error(HttpStatus estado, String codigo) {
        return ResponseEntity.status(estado).body(Map.of("error", codigo));
    }
}

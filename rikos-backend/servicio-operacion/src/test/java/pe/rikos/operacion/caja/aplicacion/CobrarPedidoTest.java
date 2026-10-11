package pe.rikos.operacion.caja.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.CobroLineaRepositorio;
import pe.rikos.operacion.caja.infraestructura.CobroRepositorio;
import pe.rikos.operacion.caja.infraestructura.MovimientoCajaRepositorio;
import pe.rikos.operacion.caja.infraestructura.PagoRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaCuenta;
import pe.rikos.operacion.venta.aplicacion.AnularLinea;
import pe.rikos.operacion.venta.aplicacion.AsignarPensionista;
import pe.rikos.operacion.venta.aplicacion.ComandasBase;
import pe.rikos.operacion.venta.aplicacion.ConsultaMesas;
import pe.rikos.operacion.venta.aplicacion.ErrorPedido;
import pe.rikos.operacion.venta.aplicacion.EstadoDeMesa;
import pe.rikos.operacion.venta.aplicacion.TerminarPedido;
import pe.rikos.operacion.venta.aplicacion.TrasladarLineas;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-033 (sprint 7, entrega 2): el cobro por lineas. El cajero marca las lineas que se pagan ahora (aunque sean de varias
 * personas), reparte el pago entre medios (efectivo con vuelto) y, si hay lineas de un pensionista, el sistema las carga a
 * su cuenta en vez de pedir medios. La mesa se libera cuando no queda nada por cobrar.
 */
@SpringBootTest
@Transactional
class CobrarPedidoTest extends ComandasBase {

    @Autowired
    CobrarPedido cobrar;
    @Autowired
    AbrirTurno abrirTurno;
    @Autowired
    ConsultaCajas consultaCajas;
    @Autowired
    CobroRepositorio cobros;
    @Autowired
    CobroLineaRepositorio cobroLineas;
    @Autowired
    PagoRepositorio pagos;
    @Autowired
    MovimientoCajaRepositorio movimientos;
    @Autowired
    DetallePedidoRepositorio lineas;
    @Autowired
    PedidoRepositorio pedidos;
    @Autowired
    ConsultaCuenta consultaCuenta;
    @Autowired
    ConsultaMesas consultaMesas;
    @Autowired
    AsignarPensionista asignarPensionista;
    @Autowired
    AnularLinea anularLinea;
    @Autowired
    TrasladarLineas trasladarLineas;
    @Autowired
    TerminarPedido terminarPedido;

    TurnoCaja turno;

    @BeforeEach
    void abrirElTurno() {
        var caja = consultaCajas.listar().valorOLanza().get(0).caja();
        turno = abrirTurno.ejecutar(caja.getId(), 10000L, ADMIN_SEMBRADO_ID).valorOLanza();
    }

    static ErrorCobro error(Resultado<?, ErrorCobro> resultado) {
        assertThat(resultado.esFracaso()).as("se esperaba un fracaso").isTrue();
        return ((Resultado.Fracaso<?, ErrorCobro>) resultado).error();
    }

    /** Una linea ya comandada de la persona indicada (precio 500 por unidad). */
    private DetallePedido comandada(String personaId, String nombre, int cantidad) {
        DetallePedido l = linea(personaId, nombre, cantidad);
        return l;
    }

    private void comandar() {
        comandarPedido.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
    }

    private Resultado<CobroRegistrado, ErrorCobro> cobrarLineas(List<String> ids, PagoSolicitado... pagosDados) {
        return cobrar.ejecutar(pedido.getId(), ids, List.of(pagosDados), ADMIN_SEMBRADO_ID);
    }

    private String estadoDelPedido() {
        return pedidos.findById(pedido.getId()).orElseThrow().getEstado();
    }

    @Test
    void cobrar_en_efectivo_calcula_el_vuelto_registra_el_movimiento_y_cierra_el_pedido() {
        var plato = comandada(persona, "Lomo", 2);
        comandar();

        var cobro = cobrarLineas(List.of(plato.getId()), new PagoSolicitado("efectivo", 2000L, null)).valorOLanza();

        assertThat(cobro.cobro().getTotal()).isEqualTo(1000L);
        assertThat(cobro.cobro().getTotalMedios()).isEqualTo(1000L);
        assertThat(cobro.cobro().getTotalCuentas()).isZero();
        assertThat(cobro.cobro().getVuelto()).isEqualTo(1000L);
        assertThat(cobro.cobro().getTurnoCajaId()).isEqualTo(turno.getId());
        assertThat(cobro.pagos()).singleElement().satisfies(p -> {
            assertThat(p.getImporte()).isEqualTo(1000L);
            assertThat(p.getMontoRecibido()).isEqualTo(2000L);
        });
        // La caja recibe lo que se cobro, no lo que entrego el cliente: el vuelto no es ingreso (RN-069).
        assertThat(movimientos.findByTurnoCajaId(turno.getId())).singleElement().satisfies(m -> {
            assertThat(m.getTipo()).isEqualTo("venta");
            assertThat(m.getMedio()).isEqualTo("efectivo");
            assertThat(m.getImporte()).isEqualTo(1000L);
        });
        assertThat(lineas.findById(plato.getId()).orElseThrow().getEstado()).isEqualTo("cobrado");
        assertThat(cobro.pedidoCerrado()).isTrue();
        assertThat(estadoDelPedido()).isEqualTo("cerrado");
    }

    @Test
    void el_pago_se_puede_dividir_entre_medios() {
        var plato = comandada(persona, "Lomo", 2);
        comandar();

        var cobro = cobrarLineas(List.of(plato.getId()), new PagoSolicitado("yape", 400L, "OP-123"),
                new PagoSolicitado("efectivo", 700L, null), new PagoSolicitado("tarjeta", 100L, null)).valorOLanza();

        assertThat(cobro.cobro().getVuelto()).isEqualTo(200L);
        assertThat(cobro.pagos()).extracting(p -> p.getMedio()).containsExactlyInAnyOrder("yape", "efectivo", "tarjeta");
        assertThat(cobro.pagos().stream().mapToLong(p -> p.getImporte()).sum()).isEqualTo(1000L);
        assertThat(movimientos.findByTurnoCajaId(turno.getId()).stream().mapToLong(m -> m.getImporte()).sum()).isEqualTo(1000L);
        assertThat(cobro.pagos().stream().filter(p -> p.getMedio().equals("yape")).findFirst().orElseThrow().getReferencia())
                .isEqualTo("OP-123");
    }

    @Test
    void un_pago_exacto_no_tiene_vuelto() {
        var plato = comandada(persona, "Lomo", 1);
        comandar();

        var cobro = cobrarLineas(List.of(plato.getId()), new PagoSolicitado("efectivo", 500L, null)).valorOLanza();

        assertThat(cobro.cobro().getVuelto()).isZero();
    }

    @Test
    void si_la_suma_no_cubre_el_total_no_se_cobra_y_la_linea_sigue_comandada() {
        var plato = comandada(persona, "Lomo", 2);
        comandar();

        assertThat(error(cobrarLineas(List.of(plato.getId()), new PagoSolicitado("efectivo", 999L, null))))
                .isEqualTo(ErrorCobro.PAGO_INSUFICIENTE);
        assertThat(lineas.findById(plato.getId()).orElseThrow().getEstado()).isEqualTo("comandado");
        assertThat(movimientos.findByTurnoCajaId(turno.getId())).isEmpty();
    }

    @Test
    void solo_el_efectivo_puede_dar_vuelto() {
        var plato = comandada(persona, "Lomo", 1);
        comandar();

        assertThat(error(cobrarLineas(List.of(plato.getId()), new PagoSolicitado("yape", 600L, null))))
                .isEqualTo(ErrorCobro.PAGO_EXCEDE_EL_TOTAL);
        // Digital exacto mas efectivo de mas: el vuelto sale del efectivo, no del digital.
        assertThat(error(cobrarLineas(List.of(plato.getId()), new PagoSolicitado("yape", 500L, null),
                new PagoSolicitado("tarjeta", 100L, null)))).isEqualTo(ErrorCobro.PAGO_EXCEDE_EL_TOTAL);
    }

    @Test
    void los_pagos_se_validan_antes_de_tocar_nada() {
        var plato = comandada(persona, "Lomo", 1);
        comandar();

        assertThat(error(cobrarLineas(List.of(plato.getId()), new PagoSolicitado("bitcoin", 500L, null))))
                .isEqualTo(ErrorCobro.MEDIO_INVALIDO);
        assertThat(error(cobrarLineas(List.of(plato.getId()), new PagoSolicitado("efectivo", 0L, null))))
                .isEqualTo(ErrorCobro.MONTO_INVALIDO);
        assertThat(error(cobrarLineas(List.of(plato.getId())))).isEqualTo(ErrorCobro.PAGO_INSUFICIENTE);
        assertThat(error(cobrarLineas(List.of(plato.getId()), new PagoSolicitado("yape", 500L, "x".repeat(51)))))
                .isEqualTo(ErrorCobro.REFERENCIA_INVALIDA);
    }

    @Test
    void sin_turno_abierto_no_se_cobra() {
        var plato = comandada(persona, "Lomo", 1);
        comandar();
        String otroCajero = nuevoUsuarioId();

        assertThat(error(cobrar.ejecutar(pedido.getId(), List.of(plato.getId()),
                List.of(new PagoSolicitado("efectivo", 500L, null)), otroCajero))).isEqualTo(ErrorCobro.SIN_TURNO_ABIERTO);
    }

    @Test
    void el_turno_de_otro_local_no_sirve() {
        var plato = comandada(persona, "Lomo", 1);
        comandar();
        pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);

        // En el local B el pedido ni se encuentra: un pedido de otro local nunca se filtra.
        assertThat(error(cobrarLineas(List.of(plato.getId()), new PagoSolicitado("efectivo", 500L, null))))
                .isEqualTo(ErrorCobro.PEDIDO_NO_ENCONTRADO);
    }

    @Test
    void el_cierre_parcial_no_libera_la_mesa_hasta_cobrar_la_ultima_linea() {
        var uno = comandada(persona, "Lomo", 1);
        var dos = comandada(persona, "Arroz", 1);
        comandar();

        var parcial = cobrarLineas(List.of(uno.getId()), new PagoSolicitado("efectivo", 500L, null)).valorOLanza();

        assertThat(parcial.pedidoCerrado()).isFalse();
        assertThat(estadoDelPedido()).isEqualTo("comandado");
        assertThat(lineas.findById(dos.getId()).orElseThrow().getEstado()).isEqualTo("comandado");

        var resto = cobrarLineas(List.of(dos.getId()), new PagoSolicitado("yape", 500L, null)).valorOLanza();

        assertThat(resto.pedidoCerrado()).isTrue();
        assertThat(estadoDelPedido()).isEqualTo("cerrado");
        assertThat(cobros.findByPedidoIdOrderByCobradoEn(pedido.getId())).hasSize(2);
    }

    @Test
    void se_cobran_lineas_de_distintas_personas_en_un_solo_cobro() {
        agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID);
        String segunda = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(1).getId();
        var uno = comandada(persona, "Lomo", 1);
        var dos = comandada(segunda, "Arroz", 2);
        comandar();

        var cobro = cobrarLineas(List.of(uno.getId(), dos.getId()), new PagoSolicitado("efectivo", 1500L, null)).valorOLanza();

        assertThat(cobro.cobro().getTotal()).isEqualTo(1500L);
        assertThat(cobroLineas.findByCobroIdIn(List.of(cobro.cobro().getId()))).hasSize(2);
        assertThat(cobro.pedidoCerrado()).isTrue();
    }

    @Test
    void una_linea_sin_comandar_no_se_cobra() {
        var pendiente = comandada(persona, "Lomo", 1);

        assertThat(error(cobrarLineas(List.of(pendiente.getId()), new PagoSolicitado("efectivo", 500L, null))))
                .isEqualTo(ErrorCobro.LINEA_SIN_COMANDAR);
    }

    @Test
    void una_linea_ya_cobrada_no_se_cobra_dos_veces() {
        var uno = comandada(persona, "Lomo", 1);
        comandada(persona, "Arroz", 1);
        comandar();
        cobrarLineas(List.of(uno.getId()), new PagoSolicitado("efectivo", 500L, null)).valorOLanza();

        assertThat(error(cobrarLineas(List.of(uno.getId()), new PagoSolicitado("efectivo", 500L, null))))
                .isEqualTo(ErrorCobro.LINEA_YA_COBRADA);
    }

    @Test
    void una_linea_anulada_o_inexistente_no_se_cobra_y_no_se_cobra_ninguna() {
        var buena = comandada(persona, "Lomo", 1);
        var mala = comandada(persona, "Arroz", 1);
        comandar();
        var motivo = motivoDeAnulacion("Se cayo", false);
        anularLinea.ejecutar(pedido.getId(), mala.getId(), motivo.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(cobrarLineas(List.of(buena.getId(), mala.getId()), new PagoSolicitado("efectivo", 1000L, null))))
                .isEqualTo(ErrorCobro.LINEA_ANULADA);
        assertThat(error(cobrarLineas(List.of(buena.getId(), "no-existe"), new PagoSolicitado("efectivo", 1000L, null))))
                .isEqualTo(ErrorCobro.LINEA_NO_ENCONTRADA);
        assertThat(error(cobrarLineas(List.of(), new PagoSolicitado("efectivo", 1000L, null)))).isEqualTo(ErrorCobro.SIN_LINEAS);
        assertThat(lineas.findById(buena.getId()).orElseThrow().getEstado()).isEqualTo("comandado");
    }

    @Test
    void con_una_linea_anulada_el_pedido_se_cierra_al_cobrar_la_otra() {
        var buena = comandada(persona, "Lomo", 1);
        var mala = comandada(persona, "Arroz", 1);
        comandar();
        var motivo = motivoDeAnulacion("Se cayo", false);
        anularLinea.ejecutar(pedido.getId(), mala.getId(), motivo.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var cobro = cobrarLineas(List.of(buena.getId()), new PagoSolicitado("efectivo", 500L, null)).valorOLanza();

        assertThat(cobro.pedidoCerrado()).isTrue();
    }

    @Test
    void al_cerrarse_el_pedido_la_mesa_queda_libre() {
        var plato = comandada(persona, "Lomo", 1);
        comandar();
        String mesaId = pedido.getMesaId();
        assertThat(consultaMesas.todas().valorOLanza().stream().filter(f -> f.mesa().getId().equals(mesaId)).findFirst()
                .orElseThrow().estado()).isEqualTo(EstadoDeMesa.OCUPADA);

        cobrarLineas(List.of(plato.getId()), new PagoSolicitado("efectivo", 500L, null)).valorOLanza();

        assertThat(consultaMesas.todas().valorOLanza().stream().filter(f -> f.mesa().getId().equals(mesaId)).findFirst()
                .orElseThrow().estado()).isEqualTo(EstadoDeMesa.LIBRE);
    }

    @Test
    void el_snapshot_guarda_precio_taper_nombre_e_importe_y_costo_nulo_si_no_hay_receta() {
        var plato = comandada(persona, "Lomo snapshot", 3);
        comandar();

        cobrarLineas(List.of(plato.getId()), new PagoSolicitado("efectivo", 1500L, null)).valorOLanza();

        var snapshot = cobroLineas.findAll().stream().filter(l -> l.getDetallePedidoId().equals(plato.getId())).findFirst()
                .orElseThrow();
        assertThat(snapshot.getPrecioUnitario()).isEqualTo(500L);
        assertThat(snapshot.getCantidad()).isEqualTo(3);
        assertThat(snapshot.getImporte()).isEqualTo(1500L);
        assertThat(snapshot.getDescuento()).isZero();
        assertThat(snapshot.getDestino()).isEqualTo("medios");
        assertThat(snapshot.getNombre()).startsWith("Lomo snapshot");
        // Sin receta no hay costo: se guarda nulo, nunca cero (un cero mentiria el margen).
        assertThat(snapshot.getCostoUnitario()).isNull();
        assertThat(snapshot.getUtilidad()).isNull();
    }

    @Test
    void una_linea_cobrada_ya_no_se_anula_ni_se_traslada_ni_se_termina_el_pedido_por_ella() {
        var uno = comandada(persona, "Lomo", 1);
        comandada(persona, "Arroz", 1);
        comandar();
        cobrarLineas(List.of(uno.getId()), new PagoSolicitado("efectivo", 500L, null)).valorOLanza();
        var motivo = motivoDeAnulacion("Error", false);

        assertThat(anularLinea.ejecutar(pedido.getId(), uno.getId(), motivo.getId(), null, ADMIN_SEMBRADO_ID))
                .satisfies(r -> assertThat(r.esFracaso()).isTrue());
        assertThat(((Resultado.Fracaso<?, ErrorPedido>) anularLinea.ejecutar(pedido.getId(), uno.getId(), motivo.getId(),
                null, ADMIN_SEMBRADO_ID)).error()).isEqualTo(ErrorPedido.LINEA_COBRADA);
        assertThat(((Resultado.Fracaso<?, ErrorPedido>) trasladarLineas.ejecutar(pedido.getId(), List.of(uno.getId()), null,
                ADMIN_SEMBRADO_ID)).error()).isEqualTo(ErrorPedido.LINEA_COBRADA);
    }

    @Test
    void terminar_pedido_solo_cierra_un_pedido_sin_lineas_comandadas() {
        comandada(persona, "Lomo", 1);
        comandar();

        assertThat(((Resultado.Fracaso<?, ErrorPedido>) terminarPedido.ejecutar(pedido.getId(), ADMIN_SEMBRADO_ID)).error())
                .isEqualTo(ErrorPedido.PEDIDO_CON_LINEAS_POR_COBRAR);
        assertThat(estadoDelPedido()).isEqualTo("comandado");
    }

    // ---- pensionistas ----

    private pe.rikos.operacion.venta.infraestructura.CuentaPersona personaPensionista(String etiqueta) {
        var plan = nuevoPlan(500L, 900L, 600L);
        var pensionista = nuevoPensionista(plan, "Cobrado");
        agregarPersona.ejecutar(pedido.getId(), null, etiqueta, ADMIN_SEMBRADO_ID);
        var cuenta = personas.findByPedidoIdOrderByOrden(pedido.getId()).stream().filter(p -> p.getEtiqueta().equals(etiqueta))
                .findFirst().orElseThrow();
        asignarPensionista.ejecutar(pedido.getId(), cuenta.getId(), pensionista.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        return personas.findById(cuenta.getId()).orElseThrow();
    }

    @Test
    void las_lineas_de_un_pensionista_se_cargan_a_su_cuenta_y_no_piden_medios() {
        var cuenta = personaPensionista("Luis");
        var suya = comandada(cuenta.getId(), "Plato de Luis", 2);
        comandar();
        long saldoAntes = consultaCuenta.resumen(cuenta.getPensionistaId()).orElseThrow().saldo();

        var cobro = cobrarLineas(List.of(suya.getId())).valorOLanza();

        assertThat(cobro.cobro().getTotalMedios()).isZero();
        assertThat(cobro.cobro().getTotalCuentas()).isEqualTo(1000L);
        assertThat(cobro.pagos()).isEmpty();
        assertThat(movimientos.findByTurnoCajaId(turno.getId())).isEmpty();
        assertThat(cobro.cargos()).singleElement().satisfies(c -> {
            assertThat(c.pensionistaId()).isEqualTo(cuenta.getPensionistaId());
            assertThat(c.importe()).isEqualTo(1000L);
        });
        assertThat(consultaCuenta.resumen(cuenta.getPensionistaId()).orElseThrow().saldo()).isEqualTo(saldoAntes - 1000L);
        assertThat(cobroLineas.findAll().stream().filter(l -> l.getDetallePedidoId().equals(suya.getId())).findFirst()
                .orElseThrow().getDestino()).isEqualTo("cuenta");
        assertThat(cobro.pedidoCerrado()).isTrue();
    }

    @Test
    void una_mesa_mixta_separa_el_pago_en_medios_y_el_cargo_a_la_cuenta() {
        var cuenta = personaPensionista("Luis");
        var normal = comandada(persona, "Plato normal", 1);
        var suya = comandada(cuenta.getId(), "Plato de Luis", 1);
        comandar();

        var cobro = cobrarLineas(List.of(normal.getId(), suya.getId()), new PagoSolicitado("efectivo", 1000L, null))
                .valorOLanza();

        assertThat(cobro.cobro().getTotal()).isEqualTo(1000L);
        assertThat(cobro.cobro().getTotalMedios()).isEqualTo(500L);
        assertThat(cobro.cobro().getTotalCuentas()).isEqualTo(500L);
        assertThat(cobro.cobro().getVuelto()).isEqualTo(500L);
        assertThat(movimientos.findByTurnoCajaId(turno.getId()).stream().mapToLong(m -> m.getImporte()).sum()).isEqualTo(500L);
        assertThat(cobro.cargos()).hasSize(1);
    }

    @Test
    void el_cargo_avisa_si_supera_el_tope_pero_nunca_impide_cobrar() {
        var cuenta = personaPensionista("Luis");
        var suya = comandada(cuenta.getId(), "Plato caro", 1);
        comandar();

        var cobro = cobrarLineas(List.of(suya.getId())).valorOLanza();

        // El alerta depende del tope calculado de la cuenta; lo que no puede pasar es que bloquee el cobro.
        assertThat(cobro.cargos()).hasSize(1);
        assertThat(cobro.cargos().get(0).superaTope())
                .isEqualTo(consultaCuenta.resumen(cuenta.getPensionistaId()).orElseThrow().superaTope());
    }

    @Test
    void no_se_pagan_en_medios_lineas_que_solo_van_a_una_cuenta() {
        var cuenta = personaPensionista("Luis");
        var suya = comandada(cuenta.getId(), "Plato de Luis", 1);
        comandar();

        assertThat(error(cobrarLineas(List.of(suya.getId()), new PagoSolicitado("efectivo", 500L, null))))
                .isEqualTo(ErrorCobro.PAGO_SIN_NADA_QUE_PAGAR);
    }

    @Test
    void un_pensionista_desactivado_no_consume() {
        var cuenta = personaPensionista("Luis");
        var suya = comandada(cuenta.getId(), "Plato de Luis", 1);
        comandar();
        var pensionista = pensionistasRepo.findById(cuenta.getPensionistaId()).orElseThrow();
        pensionista.cambiarEstado(false, ADMIN_SEMBRADO_ID);
        pensionistasRepo.save(pensionista);

        assertThat(error(cobrarLineas(List.of(suya.getId())))).isEqualTo(ErrorCobro.PENSIONISTA_INACTIVO);
        assertThat(lineas.findById(suya.getId()).orElseThrow().getEstado()).isEqualTo("comandado");
    }

    // ---- endurecimiento (auditoria CAJ-01 y CAJ-06) ----

    @Test
    void dos_pagos_en_el_mismo_medio_se_rechazan_en_vez_de_romper_con_un_500() {
        var plato = comandada(persona, "Lomo", 2);
        comandar();

        assertThat(error(cobrarLineas(List.of(plato.getId()), new PagoSolicitado("efectivo", 500L, null),
                new PagoSolicitado("efectivo", 500L, null)))).isEqualTo(ErrorCobro.MEDIO_REPETIDO);
        assertThat(lineas.findById(plato.getId()).orElseThrow().getEstado()).isEqualTo("comandado");
    }

    @Test
    void un_pago_nulo_o_un_monto_descomunal_se_rechazan_sin_escribir_nada() {
        var plato = comandada(persona, "Lomo", 1);
        comandar();
        var conNulo = new java.util.ArrayList<PagoSolicitado>();
        conNulo.add(null);

        assertThat(error(cobrar.ejecutar(pedido.getId(), List.of(plato.getId()), conNulo, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCobro.MEDIO_INVALIDO);
        assertThat(error(cobrarLineas(List.of(plato.getId()), new PagoSolicitado("efectivo", Long.MAX_VALUE, null))))
                .isEqualTo(ErrorCobro.MONTO_INVALIDO);
        assertThat(error(cobrarLineas(List.of(plato.getId()), new PagoSolicitado("yape", 400L, null),
                new PagoSolicitado("plin", 100L, null), new PagoSolicitado("tarjeta", 100L, null),
                new PagoSolicitado("efectivo", 1000L, null), new PagoSolicitado("yape", 1L, null))))
                .isEqualTo(ErrorCobro.MEDIO_REPETIDO);
        assertThat(movimientos.findByTurnoCajaId(turno.getId())).isEmpty();
        assertThat(lineas.findById(plato.getId()).orElseThrow().getEstado()).isEqualTo("comandado");
    }

    @Autowired
    pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio pensionistasRepo;
    @Autowired
    pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio usuariosRepo;

    private String nuevoUsuarioId() {
        String sufijo = pe.rikos.contratos.Identificador.nuevo().valor().substring(16);
        return usuariosRepo.save(new pe.rikos.operacion.identidad.infraestructura.Usuario(
                pe.rikos.contratos.Identificador.nuevo().valor(), "cobra-" + sufijo, "Otro", "Cajero", null, null, null, "x",
                null)).getId();
    }
}

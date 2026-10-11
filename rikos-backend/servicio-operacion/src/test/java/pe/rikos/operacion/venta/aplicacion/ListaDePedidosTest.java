package pe.rikos.operacion.venta.aplicacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprint 6b: la lista de pedidos del local (en curso y cerrados) con filtros y paginacion, y sus conteos. */
@SpringBootTest
@Transactional
class ListaDePedidosTest extends ComandasBase {

    @Autowired
    ListaDePedidos lista;
    @Autowired
    ComandarPedido comandar;
    @Autowired
    AnularLinea anular;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManager em;

    private static FiltroDePedidos filtro(String situacion, String canal, String estado, String q, LocalDate desde,
                                          LocalDate hasta, int pagina, int tamano) {
        return new FiltroDePedidos(situacion, canal, estado, q, desde, hasta, pagina, tamano);
    }

    private static FiltroDePedidos enCurso() {
        return filtro(null, null, null, null, null, null, 0, 20);
    }

    private static ErrorPedido errorDeLista(pe.rikos.contratos.Resultado<?, ErrorPedido> r) {
        assertThat(r.esExito()).isFalse();
        return ((pe.rikos.contratos.Resultado.Fracaso<?, ErrorPedido>) r).error();
    }

    private void flushYLimpiar() {
        em.flush();
        em.clear();
    }

    @Test
    void trae_los_pedidos_en_curso_con_su_mesa_mozo_comensales_total_y_lineas() {
        agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID);
        var chicha = linea("Chicha", 2);
        linea("Lomo", 1);
        comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        linea("Extra", 1);
        jdbc.update("update pedido set nombre_cliente = 'Familia Garcia' where id = ?", pedido.getId());
        flushYLimpiar();

        var pagina = lista.listar(enCurso()).valorOLanza();

        assertThat(pagina.total()).isEqualTo(1);
        var fila = pagina.contenido().get(0);
        assertThat(fila.id()).isEqualTo(pedido.getId());
        assertThat(fila.numero()).isEqualTo(pedido.getNumero());
        assertThat(fila.canal()).isEqualTo("mesa");
        assertThat(fila.estado()).isEqualTo("comandado");
        assertThat(fila.mesaNumero()).isEqualTo("1");
        assertThat(fila.salonNombre()).isNotBlank();
        assertThat(fila.nombreCliente()).isEqualTo("Familia Garcia");
        assertThat(fila.comensales()).isEqualTo(2);
        assertThat(fila.mozoNombre()).isNotBlank();
        assertThat(fila.abiertoEn()).isNotNull();
        assertThat(fila.total()).isEqualTo(chicha.getPrecioUnitario() * 2 + 500 + 500);
        assertThat(fila.lineas()).hasSize(3);
        assertThat(fila.lineas()).extracting("estado").containsExactlyInAnyOrder("comandado", "comandado", "pendiente");
    }

    @Test
    void las_lineas_anuladas_no_suman_ni_salen_en_la_lista() {
        var chicha = linea("Chicha", 1);
        linea("Lomo", 1);
        comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        var motivo = motivoDeAnulacion("Error", false);
        anular.ejecutar(pedido.getId(), chicha.getId(), motivo.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        flushYLimpiar();

        var fila = lista.listar(enCurso()).valorOLanza().contenido().get(0);

        assertThat(fila.total()).isEqualTo(500);
        assertThat(fila.lineas()).hasSize(1);
    }

    @Test
    void un_pedido_para_llevar_no_tiene_mesa_ni_salon() {
        abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        flushYLimpiar();

        var llevar = lista.listar(filtro(null, "para_llevar", null, null, null, null, 0, 20)).valorOLanza();

        assertThat(llevar.contenido()).hasSize(1);
        var fila = llevar.contenido().get(0);
        assertThat(fila.mesaId()).isNull();
        assertThat(fila.mesaNumero()).isNull();
        assertThat(fila.salonNombre()).isNull();
        assertThat(fila.canal()).isEqualTo("para_llevar");
    }

    @Test
    void filtra_por_canal_por_estado_y_por_texto() {
        var llevar = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", 1, "Sra Rojas", ADMIN_SEMBRADO_ID)
                .valorOLanza();
        linea("Chicha", 1);
        comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        flushYLimpiar();

        assertThat(lista.listar(filtro(null, "mesa", null, null, null, null, 0, 20)).valorOLanza().contenido())
                .extracting("id").containsExactly(pedido.getId());
        assertThat(lista.listar(filtro(null, null, "abierto", null, null, null, 0, 20)).valorOLanza().contenido())
                .extracting("id").containsExactly(llevar.getId());
        assertThat(lista.listar(filtro(null, null, "comandado", null, null, null, 0, 20)).valorOLanza().contenido())
                .extracting("id").containsExactly(pedido.getId());
        // Texto: nombre del cliente (sin distinguir mayusculas), numero del pedido y numero de mesa.
        assertThat(lista.listar(filtro(null, null, null, "rojas", null, null, 0, 20)).valorOLanza().contenido())
                .extracting("id").containsExactly(llevar.getId());
        assertThat(lista.listar(filtro(null, null, null, llevar.getNumero(), null, null, 0, 20)).valorOLanza()
                .contenido()).extracting("id").contains(llevar.getId());
        assertThat(lista.listar(filtro(null, null, null, "  ", null, null, 0, 20)).valorOLanza().total()).isEqualTo(2);
        assertThat(lista.listar(filtro(null, null, null, "no-existe-nadie", null, null, 0, 20)).valorOLanza().contenido())
                .isEmpty();
    }

    @Test
    void filtra_por_fecha_operativa_con_los_dos_extremos_incluidos() {
        var ayer = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        em.flush();
        jdbc.update("update pedido set fecha_operativa = date '2031-06-10' where id = ?", ayer.getId());
        jdbc.update("update pedido set fecha_operativa = date '2031-06-12' where id = ?", pedido.getId());
        flushYLimpiar();

        assertThat(lista.listar(filtro(null, null, null, null, LocalDate.of(2031, 6, 10), LocalDate.of(2031, 6, 10), 0, 20))
                .valorOLanza().contenido()).extracting("id").containsExactly(ayer.getId());
        assertThat(lista.listar(filtro(null, null, null, null, LocalDate.of(2031, 6, 11), null, 0, 20)).valorOLanza()
                .contenido()).extracting("id").containsExactly(pedido.getId());
        assertThat(lista.listar(filtro(null, null, null, null, LocalDate.of(2031, 6, 10), LocalDate.of(2031, 6, 12), 0, 20))
                .valorOLanza().total()).isEqualTo(2);
    }

    @Test
    void ordena_del_mas_reciente_al_mas_antiguo_y_pagina_de_forma_estable() {
        var b = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        var c = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        em.flush();
        jdbc.update("update pedido set abierto_en = now() - interval '3 hours' where id = ?", pedido.getId());
        jdbc.update("update pedido set abierto_en = now() - interval '2 hours' where id = ?", b.getId());
        jdbc.update("update pedido set abierto_en = now() - interval '1 hours' where id = ?", c.getId());
        flushYLimpiar();

        var primera = lista.listar(filtro(null, null, null, null, null, null, 0, 2)).valorOLanza();
        var segunda = lista.listar(filtro(null, null, null, null, null, null, 1, 2)).valorOLanza();

        assertThat(primera.contenido()).extracting("id").containsExactly(c.getId(), b.getId());
        assertThat(segunda.contenido()).extracting("id").containsExactly(pedido.getId());
        assertThat(primera.total()).isEqualTo(3);
        assertThat(primera.totalPaginas()).isEqualTo(2);
        assertThat(primera.pagina()).isZero();
        assertThat(segunda.pagina()).isEqualTo(1);
        assertThat(primera.tamano()).isEqualTo(2);
    }

    @Test
    void los_cerrados_salen_vacios_hasta_el_sprint_7_y_sin_lineas() {
        var pagina = lista.listar(filtro("cerrados", null, null, null, null, null, 0, 20)).valorOLanza();

        assertThat(pagina.contenido()).isEmpty();
        assertThat(pagina.total()).isZero();
        assertThat(pagina.totalPaginas()).isZero();
    }

    @Test
    void un_pedido_pendiente_de_cierre_sigue_en_curso() {
        em.flush();
        jdbc.update("update pedido set estado = 'pendiente_cierre' where id = ?", pedido.getId());
        flushYLimpiar();

        assertThat(lista.listar(enCurso()).valorOLanza().contenido()).extracting("estado")
                .containsExactly("pendiente_cierre");
        assertThat(lista.resumen().valorOLanza().porCobrar()).isEqualTo(1);
    }

    @Test
    void solo_se_ven_los_pedidos_del_local_de_la_sesion() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);

        assertThat(lista.listar(enCurso()).valorOLanza().contenido()).isEmpty();
        assertThat(lista.resumen().valorOLanza().enCurso()).isZero();
    }

    @Test
    void los_filtros_invalidos_se_rechazan_y_no_dan_una_lista_vacia_enganosa() {
        assertThat(errorDeLista(lista.listar(filtro("todos", null, null, null, null, null, 0, 20))))
                .isEqualTo(ErrorPedido.FILTRO_INVALIDO);
        assertThat(errorDeLista(lista.listar(filtro(null, "delivery", null, null, null, null, 0, 20))))
                .isEqualTo(ErrorPedido.FILTRO_INVALIDO);
        assertThat(errorDeLista(lista.listar(filtro(null, null, "cerrado", null, null, null, 0, 20))))
                .isEqualTo(ErrorPedido.FILTRO_INVALIDO);
        assertThat(errorDeLista(lista.listar(filtro(null, null, "inventado", null, null, null, 0, 20))))
                .isEqualTo(ErrorPedido.FILTRO_INVALIDO);
        assertThat(errorDeLista(lista.listar(filtro(null, null, null, null, LocalDate.of(2031, 6, 12),
                LocalDate.of(2031, 6, 10), 0, 20)))).isEqualTo(ErrorPedido.FILTRO_INVALIDO);
        assertThat(errorDeLista(lista.listar(filtro(null, null, null, null, null, null, -1, 20))))
                .isEqualTo(ErrorPedido.FILTRO_INVALIDO);
        assertThat(errorDeLista(lista.listar(filtro(null, null, null, null, null, null, 0, 0))))
                .isEqualTo(ErrorPedido.FILTRO_INVALIDO);
        assertThat(errorDeLista(lista.listar(filtro(null, null, null, null, null, null, 0, 101))))
                .isEqualTo(ErrorPedido.FILTRO_INVALIDO);
    }

    @Test
    void el_resumen_cuenta_los_pedidos_en_curso_del_local() {
        abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        var resumen = lista.resumen().valorOLanza();

        assertThat(resumen.enCurso()).isEqualTo(2);
        assertThat(resumen.porCobrar()).isZero();
    }

    @Test
    void sin_local_activo_no_se_lista_nada() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);

        assertThat(errorDeLista(lista.listar(enCurso()))).isEqualTo(ErrorPedido.SIN_LOCAL_ACTIVO);
        assertThat(errorDeLista(lista.resumen())).isEqualTo(ErrorPedido.SIN_LOCAL_ACTIVO);
        assertThat(List.of()).isEmpty();
    }
}

package pe.rikos.operacion.venta.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.pensionistas.infraestructura.PreTicket;
import pe.rikos.operacion.pensionistas.infraestructura.PreTicketRepositorio;
import pe.rikos.operacion.venta.aplicacion.AbrirPedido;
import pe.rikos.operacion.venta.aplicacion.Canal;
import pe.rikos.operacion.venta.aplicacion.PedidosBase;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VincularPreTicketControllerTest extends PedidosBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    MockMvc mvc;
    @Autowired
    AbrirPedido abrirPedido;
    @Autowired
    CuentaPersonaRepositorio personas;
    @Autowired
    PreTicketRepositorio preTickets;
    @Autowired
    pe.rikos.operacion.identidad.infraestructura.seguridad.JwtServicio jwt;

    String localId;
    Pedido pedido;
    String personaId;
    PlanTarifario plan;
    Pensionista pensionista;
    String authHeader;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localId = nuevoLocal("vpc");
        plan = nuevoPlan(500, 800, 800);
        pensionista = nuevoPensionista(plan, "Gomez");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localId);
        pedido = abrirPedido.ejecutar(null, Canal.MESA, nuevaMesa("20"), "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        personaId = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(0).getId();
        authHeader = "Bearer " + jwt.generar(ADMIN_SEMBRADO_ID, "admin", java.util.List.of("Administrador del sistema"),
                java.util.List.of("venta.crear"), java.util.List.of(localId), localId);
    }

    @AfterEach
    void limpiar() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private PreTicket emitirPreTicket(String codigo) {
        PreTicket pt = new PreTicket(Identificador.nuevo().valor(), codigo, pensionista.getId(), localId, "almuerzo",
                OffsetDateTime.now().plusHours(2));
        return preTickets.save(pt);
    }

    @Test
    void vincula_pre_ticket_exitosamente_via_http() throws Exception {
        String codigo = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
        PreTicket pt = emitirPreTicket(codigo);

        mvc.perform(put("/pedidos/{id}/personas/{personaId}/pre-ticket", pedido.getId(), personaId)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("codigo", codigo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pedido.getId()))
                .andExpect(jsonPath("$.personas[0].id").value(personaId))
                .andExpect(jsonPath("$.personas[0].tipoCliente").value("pensionista"))
                .andExpect(jsonPath("$.personas[0].pensionistaId").value(pensionista.getId()))
                .andExpect(jsonPath("$.personas[0].preTicketId").value(pt.getId()));
    }

    @Test
    void rechaza_404_si_codigo_no_existe() throws Exception {
        mvc.perform(put("/pedidos/{id}/personas/{personaId}/pre-ticket", pedido.getId(), personaId)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("codigo", "000000"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRE_TICKET_NO_ENCONTRADO"));
    }

    @Test
    void rechaza_409_si_ya_esta_vinculado() throws Exception {
        String codigo = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
        PreTicket pt = emitirPreTicket(codigo);
        pt.vincular(ADMIN_SEMBRADO_ID);
        preTickets.save(pt);

        mvc.perform(put("/pedidos/{id}/personas/{personaId}/pre-ticket", pedido.getId(), personaId)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("codigo", codigo))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PRE_TICKET_YA_VINCULADO"));
    }
}

package pe.rikos.operacion.caja.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.infraestructura.*;
import pe.rikos.operacion.venta.aplicacion.PedidosBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-035 y US-036 con los roles reales: cerrar el turno es caja.crear, ver el resumen y el cierre es caja.ver, y reabrir un cobro
 * es caja.reabrir (cajero y administradores; el mozo, cocina y motorizado no).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CierreYReaperturaAccesoPorRolTest extends PedidosBase {

    private static final ObjectMapper MAPEADOR = new ObjectMapper();

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuarioRepositorio usuarios;
    @Autowired
    RolRepositorio roles;
    @Autowired
    UsuarioRolRepositorio usuarioRoles;
    @Autowired
    UsuarioLocalRepositorio usuarioLocales;
    @Autowired
    LocalRepositorio locales;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    MotivoRepositorio motivos;

    Local local;
    Producto chicha;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        try {
            local = locales.findById(nuevoLocal("ci")).orElseThrow();
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, local.getId());
        try {
            chicha = productoDeCarta("Chicha", 500L);
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
    }

    String tokenPara(String nombreRol) throws Exception {
        String login = "cier-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(18);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", nombreRol,
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals(nombreRol)).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));
        usuarioLocales.save(new UsuarioLocal(usuario, local, true, ADMIN_SEMBRADO_ID));
        String respuesta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + login + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + MAPEADOR.readTree(respuesta).get("token").asText();
    }

    String mesaNueva(String numero) {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, local.getId());
        try {
            return nuevaMesa(numero);
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
    }

    int cajas = 0;

    /** Abre un turno en una caja nueva para quien tiene este token. */
    void abrirTurno(String token) throws Exception {
        String admin = tokenPara("administrador central");
        String cajaId = MAPEADOR.readTree(mvc.perform(post("/cajas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Caja c" + (++cajas) + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post("/cajas/" + cajaId + "/turnos").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"fondoInicial\":10000}")).andExpect(status().isCreated());
    }

    /** Un pedido de mesa con una linea comandada, ya cobrada en efectivo; devuelve el id del cobro. */
    String[] pedidoCobrado(String token) throws Exception {
        JsonNode pedido = MAPEADOR.readTree(mvc.perform(post("/pedidos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"mesa\",\"mesaId\":\"" + mesaNueva("m" + (++cajas)) + "\",\"servicio\":\"almuerzo\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String id = pedido.get("id").asText();
        String personaId = pedido.get("personas").get(0).get("id").asText();
        String lineaId = MAPEADOR.readTree(mvc.perform(post("/pedidos/" + id + "/lineas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":\"" + personaId + "\",\"tipo\":\"producto\",\"productoId\":\"" + chicha.getId()
                                + "\",\"cantidad\":2}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("personas").get(0).get("lineas").get(0).get("id").asText();
        mvc.perform(post("/pedidos/" + id + "/comandas").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isCreated());
        String cobroId = MAPEADOR.readTree(mvc.perform(post("/pedidos/" + id + "/cobros").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lineaIds\":[\"" + lineaId + "\"],\"pagos\":[{\"medio\":\"efectivo\",\"monto\":1000}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        return new String[] {id, cobroId};
    }

    String motivoDeReapertura() {
        return motivos.save(new Motivo(Identificador.nuevo().valor(), "reapertura",
                "Error " + Identificador.nuevo().valor().substring(18), false)).getId();
    }

    @Test
    void el_cajero_ve_el_resumen_antes_de_contar_y_cierra_su_turno() throws Exception {
        String cajero = tokenPara("cajero");
        abrirTurno(cajero);
        pedidoCobrado(cajero);

        mvc.perform(get("/turnos/actual/resumen").header("Authorization", cajero)).andExpect(status().isOk())
                .andExpect(jsonPath("$.fondoInicial").value(10000)).andExpect(jsonPath("$.efectivoCobrado").value(1000))
                .andExpect(jsonPath("$.efectivoEsperado").value(11000)).andExpect(jsonPath("$.cobros").value(1));
        String turnoId = MAPEADOR.readTree(mvc.perform(post("/turnos/actual/cierre").header("Authorization", cajero)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"efectivoContado\":10999,\"observacion\":\"falta un centimo\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.efectivoEsperado").value(11000))
                .andExpect(jsonPath("$.diferencia").value(-1)).andExpect(jsonPath("$.alerta").value(true))
                .andReturn().getResponse().getContentAsString()).get("turnoId").asText();
        mvc.perform(get("/turnos/" + turnoId + "/cierre").header("Authorization", cajero)).andExpect(status().isOk())
                .andExpect(jsonPath("$.efectivoContado").value(10999));
        // Cerrado: ya no hay turno ni resumen, y no se cierra dos veces.
        mvc.perform(get("/turnos/actual").header("Authorization", cajero)).andExpect(status().isNoContent());
        mvc.perform(get("/turnos/actual/resumen").header("Authorization", cajero)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SIN_TURNO_ABIERTO"));
        mvc.perform(post("/turnos/actual/cierre").header("Authorization", cajero).contentType(MediaType.APPLICATION_JSON)
                .content("{\"efectivoContado\":0}")).andExpect(status().isConflict());
    }

    @Test
    void la_caja_lista_los_cobros_de_su_turno_para_reabrirlos() throws Exception {
        String cajero = tokenPara("cajero");
        abrirTurno(cajero);
        String[] cobrado = pedidoCobrado(cajero);

        mvc.perform(get("/turnos/actual/cobros").header("Authorization", cajero)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(cobrado[1]))
                .andExpect(jsonPath("$[0].pedidoId").value(cobrado[0])).andExpect(jsonPath("$[0].total").value(1000))
                .andExpect(jsonPath("$[0].vuelto").value(0)).andExpect(jsonPath("$[0].revertido").value(false));
        // Sin turno no hay lista.
        mvc.perform(get("/turnos/actual/cobros").header("Authorization", tokenPara("cajero")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("SIN_TURNO_ABIERTO"));
        mvc.perform(get("/turnos/actual/cobros").header("Authorization", tokenPara("mozo"))).andExpect(status().isForbidden());
    }

    @Test
    void un_conteo_invalido_es_422() throws Exception {
        String cajero = tokenPara("cajero");
        abrirTurno(cajero);

        mvc.perform(post("/turnos/actual/cierre").header("Authorization", cajero).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"efectivoContado\":-5}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error").value("CONTADO_INVALIDO"));
        mvc.perform(post("/turnos/actual/cierre").header("Authorization", cajero).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error").value("CONTADO_INVALIDO"));
    }

    @Test
    void el_cajero_reabre_un_cobro_dentro_de_su_turno_y_se_ve_en_la_lista_de_cobros() throws Exception {
        String cajero = tokenPara("cajero");
        abrirTurno(cajero);
        String[] cobrado = pedidoCobrado(cajero);

        mvc.perform(post("/cobros/" + cobrado[1] + "/reapertura").header("Authorization", cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivoId\":\"" + motivoDeReapertura() + "\",\"observacion\":\"cobre de mas\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.revertido").value(true));
        mvc.perform(get("/pedidos/" + cobrado[0] + "/cobros").header("Authorization", cajero)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revertido").value(true)).andExpect(jsonPath("$[0].lineaIds.length()").value(1));
        mvc.perform(get("/pedidos/" + cobrado[0]).header("Authorization", cajero)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("comandado"))
                .andExpect(jsonPath("$.personas[0].lineas[0].estado").value("comandado"));
        mvc.perform(get("/turnos/actual/resumen").header("Authorization", cajero))
                .andExpect(jsonPath("$.efectivoEsperado").value(10000)).andExpect(jsonPath("$.cobros").value(0));
        // Reabierto una vez, no se reabre otra.
        mvc.perform(post("/cobros/" + cobrado[1] + "/reapertura").header("Authorization", cajero)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivoId\":\"" + motivoDeReapertura() + "\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("COBRO_YA_REVERTIDO"));
    }

    @Test
    void con_el_turno_cerrado_la_reapertura_es_409() throws Exception {
        String cajero = tokenPara("cajero");
        abrirTurno(cajero);
        String[] cobrado = pedidoCobrado(cajero);
        mvc.perform(post("/turnos/actual/cierre").header("Authorization", cajero).contentType(MediaType.APPLICATION_JSON)
                .content("{\"efectivoContado\":11000}")).andExpect(status().isCreated());

        mvc.perform(post("/cobros/" + cobrado[1] + "/reapertura").header("Authorization", cajero)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivoId\":\"" + motivoDeReapertura() + "\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("TURNO_CERRADO"));
    }

    @Test
    void mozo_cocina_y_motorizado_no_cierran_ni_reabren_ni_ven_el_resumen() throws Exception {
        String cajero = tokenPara("cajero");
        abrirTurno(cajero);
        String[] cobrado = pedidoCobrado(cajero);
        for (String rol : new String[] {"mozo", "cocina", "motorizado"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/turnos/actual/resumen").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(post("/turnos/actual/cierre").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"efectivoContado\":0}")).andExpect(status().isForbidden());
            mvc.perform(post("/cobros/" + cobrado[1] + "/reapertura").header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
            mvc.perform(get("/pedidos/" + cobrado[0] + "/cobros").header("Authorization", token)).andExpect(status().isForbidden());
        }
    }
}

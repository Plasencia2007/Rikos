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
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.infraestructura.*;
import pe.rikos.operacion.venta.aplicacion.PedidosBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-033 con los roles reales: cobrar es caja.crear (cajero y administradores). El mozo pide pero no cobra, y cocina y
 * motorizado tampoco. El cobro exige un turno abierto de quien cobra y deja la mesa libre al cobrar la ultima linea.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CobroAccesoPorRolTest extends PedidosBase {

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

    Local local;
    Producto chicha;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        try {
            local = locales.findById(nuevoLocal("co")).orElseThrow();
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
        String login = "cobro-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(18);
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

    /** Un pedido de mesa con una linea ya comandada; devuelve {pedidoId, lineaId}. */
    String[] pedidoComandado(String token, String mesaId) throws Exception {
        JsonNode pedido = MAPEADOR.readTree(mvc.perform(post("/pedidos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"mesa\",\"mesaId\":\"" + mesaId + "\",\"servicio\":\"almuerzo\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String id = pedido.get("id").asText();
        String personaId = pedido.get("personas").get(0).get("id").asText();
        JsonNode conLinea = MAPEADOR.readTree(mvc.perform(post("/pedidos/" + id + "/lineas")
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":\"" + personaId + "\",\"tipo\":\"producto\",\"productoId\":\""
                                + chicha.getId() + "\",\"cantidad\":2}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/pedidos/" + id + "/comandas").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isCreated());
        return new String[] {id, conLinea.get("personas").get(0).get("lineas").get(0).get("id").asText()};
    }

    String cuerpo(String lineaId, long recibido) {
        return "{\"lineaIds\":[\"" + lineaId + "\"],\"pagos\":[{\"medio\":\"efectivo\",\"monto\":" + recibido + "}]}";
    }

    int cajasCreadas = 0;

    /** Abre un turno en una caja NUEVA (cada prueba que abre varios turnos usa su propia caja: una caja, un turno). */
    String abrirTurno(String token) throws Exception {
        String admin = tokenPara("administrador central");
        String cajaId = MAPEADOR.readTree(mvc.perform(post("/cajas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Caja t" + (++cajasCreadas) + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post("/cajas/" + cajaId + "/turnos").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"fondoInicial\":10000}")).andExpect(status().isCreated());
        return cajaId;
    }

    @Test
    void el_cajero_cobra_con_vuelto_y_la_mesa_queda_libre() throws Exception {
        String cajero = tokenPara("cajero");
        String mesa = mesaNueva("1");
        String[] pedido = pedidoComandado(cajero, mesa);
        abrirTurno(cajero);

        mvc.perform(post("/pedidos/" + pedido[0] + "/cobros").header("Authorization", cajero)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(pedido[1], 2000)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(1000))
                .andExpect(jsonPath("$.totalMedios").value(1000)).andExpect(jsonPath("$.vuelto").value(1000))
                .andExpect(jsonPath("$.pedidoCerrado").value(true))
                .andExpect(jsonPath("$.pagos[0].medio").value("efectivo"))
                .andExpect(jsonPath("$.pagos[0].importe").value(1000))
                .andExpect(jsonPath("$.pagos[0].montoRecibido").value(2000));
        mvc.perform(get("/pedidos/" + pedido[0]).header("Authorization", cajero)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("cerrado"))
                .andExpect(jsonPath("$.personas[0].lineas[0].estado").value("cobrado"));
        mvc.perform(get("/mesas").header("Authorization", cajero)).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + mesa + "')].estado").value("libre"));
    }

    @Test
    void los_administradores_tambien_cobran() throws Exception {
        int n = 1;
        for (String rol : new String[] {"administrador de local", "administrador central"}) {
            String token = tokenPara(rol);
            String[] pedido = pedidoComandado(token, mesaNueva("a" + n++));
            abrirTurno(token);
            mvc.perform(post("/pedidos/" + pedido[0] + "/cobros").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo(pedido[1], 1000)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void mozo_cocina_y_motorizado_no_cobran() throws Exception {
        String admin = tokenPara("administrador de local");
        String[] pedido = pedidoComandado(admin, mesaNueva("1"));
        abrirTurno(admin);
        for (String rol : new String[] {"mozo", "cocina", "motorizado"}) {
            mvc.perform(post("/pedidos/" + pedido[0] + "/cobros").header("Authorization", tokenPara(rol))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo(pedido[1], 1000)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void sin_turno_abierto_el_cobro_es_409() throws Exception {
        String cajero = tokenPara("cajero");
        String[] pedido = pedidoComandado(cajero, mesaNueva("1"));

        mvc.perform(post("/pedidos/" + pedido[0] + "/cobros").header("Authorization", cajero)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(pedido[1], 1000)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("SIN_TURNO_ABIERTO"));
    }

    @Test
    void errores_de_pago_y_de_linea_tienen_su_codigo() throws Exception {
        String cajero = tokenPara("cajero");
        String[] pedido = pedidoComandado(cajero, mesaNueva("1"));
        abrirTurno(cajero);

        mvc.perform(post("/pedidos/" + pedido[0] + "/cobros").header("Authorization", cajero)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(pedido[1], 500)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error").value("PAGO_INSUFICIENTE"));
        mvc.perform(post("/pedidos/" + pedido[0] + "/cobros").header("Authorization", cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lineaIds\":[\"" + Identificador.nuevo().valor() + "\"],\"pagos\":[]}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("LINEA_NO_ENCONTRADA"));
        mvc.perform(post("/pedidos/" + Identificador.nuevo().valor() + "/cobros").header("Authorization", cajero)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(pedido[1], 1000)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("PEDIDO_NO_ENCONTRADO"));
        // Cobrada la linea, un segundo cobro de la misma es 409 y el pedido cerrado tambien.
        mvc.perform(post("/pedidos/" + pedido[0] + "/cobros").header("Authorization", cajero)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(pedido[1], 1000))).andExpect(status().isCreated());
        mvc.perform(post("/pedidos/" + pedido[0] + "/cobros").header("Authorization", cajero)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(pedido[1], 1000)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("PEDIDO_CERRADO"));
    }

    @Test
    void terminar_un_pedido_con_lineas_comandadas_es_409() throws Exception {
        String admin = tokenPara("administrador de local");
        String[] pedido = pedidoComandado(admin, mesaNueva("1"));

        mvc.perform(post("/pedidos/" + pedido[0] + "/terminar").header("Authorization", admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("PEDIDO_CON_LINEAS_POR_COBRAR"));
    }

    @Autowired
    pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio motivos;

    String motivoDeDescuento() {
        return motivos.save(new pe.rikos.operacion.catalogo.infraestructura.Motivo(Identificador.nuevo().valor(), "descuento",
                "Cortesia " + Identificador.nuevo().valor().substring(18), false)).getId();
    }

    String cuerpoConDescuento(String lineaId, long recibido, String motivoId) {
        return "{\"lineaIds\":[\"" + lineaId + "\"],\"pagos\":[{\"medio\":\"efectivo\",\"monto\":" + recibido
                + "}],\"descuentos\":[{\"lineaId\":\"" + lineaId + "\",\"tipo\":\"monto\",\"valor\":200,\"motivoId\":\""
                + motivoId + "\"}]}";
    }

    @Test
    void descontar_exige_su_propio_permiso_y_los_administradores_lo_tienen() throws Exception {
        String motivo = motivoDeDescuento();
        String cajero = tokenPara("cajero");
        String[] delCajero = pedidoComandado(cajero, mesaNueva("1"));
        abrirTurno(cajero);

        // El cajero cobra (caja.crear) pero no descuenta (caja.descontar es de los administradores).
        mvc.perform(post("/pedidos/" + delCajero[0] + "/cobros").header("Authorization", cajero)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoConDescuento(delCajero[1], 1000, motivo)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("SIN_PERMISO_PARA_DESCONTAR"));

        String admin = tokenPara("administrador de local");
        String[] delAdmin = pedidoComandado(admin, mesaNueva("2"));
        abrirTurno(admin);
        mvc.perform(post("/pedidos/" + delAdmin[0] + "/cobros").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoConDescuento(delAdmin[1], 1000, motivo)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(800))
                .andExpect(jsonPath("$.vuelto").value(200));
    }

    @Test
    void un_pago_nulo_es_422_y_no_un_500() throws Exception {
        String cajero = tokenPara("cajero");
        String[] pedido = pedidoComandado(cajero, mesaNueva("1"));
        abrirTurno(cajero);

        mvc.perform(post("/pedidos/" + pedido[0] + "/cobros").header("Authorization", cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lineaIds\":[\"" + pedido[1] + "\"],\"pagos\":[null]}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error").value("MEDIO_INVALIDO"));
    }

    @Test
    void sin_sesion_no_cobra() throws Exception {
        mvc.perform(post("/pedidos/x/cobros").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}

package pe.rikos.operacion.venta.api;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-029 con los roles reales. Reorganizar (pasar productos, mover el pedido, unir y separar mesas) es
 * venta.modificar: mozo, cajero y administradores si; cocina y motorizado no. Otro local es 404 en cada verbo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReorganizarAccesoPorRolTest extends PedidosBase {

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
        local = nuevoLocalDeAcceso("ra");
        chicha = productoDeCarta("Chicha", 500L);
    }

    Local nuevoLocalDeAcceso(String prefijo) {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        try {
            return locales.findById(nuevoLocal(prefijo)).orElseThrow();
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
    }

    String mesaNueva(Local delLocal, String numero) {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, delLocal.getId());
        try {
            return nuevaMesa(numero);
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
    }

    String tokenPara(String nombreRol, Local delLocal) throws Exception {
        String login = "reorol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(18);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", nombreRol,
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals(nombreRol)).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));
        usuarioLocales.save(new UsuarioLocal(usuario, delLocal, true, ADMIN_SEMBRADO_ID));
        String respuesta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + login + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + MAPEADOR.readTree(respuesta).get("token").asText();
    }

    String tokenPara(String nombreRol) throws Exception {
        return tokenPara(nombreRol, local);
    }

    /** Un pedido de mesa con una linea; devuelve {pedidoId, lineaId, personaId}. */
    String[] pedidoConLinea(String token, String mesaId) throws Exception {
        JsonNode pedido = MAPEADOR.readTree(mvc.perform(post("/pedidos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"mesa\",\"mesaId\":\"" + mesaId + "\",\"servicio\":\"almuerzo\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String id = pedido.get("id").asText();
        String personaId = pedido.get("personas").get(0).get("id").asText();
        JsonNode conLinea = MAPEADOR.readTree(mvc.perform(post("/pedidos/" + id + "/lineas")
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":\"" + personaId + "\",\"tipo\":\"producto\",\"productoId\":\""
                                + chicha.getId() + "\",\"cantidad\":1}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        return new String[] {id, conLinea.get("personas").get(0).get("lineas").get(0).get("id").asText(), personaId};
    }

    String cuerpoTraslado(String lineaId) {
        return "{\"lineaIds\":[\"" + lineaId + "\"]}";
    }

    @Test
    void mozo_cajero_y_administradores_reorganizan() throws Exception {
        int n = 1;
        for (String rol : new String[] {"mozo", "cajero", "administrador de local", "administrador central"}) {
            String token = tokenPara(rol);
            String mesa = mesaNueva(local, "a" + n);
            String destino = mesaNueva(local, "b" + n);
            String secundaria = mesaNueva(local, "c" + n++);
            String[] pedido = pedidoConLinea(token, mesa);

            // Dividir la cuenta: a una persona nueva.
            mvc.perform(post("/pedidos/" + pedido[0] + "/lineas/trasladar").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoTraslado(pedido[1])))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.personas.length()").value(2))
                    .andExpect(jsonPath("$.personas[1].lineas[0].id").value(pedido[1]))
                    .andExpect(jsonPath("$.total").value(500));
            // Mover el pedido a otra mesa libre.
            mvc.perform(put("/pedidos/" + pedido[0] + "/mesa").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"mesaId\":\"" + destino + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.mesaId").value(destino));
            // Unir una mesa libre a la del pedido, verlo desde la secundaria y separarla.
            mvc.perform(post("/mesas/" + secundaria + "/unir").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"principalId\":\"" + destino + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.mesaPrincipalId").value(destino))
                    .andExpect(jsonPath("$.estado").value("unida"));
            mvc.perform(get("/mesas/" + secundaria + "/pedido").header("Authorization", token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(pedido[0]));
            mvc.perform(post("/mesas/" + secundaria + "/separar").header("Authorization", token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("libre"));
        }
    }

    @Test
    void cocina_y_motorizado_no_reorganizan() throws Exception {
        String admin = tokenPara("administrador de local");
        String mesa = mesaNueva(local, "1");
        String otra = mesaNueva(local, "2");
        String[] pedido = pedidoConLinea(admin, mesa);

        for (String rol : new String[] {"cocina", "motorizado"}) {
            String token = tokenPara(rol);
            mvc.perform(post("/pedidos/" + pedido[0] + "/lineas/trasladar").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoTraslado(pedido[1])))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/pedidos/" + pedido[0] + "/mesa").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"mesaId\":\"" + otra + "\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/mesas/" + otra + "/unir").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"principalId\":\"" + mesa + "\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/mesas/" + otra + "/separar").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void un_pedido_o_una_mesa_de_otro_local_da_404_en_cada_verbo() throws Exception {
        String admin = tokenPara("administrador de local");
        String mesa = mesaNueva(local, "1");
        String libre = mesaNueva(local, "2");
        String[] pedido = pedidoConLinea(admin, mesa);
        String deOtro = tokenPara("administrador de local", nuevoLocalDeAcceso("ro"));

        mvc.perform(post("/pedidos/" + pedido[0] + "/lineas/trasladar").header("Authorization", deOtro)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoTraslado(pedido[1])))
                .andExpect(status().isNotFound());
        mvc.perform(put("/pedidos/" + pedido[0] + "/mesa").header("Authorization", deOtro)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mesaId\":\"" + libre + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/mesas/" + libre + "/unir").header("Authorization", deOtro)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"principalId\":\"" + mesa + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/mesas/" + libre + "/separar").header("Authorization", deOtro))
                .andExpect(status().isNotFound());
    }

    @Test
    void los_errores_dan_codigos_claros() throws Exception {
        String token = tokenPara("mozo");
        String mesa = mesaNueva(local, "1");
        String ocupada = mesaNueva(local, "2");
        String libre = mesaNueva(local, "3");
        String[] pedido = pedidoConLinea(token, mesa);
        String[] deLaOcupada = pedidoConLinea(token, ocupada);

        mvc.perform(post("/pedidos/" + pedido[0] + "/lineas/trasladar").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lineaIds\":[]}"))
                .andExpect(status().is(422)).andExpect(jsonPath("$.error").value("SIN_LINEAS_PARA_MOVER"));
        mvc.perform(post("/pedidos/" + pedido[0] + "/lineas/trasladar").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lineaIds\":[\"" + pedido[1] + "\"],\"personaDestinoId\":\"" + pedido[2] + "\"}"))
                .andExpect(status().is(422)).andExpect(jsonPath("$.error").value("MISMO_DESTINO"));
        mvc.perform(put("/pedidos/" + pedido[0] + "/mesa").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mesaId\":\"" + ocupada + "\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("MESA_OCUPADA"));
        mvc.perform(post("/mesas/" + libre + "/unir").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"principalId\":\"" + libre + "\"}"))
                .andExpect(status().is(422)).andExpect(jsonPath("$.error").value("PRINCIPAL_INVALIDA"));
        mvc.perform(post("/mesas/" + libre + "/separar").header("Authorization", token))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("NO_ESTA_UNIDA"));
        // Una mesa con un pedido abierto sin comandar no se une como secundaria: primero se termina el pedido.
        mvc.perform(post("/mesas/" + ocupada + "/unir").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"principalId\":\"" + libre + "\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("TIENE_PEDIDO_ABIERTO"));
        // Ya comandado, la mesa esta ocupada.
        mvc.perform(post("/pedidos/" + deLaOcupada[0] + "/comandas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isCreated());
        mvc.perform(post("/mesas/" + ocupada + "/unir").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"principalId\":\"" + libre + "\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("MESA_NO_LIBRE"));
    }

    @Test
    void terminar_un_pedido_sin_cobrar_lo_pasa_al_historial_y_libera_la_mesa() throws Exception {
        int n = 1;
        for (String rol : new String[] {"mozo", "cajero", "administrador de local", "administrador central"}) {
            String token = tokenPara(rol);
            String mesa = mesaNueva(local, "t" + n++);
            String[] pedido = pedidoConLinea(token, mesa);

            mvc.perform(post("/pedidos/" + pedido[0] + "/terminar").header("Authorization", token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("cerrado"));
            // La mesa queda libre y el pedido ya no es de los "en curso".
            mvc.perform(get("/mesas").header("Authorization", token)).andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id==\"" + mesa + "\")].estado").value("libre"));
            // Un pedido cerrado no se modifica.
            mvc.perform(post("/pedidos/" + pedido[0] + "/personas").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("PEDIDO_CERRADO"));
            // Terminarlo otra vez no se puede.
            mvc.perform(post("/pedidos/" + pedido[0] + "/terminar").header("Authorization", token))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("PEDIDO_NO_TERMINABLE"));
        }
        // Todos aparecen en el historial.
        mvc.perform(get("/pedidos?situacion=cerrados").header("Authorization", tokenPara("cajero")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.contenido[0].estado").value("cerrado"));
    }

    @Test
    void cocina_y_motorizado_no_terminan_y_otro_local_da_404() throws Exception {
        String admin = tokenPara("administrador de local");
        String[] pedido = pedidoConLinea(admin, mesaNueva(local, "1"));

        for (String rol : new String[] {"cocina", "motorizado"}) {
            mvc.perform(post("/pedidos/" + pedido[0] + "/terminar").header("Authorization", tokenPara(rol)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/pedidos/" + pedido[0] + "/terminar")
                        .header("Authorization", tokenPara("administrador de local", nuevoLocalDeAcceso("rt"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void el_mapa_marca_tomando_pedido_en_la_mesa_con_pedido_abierto_y_ocupada_solo_al_comandar() throws Exception {
        String token = tokenPara("mozo");
        String mesa = mesaNueva(local, "1");
        String[] pedido = pedidoConLinea(token, mesa);

        mvc.perform(get("/mesas").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==\"" + mesa + "\")].estado").value("libre"))
                .andExpect(jsonPath("$[?(@.id==\"" + mesa + "\")].tomandoPedido").value(true));
        // Abrirla otra vez entra al mismo pedido (200).
        mvc.perform(post("/pedidos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"mesa\",\"mesaId\":\"" + mesa + "\",\"servicio\":\"almuerzo\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(pedido[0]));
        mvc.perform(post("/pedidos/" + pedido[0] + "/comandas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isCreated());
        mvc.perform(get("/mesas").header("Authorization", token))
                .andExpect(jsonPath("$[?(@.id==\"" + mesa + "\")].estado").value("ocupada"))
                .andExpect(jsonPath("$[?(@.id==\"" + mesa + "\")].tomandoPedido").value(false));
    }
}

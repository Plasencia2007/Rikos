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
import pe.rikos.operacion.catalogo.aplicacion.MarcarAgotado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.infraestructura.*;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.venta.aplicacion.PedidosBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-028 con los roles reales. Mozo, cajero y administradores toman y corrigen pedidos (venta.crear); cocina y
 * motorizado los ven (venta.ver) pero no los tocan. Un pedido de otro local es 404 en cada verbo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PedidosAccesoPorRolTest extends PedidosBase {

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
    MarcarAgotado marcarAgotado;

    Local local;
    Producto chicha;

    @BeforeEach
    void preparar() {
        local = nuevoLocalDeAcceso("pa");
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

    String tokenPara(String nombreRol, Local delLocal) throws Exception {
        String login = "pedrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(18);
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

    JsonNode abrirParaLlevar(String token) throws Exception {
        String cuerpo = mvc.perform(post("/pedidos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"para_llevar\",\"servicio\":\"almuerzo\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return MAPEADOR.readTree(cuerpo);
    }

    String lineaDeCarta(String personaId, int cantidad) {
        return "{\"personaId\":\"" + personaId + "\",\"tipo\":\"producto\",\"productoId\":\"" + chicha.getId()
                + "\",\"cantidad\":" + cantidad + "}";
    }

    @Test
    void mozo_cajero_y_administradores_toman_pedidos_y_ven_el_total() throws Exception {
        for (String rol : new String[] {"mozo", "cajero", "administrador de local", "administrador central"}) {
            String token = tokenPara(rol);
            JsonNode pedido = abrirParaLlevar(token);
            String id = pedido.get("id").asText();
            String personaId = pedido.get("personas").get(0).get("id").asText();

            mvc.perform(post("/pedidos/" + id + "/lineas").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(lineaDeCarta(personaId, 2)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.total").value(1000))
                    .andExpect(jsonPath("$.personas[0].lineas[0].precioUnitario").value(500));
            mvc.perform(get("/pedidos/" + id).header("Authorization", token)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("abierto"));
        }
    }

    @Test
    void cocina_y_motorizado_ven_los_pedidos_pero_no_los_tocan() throws Exception {
        String admin = tokenPara("administrador de local");
        JsonNode pedido = abrirParaLlevar(admin);
        String id = pedido.get("id").asText();
        String personaId = pedido.get("personas").get(0).get("id").asText();
        JsonNode conLinea = MAPEADOR.readTree(mvc.perform(post("/pedidos/" + id + "/lineas")
                        .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(lineaDeCarta(personaId, 1))).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString());
        String lineaId = conLinea.get("personas").get(0).get("lineas").get(0).get("id").asText();

        for (String rol : new String[] {"cocina", "motorizado"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/pedidos/" + id).header("Authorization", token)).andExpect(status().isOk());
            mvc.perform(get("/pedidos/" + id + "/oferta").header("Authorization", token)).andExpect(status().isOk());
            mvc.perform(post("/pedidos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"canal\":\"para_llevar\",\"servicio\":\"almuerzo\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/pedidos/" + id + "/lineas").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(lineaDeCarta(personaId, 1)))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/pedidos/" + id + "/lineas/" + lineaId).header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":2}"))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/pedidos/" + id + "/lineas/" + lineaId).header("Authorization", token))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/pedidos/" + id + "/personas").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void un_pedido_de_otro_local_da_404_en_cada_verbo() throws Exception {
        JsonNode pedido = abrirParaLlevar(tokenPara("administrador de local"));
        String id = pedido.get("id").asText();
        String personaId = pedido.get("personas").get(0).get("id").asText();
        Local otro = nuevoLocalDeAcceso("po");
        String deOtro = tokenPara("administrador de local", otro);

        mvc.perform(get("/pedidos/" + id).header("Authorization", deOtro)).andExpect(status().isNotFound());
        mvc.perform(get("/pedidos/" + id + "/oferta").header("Authorization", deOtro)).andExpect(status().isNotFound());
        mvc.perform(post("/pedidos/" + id + "/lineas").header("Authorization", deOtro)
                        .contentType(MediaType.APPLICATION_JSON).content(lineaDeCarta(personaId, 1)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/pedidos/" + id + "/personas").header("Authorization", deOtro)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/pedidos/" + id + "/lineas/x").header("Authorization", deOtro)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":1}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/pedidos/" + id + "/lineas/x").header("Authorization", deOtro))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/pedidos/" + id + "/personas/" + personaId).header("Authorization", deOtro))
                .andExpect(status().isNotFound());
        mvc.perform(put("/pedidos/" + id + "/personas/" + personaId + "/pensionista")
                        .header("Authorization", deOtro).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pensionistaId\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void los_errores_dan_codigos_claros() throws Exception {
        String token = tokenPara("mozo");
        JsonNode pedido = abrirParaLlevar(token);
        String id = pedido.get("id").asText();
        String personaId = pedido.get("personas").get(0).get("id").asText();
        Producto sinPrecio = productoDeCarta("Sin precio", null);

        mvc.perform(post("/pedidos/" + id + "/lineas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(lineaDeCarta(personaId, 0)))
                .andExpect(status().is(422)).andExpect(jsonPath("$.error").value("CANTIDAD_INVALIDA"));
        mvc.perform(post("/pedidos/" + id + "/lineas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":\"" + personaId + "\",\"tipo\":\"producto\",\"productoId\":\""
                                + sinPrecio.getId() + "\",\"cantidad\":1}"))
                .andExpect(status().is(422)).andExpect(jsonPath("$.error").value("SIN_PRECIO"));
        mvc.perform(post("/pedidos/" + id + "/lineas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":\"" + personaId + "\",\"tipo\":\"extra_libre\",\"cantidad\":1,"
                                + "\"descripcion\":\"Huevo\"}"))
                .andExpect(status().is(422)).andExpect(jsonPath("$.error").value("PRECIO_INVALIDO"));
        mvc.perform(post("/pedidos/" + id + "/lineas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":\"" + personaId + "\",\"tipo\":\"inventado\",\"cantidad\":1}"))
                .andExpect(status().is(422)).andExpect(jsonPath("$.error").value("TIPO_INVALIDO"));
        mvc.perform(post("/pedidos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"volando\",\"servicio\":\"almuerzo\"}"))
                .andExpect(status().is(422)).andExpect(jsonPath("$.error").value("CANAL_INVALIDO"));
    }

    @Test
    void un_producto_agotado_y_una_mesa_ocupada_dan_409() throws Exception {
        String admin = tokenPara("administrador de local");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, local.getId());
        String mesa = nuevaMesa("1");
        marcarAgotado.ejecutar(chicha.getId()).valorOLanza();
        ContextoDeSeguridadDePrueba.limpiar();

        String cuerpo = "{\"canal\":\"mesa\",\"mesaId\":\"" + mesa + "\",\"servicio\":\"almuerzo\"}";
        JsonNode pedido = MAPEADOR.readTree(mvc.perform(post("/pedidos").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        // Abierto sin comandar no ocupa la mesa: abrir otra vez devuelve ESE pedido (200, el mismo id).
        mvc.perform(post("/pedidos").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(pedido.get("id").asText()));
        // Ya comandado, si esta ocupada (409).
        Producto gaseosa = productoDeCarta("Gaseosa", 400L);
        mvc.perform(post("/pedidos/" + pedido.get("id").asText() + "/lineas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":\"" + pedido.get("personas").get(0).get("id").asText()
                                + "\",\"tipo\":\"producto\",\"productoId\":\"" + gaseosa.getId() + "\",\"cantidad\":1}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/pedidos/" + pedido.get("id").asText() + "/comandas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isCreated());
        mvc.perform(post("/pedidos").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("MESA_OCUPADA"));
        mvc.perform(post("/pedidos/" + pedido.get("id").asText() + "/lineas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lineaDeCarta(pedido.get("personas").get(0).get("id").asText(), 1)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("PRODUCTO_AGOTADO"));
        // El pedido de la mesa se encuentra por la mesa.
        mvc.perform(get("/mesas/" + mesa + "/pedido").header("Authorization", admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pedido.get("id").asText()));
    }

    @Test
    void con_un_pensionista_el_pedido_no_lleva_credenciales() throws Exception {
        String token = tokenPara("mozo");
        JsonNode pedido = abrirParaLlevar(token);
        String id = pedido.get("id").asText();
        String personaId = pedido.get("personas").get(0).get("id").asText();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        Pensionista luis = nuevoPensionista(nuevoPlan(500, 800, 800), "Hache");
        ContextoDeSeguridadDePrueba.limpiar();

        String cuerpo = mvc.perform(put("/pedidos/" + id + "/personas/" + personaId + "/pensionista")
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pensionistaId\":\"" + luis.getId() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.personas[0].tipoCliente").value("pensionista"))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo.toLowerCase()).doesNotContain("\"pin").doesNotContain("hash").doesNotContain("contrasena");
    }
}

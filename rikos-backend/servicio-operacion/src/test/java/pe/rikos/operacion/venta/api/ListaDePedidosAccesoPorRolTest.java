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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 6b con los roles reales: la lista de pedidos y sus conteos los ve quien tiene venta.ver (mozo, cajero,
 * administradores, y tambien cocina y motorizado); sin sesion no entra nadie. Un pedido de otro local no aparece.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ListaDePedidosAccesoPorRolTest extends PedidosBase {

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
        local = nuevoLocalDeAcceso("lp");
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
        String login = "lisrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(18);
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

    /** Un pedido para llevar con una linea de Chicha x2; devuelve su id. */
    String pedidoConLinea(String token) throws Exception {
        JsonNode pedido = MAPEADOR.readTree(mvc.perform(post("/pedidos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"para_llevar\",\"servicio\":\"almuerzo\",\"nombreCliente\":\"Sra Rojas\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String id = pedido.get("id").asText();
        mvc.perform(post("/pedidos/" + id + "/lineas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":\"" + pedido.get("personas").get(0).get("id").asText()
                                + "\",\"tipo\":\"producto\",\"productoId\":\"" + chicha.getId() + "\",\"cantidad\":2}"))
                .andExpect(status().isCreated());
        return id;
    }

    @Test
    void todos_los_roles_de_sala_ven_la_lista_y_los_conteos() throws Exception {
        String admin = tokenPara("administrador de local");
        String id = pedidoConLinea(admin);

        for (String rol : new String[] {"mozo", "cajero", "administrador de local", "administrador central", "cocina",
                "motorizado"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/pedidos").header("Authorization", token)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.pagina").value(0))
                    .andExpect(jsonPath("$.tamano").value(20)).andExpect(jsonPath("$.totalPaginas").value(1))
                    .andExpect(jsonPath("$.contenido[0].id").value(id))
                    .andExpect(jsonPath("$.contenido[0].canal").value("para_llevar"))
                    .andExpect(jsonPath("$.contenido[0].nombreCliente").value("Sra Rojas"))
                    .andExpect(jsonPath("$.contenido[0].comensales").value(1))
                    .andExpect(jsonPath("$.contenido[0].total").value(1000))
                    .andExpect(jsonPath("$.contenido[0].mozoNombre").isNotEmpty())
                    .andExpect(jsonPath("$.contenido[0].abiertoEn").isNotEmpty())
                    .andExpect(jsonPath("$.contenido[0].mesaNumero").doesNotExist())
                    .andExpect(jsonPath("$.contenido[0].lineas[0].nombre").value(chicha.getNombre()))
                    .andExpect(jsonPath("$.contenido[0].lineas[0].cantidad").value(2))
                    .andExpect(jsonPath("$.contenido[0].lineas[0].estado").value("pendiente"));
            mvc.perform(get("/pedidos/resumen").header("Authorization", token)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.enCurso").value(1)).andExpect(jsonPath("$.porCobrar").value(0));
        }
    }

    @Test
    void el_pedido_trae_el_nombre_del_mozo_y_la_hora_de_apertura() throws Exception {
        String id = pedidoConLinea(tokenPara("mozo"));

        mvc.perform(get("/pedidos/" + id).header("Authorization", tokenPara("cajero"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.mozoNombre").isNotEmpty()).andExpect(jsonPath("$.abiertoEn").isNotEmpty());
    }

    @Test
    void sin_sesion_no_se_ve_nada() throws Exception {
        mvc.perform(get("/pedidos")).andExpect(status().is4xxClientError());
        mvc.perform(get("/pedidos/resumen")).andExpect(status().is4xxClientError());
    }

    @Test
    void los_pedidos_de_otro_local_no_aparecen() throws Exception {
        pedidoConLinea(tokenPara("administrador de local"));
        String deOtro = tokenPara("administrador de local", nuevoLocalDeAcceso("lo"));

        mvc.perform(get("/pedidos").header("Authorization", deOtro)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.contenido.length()").value(0));
        mvc.perform(get("/pedidos/resumen").header("Authorization", deOtro)).andExpect(jsonPath("$.enCurso").value(0));
    }

    @Test
    void los_filtros_y_los_errores_responden_con_codigos_claros() throws Exception {
        String admin = tokenPara("administrador de local");
        pedidoConLinea(admin);

        mvc.perform(get("/pedidos?canal=mesa").header("Authorization", admin)).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/pedidos?q=rojas").header("Authorization", admin)).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/pedidos?situacion=cerrados").header("Authorization", admin))
                .andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.totalPaginas").value(0));
        mvc.perform(get("/pedidos?situacion=todos").header("Authorization", admin)).andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("FILTRO_INVALIDO"));
        mvc.perform(get("/pedidos?tamano=500").header("Authorization", admin)).andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("FILTRO_INVALIDO"));
        mvc.perform(get("/pedidos?desde=2031-06-12&hasta=2031-06-10").header("Authorization", admin))
                .andExpect(status().is(422)).andExpect(jsonPath("$.error").value("FILTRO_INVALIDO"));
    }
}

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
 * US-030 y US-031 con los roles reales. Mozo, cajero y administradores comandan e imprimen (venta.crear); anular
 * exige venta.anular (cajero y administradores, NO el mozo); cocina y motorizado solo ven las comandas. Un pedido o
 * una comanda de otro local es 404 en cada verbo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ComandasYAnulacionesAccesoPorRolTest extends PedidosBase {

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
    Motivo motivo;

    @BeforeEach
    void preparar() {
        local = nuevoLocalDeAcceso("ca");
        chicha = productoDeCarta("Chicha", 500L);
        motivo = motivos.save(new Motivo(Identificador.nuevo().valor(), "anulacion", "Error del mozo " + pe.rikos.contratos.Identificador.nuevo().valor().substring(18), false));
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
        String login = "comrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(18);
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

    /** Un pedido para llevar con una linea; devuelve {pedidoId, lineaId}. */
    String[] pedidoConLinea(String token) throws Exception {
        JsonNode pedido = MAPEADOR.readTree(mvc.perform(post("/pedidos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"para_llevar\",\"servicio\":\"almuerzo\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String id = pedido.get("id").asText();
        String personaId = pedido.get("personas").get(0).get("id").asText();
        JsonNode conLinea = MAPEADOR.readTree(mvc.perform(post("/pedidos/" + id + "/lineas")
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":\"" + personaId + "\",\"tipo\":\"producto\",\"productoId\":\""
                                + chicha.getId() + "\",\"cantidad\":1}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        return new String[] {id, conLinea.get("personas").get(0).get("lineas").get(0).get("id").asText()};
    }

    String comandar(String token, String pedidoId) throws Exception {
        String cuerpo = mvc.perform(post("/pedidos/" + pedidoId + "/comandas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return MAPEADOR.readTree(cuerpo).get("id").asText();
    }

    String cuerpoDeAnulacion() {
        return "{\"motivoId\":\"" + motivo.getId() + "\"}";
    }

    @Test
    void mozo_cajero_y_administradores_comandan_e_imprimen_con_copia_desde_la_segunda() throws Exception {
        for (String rol : new String[] {"mozo", "cajero", "administrador de local", "administrador central"}) {
            String token = tokenPara(rol);
            String[] pedido = pedidoConLinea(token);
            String comandaId = comandar(token, pedido[0]);

            mvc.perform(get("/comandas/" + comandaId).header("Authorization", token)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.numero").value(1)).andExpect(jsonPath("$.canal").value("para_llevar"))
                    .andExpect(jsonPath("$.personas[0].lineas[0].cantidad").value(1));
            mvc.perform(get("/pedidos/" + pedido[0] + "/comandas").header("Authorization", token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(comandaId));
            mvc.perform(post("/comandas/" + comandaId + "/impresiones").header("Authorization", token))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.copia").value(false))
                    .andExpect(jsonPath("$.impresiones").value(1));
            mvc.perform(post("/comandas/" + comandaId + "/impresiones").header("Authorization", token))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.copia").value(true))
                    .andExpect(jsonPath("$.impresiones").value(2));
            mvc.perform(get("/pedidos/" + pedido[0]).header("Authorization", token))
                    .andExpect(jsonPath("$.estado").value("comandado"))
                    .andExpect(jsonPath("$.personas[0].lineas[0].estado").value("comandado"));
        }
    }

    @Test
    void cocina_y_motorizado_ven_la_comanda_pero_no_comandan_ni_imprimen() throws Exception {
        String admin = tokenPara("administrador de local");
        String[] pedido = pedidoConLinea(admin);
        String comandaId = comandar(admin, pedido[0]);

        for (String rol : new String[] {"cocina", "motorizado"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/comandas/" + comandaId).header("Authorization", token)).andExpect(status().isOk());
            mvc.perform(get("/pedidos/" + pedido[0] + "/comandas").header("Authorization", token))
                    .andExpect(status().isOk());
            mvc.perform(post("/pedidos/" + pedido[0] + "/comandas").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/comandas/" + comandaId + "/impresiones").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/pedidos/" + pedido[0] + "/lineas/" + pedido[1] + "/anular")
                            .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoDeAnulacion()))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void solo_cajero_y_administradores_anulan_el_mozo_no() throws Exception {
        String admin = tokenPara("administrador de local");
        for (String rol : new String[] {"cajero", "administrador de local", "administrador central"}) {
            String[] pedido = pedidoConLinea(admin);
            comandar(admin, pedido[0]);

            mvc.perform(post("/pedidos/" + pedido[0] + "/lineas/" + pedido[1] + "/anular")
                            .header("Authorization", tokenPara("mozo")).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoDeAnulacion()))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/pedidos/" + pedido[0] + "/lineas/" + pedido[1] + "/anular")
                            .header("Authorization", tokenPara(rol)).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoDeAnulacion()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.personas[0].lineas[0].estado").value("anulado"))
                    .andExpect(jsonPath("$.personas[0].lineas[0].anuladaPostComanda").value(true))
                    .andExpect(jsonPath("$.personas[0].lineas[0].motivoAnulacion").value(motivo.getDescripcion()));
        }
    }

    @Test
    void un_pedido_o_una_comanda_de_otro_local_da_404_en_cada_verbo() throws Exception {
        String admin = tokenPara("administrador de local");
        String[] pedido = pedidoConLinea(admin);
        String comandaId = comandar(admin, pedido[0]);
        String deOtro = tokenPara("administrador de local", nuevoLocalDeAcceso("co"));

        mvc.perform(post("/pedidos/" + pedido[0] + "/comandas").header("Authorization", deOtro)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/pedidos/" + pedido[0] + "/comandas").header("Authorization", deOtro))
                .andExpect(status().isNotFound());
        mvc.perform(get("/comandas/" + comandaId).header("Authorization", deOtro)).andExpect(status().isNotFound());
        mvc.perform(post("/comandas/" + comandaId + "/impresiones").header("Authorization", deOtro))
                .andExpect(status().isNotFound());
        mvc.perform(post("/pedidos/" + pedido[0] + "/lineas/" + pedido[1] + "/anular")
                        .header("Authorization", deOtro).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeAnulacion()))
                .andExpect(status().isNotFound());
    }

    @Test
    void los_errores_dan_codigos_claros() throws Exception {
        String admin = tokenPara("administrador de local");
        String[] pedido = pedidoConLinea(admin);

        // Comandar dos veces seguidas: la segunda no tiene nada nuevo que mandar.
        comandar(admin, pedido[0]);
        mvc.perform(post("/pedidos/" + pedido[0] + "/comandas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("SIN_LINEAS_PENDIENTES"));

        // Anular: motivo inexistente, y ya anulada.
        mvc.perform(post("/pedidos/" + pedido[0] + "/lineas/" + pedido[1] + "/anular")
                        .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivoId\":\"no-existe\"}"))
                .andExpect(status().is(422)).andExpect(jsonPath("$.error").value("MOTIVO_NO_ENCONTRADO"));
        mvc.perform(post("/pedidos/" + pedido[0] + "/lineas/" + pedido[1] + "/anular")
                        .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeAnulacion()))
                .andExpect(status().isOk());
        mvc.perform(post("/pedidos/" + pedido[0] + "/lineas/" + pedido[1] + "/anular")
                        .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeAnulacion()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("LINEA_ANULADA"));
        mvc.perform(get("/comandas/no-existe").header("Authorization", admin)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("COMANDA_NO_ENCONTRADA"));
    }
}

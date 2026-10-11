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
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.identidad.infraestructura.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-027 con los roles reales. Administradores: todo. Cajero y mozo: ven el
 * mapa y bloquean y liberan, pero no crean ni editan mesas. Cocina y
 * motorizado: nada de mesas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MesasAccesoPorRolTest {

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

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
    CrearLocal crearLocal;

    Local local;

    @BeforeEach
    void preparar() {
        local = nuevoLocal("mr");
    }

    Local nuevoLocal(String prefijo) {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        try {
            String codigo = prefijo + Identificador.nuevo().valor().substring(20).toLowerCase();
            return locales.findById(crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID)
                    .valorOLanza().getId()).orElseThrow();
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
    }

    String tokenPara(String nombreRol, Local delLocal) throws Exception {
        String login = "mesrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(18);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", nombreRol,
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals(nombreRol)).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));
        usuarioLocales.save(new UsuarioLocal(usuario, delLocal, true, ADMIN_SEMBRADO_ID));
        String respuesta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario.getNombreUsuario()
                                + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + MAPEADOR.readTree(respuesta).get("token").asText();
    }

    String tokenPara(String nombreRol) throws Exception {
        return tokenPara(nombreRol, local);
    }

    String crearMesaComo(String token, String numero) throws Exception {
        String cuerpo = mvc.perform(post("/mesas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"" + numero + "\",\"nombre\":\"Terraza\",\"capacidad\":4}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return MAPEADOR.readTree(cuerpo).get("id").asText();
    }

    @Test
    void administradores_hacen_todo_y_el_mapa_muestra_el_estado_derivado() throws Exception {
        for (String rol : new String[] {"administrador central", "administrador de local"}) {
            String token = tokenPara(rol);
            String numero = rol.startsWith("administrador c") ? "1" : "2";
            String id = crearMesaComo(token, numero);

            mvc.perform(put("/mesas/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"numero\":\"" + numero + "\",\"nombre\":\"Ventana\",\"capacidad\":6}"))
                    .andExpect(status().isOk());
            mvc.perform(post("/mesas/" + id + "/bloquear").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Mesa rota\"}"))
                    .andExpect(status().isOk());
            JsonNode mapa = MAPEADOR.readTree(mvc.perform(get("/mesas").header("Authorization", token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            JsonNode fila = null;
            for (JsonNode f : mapa) {
                if (f.get("id").asText().equals(id)) {
                    fila = f;
                }
            }
            assertThat(fila).isNotNull();
            assertThat(fila.get("estado").asText()).isEqualTo("bloqueada");
            assertThat(fila.get("motivoBloqueo").asText()).isEqualTo("Mesa rota");
            mvc.perform(post("/mesas/" + id + "/liberar").header("Authorization", token)).andExpect(status().isOk());
            mvc.perform(post("/mesas/" + id + "/desactivar").header("Authorization", token))
                    .andExpect(status().isOk());
            mvc.perform(post("/mesas/" + id + "/activar").header("Authorization", token)).andExpect(status().isOk());
        }
    }

    @Test
    void cajero_y_mozo_ven_el_mapa_y_bloquean_pero_no_crean_ni_editan() throws Exception {
        String admin = tokenPara("administrador de local");
        String id = crearMesaComo(admin, "1");
        for (String rol : new String[] {"cajero", "mozo"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/mesas").header("Authorization", token)).andExpect(status().isOk());
            mvc.perform(post("/mesas/" + id + "/bloquear").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Reservada\"}"))
                    .andExpect(status().isOk());
            mvc.perform(post("/mesas/" + id + "/liberar").header("Authorization", token)).andExpect(status().isOk());

            mvc.perform(post("/mesas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"numero\":\"9\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/mesas/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"numero\":\"1\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/mesas/" + id + "/desactivar").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/mesas/" + id + "/activar").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void cocina_y_motorizado_solo_ven_el_mapa_y_no_bloquean_ni_administran() throws Exception {
        // Ambos tienen venta.ver sembrado (cocina lo necesita para ver comandas): ven el mapa, nada mas.
        String admin = tokenPara("administrador de local");
        String id = crearMesaComo(admin, "1");
        for (String rol : new String[] {"cocina", "motorizado"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/mesas").header("Authorization", token)).andExpect(status().isOk());
            mvc.perform(post("/mesas/" + id + "/bloquear").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/mesas/" + id + "/liberar").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/mesas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"numero\":\"9\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/mesas/" + id + "/desactivar").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void los_errores_dan_codigos_claros() throws Exception {
        String token = tokenPara("administrador de local");
        String id = crearMesaComo(token, "1");

        mvc.perform(post("/mesas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"1\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/mesas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"   \"}"))
                .andExpect(status().is(422));
        mvc.perform(post("/mesas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"8\",\"capacidad\":0}"))
                .andExpect(status().is(422));
        mvc.perform(post("/mesas/" + id + "/bloquear").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"  \"}"))
                .andExpect(status().is(422));
        mvc.perform(post("/mesas/" + id + "/liberar").header("Authorization", token))
                .andExpect(status().isConflict());
        mvc.perform(post("/mesas/no-existe/bloquear").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void una_mesa_de_otro_local_no_se_ve_ni_se_toca() throws Exception {
        String token = tokenPara("administrador de local");
        Local otro = nuevoLocal("mo");
        String idDelOtro = crearMesaComo(tokenPara("administrador de local", otro), "1");

        mvc.perform(post("/mesas/" + idDelOtro + "/bloquear").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}"))
                .andExpect(status().isNotFound());
        JsonNode mapa = MAPEADOR.readTree(mvc.perform(get("/mesas").header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        for (JsonNode f : mapa) {
            assertThat(f.get("id").asText()).isNotEqualTo(idDelOtro);
        }
    }

    /**
     * El POS decide que botones mostrar con los permisos PROPIOS del usuario. No puede usar
     * /usuarios/{id}/permisos (es de administradores): daria 403 al mozo y dejaria una fila
     * de acceso denegado en la bitacora en cada ingreso.
     */
    @Test
    void cada_usuario_ve_sus_propios_permisos_sin_ser_administrador() throws Exception {
        java.util.Map<String, Boolean> puedeBloquear = java.util.Map.of("mozo", true, "cajero", true,
                "administrador de local", true, "cocina", false, "motorizado", false);
        for (var entrada : puedeBloquear.entrySet()) {
            String cuerpo = mvc.perform(get("/auth/mis-permisos").header("Authorization", tokenPara(entrada.getKey())))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            java.util.List<String> codigos = new java.util.ArrayList<>();
            MAPEADOR.readTree(cuerpo).forEach(n -> codigos.add(n.asText()));
            assertThat(codigos.contains("venta.bloquear_mesa")).as(entrada.getKey()).isEqualTo(entrada.getValue());
        }
        // Solo administradores administran mesas.
        String mozo = mvc.perform(get("/auth/mis-permisos").header("Authorization", tokenPara("mozo")))
                .andReturn().getResponse().getContentAsString();
        assertThat(mozo).doesNotContain("venta.administrar\"");
        mvc.perform(get("/auth/mis-permisos")).andExpect(status().isForbidden());
    }
}

package pe.rikos.operacion.pensionistas.api;

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
import pe.rikos.operacion.pensionistas.aplicacion.CrearPlanTarifario;
import pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan;
import pe.rikos.operacion.identidad.infraestructura.*;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-016 con los roles reales. Administrador central y de local hacen todo
 * (crear, editar, PIN, activar y desactivar); cajero y mozo solo consultan;
 * cocina y motorizado no entran. Y RN-090 de punta a punta: ninguna respuesta
 * de lectura lleva el PIN ni su hash.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PensionistasAccesoPorRolTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepositorio usuarios;
    @Autowired
    private RolRepositorio roles;
    @Autowired
    private UsuarioRolRepositorio usuarioRoles;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CrearPlanTarifario crearPlan;

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    private String planId;

    @BeforeEach
    void plan() {
        // Crear el plan se audita: hace falta un usuario en el contexto solo durante este paso.
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        try {
            planId = crearPlan.ejecutar("Plan acceso " + Identificador.nuevo().valor().substring(16), null,
                    new PreciosPlan(500L, 800L, 800L), null, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
    }

    private static String dni() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
    }

    private String cuerpo() {
        return "{\"dni\":\"" + dni() + "\",\"nombres\":\"Ana\",\"apellidos\":\"Torres\",\"planId\":\"" + planId
                + "\",\"comidas\":[\"almuerzo\"],\"responsable\":{\"nombreCompleto\":\"Marta Torres\",\"dni\":\""
                + dni() + "\",\"telefono\":\"999888777\",\"correo\":\"marta@correo.pe\",\"parentesco\":\"Madre\","
                + "\"direccion\":\"Av. Lima 100\"}}";
    }

    private String tokenPara(String nombreRol) throws Exception {
        String login = "penrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(18);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", nombreRol,
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals(nombreRol)).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));

        String respuesta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario.getNombreUsuario()
                                + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + MAPEADOR.readTree(respuesta).get("token").asText();
    }

    private JsonNode crearComo(String token) throws Exception {
        String respuesta = mvc.perform(post("/pensionistas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return MAPEADOR.readTree(respuesta);
    }

    @Test
    void administrador_central_y_de_local_hacen_todo_y_el_pin_solo_aparece_al_crear_y_regenerar() throws Exception {
        for (String rol : new String[] {"administrador central", "administrador de local"}) {
            String token = tokenPara(rol);
            JsonNode creado = crearComo(token);
            String id = creado.get("pensionista").get("id").asText();
            assertThat(creado.get("pin").asText()).matches("\\d{4}");
            assertThat(creado.get("pensionista").get("cuentaId").asText()).isNotBlank();

            // La ficha y la lista no llevan pin, hash ni contrasena.
            String ficha = mvc.perform(get("/pensionistas/" + id).header("Authorization", token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String lista = mvc.perform(get("/pensionistas").header("Authorization", token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            for (String texto : new String[] {ficha, lista}) {
                assertThat(texto.toLowerCase()).doesNotContain("pin").doesNotContain("hash").doesNotContain("contrasena")
                        .doesNotContain("$2a$");
            }

            mvc.perform(put("/pensionistas/" + id).header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo()))
                    .andExpect(status().isOk());
            String nuevo = mvc.perform(post("/pensionistas/" + id + "/pin").header("Authorization", token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(MAPEADOR.readTree(nuevo).get("pin").asText()).matches("\\d{4}");
            mvc.perform(post("/pensionistas/" + id + "/desactivar").header("Authorization", token))
                    .andExpect(status().isNoContent());
            mvc.perform(post("/pensionistas/" + id + "/activar").header("Authorization", token))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    void cajero_y_mozo_consultan_pero_no_escriben_nada() throws Exception {
        String id = crearComo(tokenPara("administrador central")).get("pensionista").get("id").asText();
        for (String rol : new String[] {"cajero", "mozo"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/pensionistas").header("Authorization", token)).andExpect(status().isOk());
            mvc.perform(get("/pensionistas/" + id).header("Authorization", token)).andExpect(status().isOk());
            mvc.perform(post("/pensionistas").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo()))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/pensionistas/" + id).header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo()))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/pensionistas/" + id + "/pin").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/pensionistas/" + id + "/desactivar").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/pensionistas/" + id + "/activar").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void cocina_y_motorizado_no_ven_pensionistas() throws Exception {
        for (String rol : new String[] {"cocina", "motorizado"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/pensionistas").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(post("/pensionistas").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo()))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void los_errores_de_datos_dan_codigos_claros() throws Exception {
        String token = tokenPara("administrador central");
        String primero = cuerpo();
        mvc.perform(post("/pensionistas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(primero)).andExpect(status().isCreated());

        // Mismo DNI: 409.
        mvc.perform(post("/pensionistas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(primero))
                .andExpect(status().isConflict());
        // Sin responsable: 422.
        mvc.perform(post("/pensionistas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"" + dni() + "\",\"nombres\":\"Ana\",\"apellidos\":\"T\",\"planId\":\""
                                + planId + "\",\"comidas\":[\"almuerzo\"]}"))
                .andExpect(status().is(422));
        // Comida que no existe: 422.
        mvc.perform(post("/pensionistas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo().replace("almuerzo", "merienda")))
                .andExpect(status().is(422));
        mvc.perform(get("/pensionistas/no-existe").header("Authorization", token)).andExpect(status().isNotFound());
        mvc.perform(post("/pensionistas/no-existe/pin").header("Authorization", token))
                .andExpect(status().isNotFound());
    }
}

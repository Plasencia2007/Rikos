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
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.infraestructura.*;
import pe.rikos.operacion.venta.aplicacion.PedidosBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-032 con los roles reales: ver las cajas es caja.ver (cajero y administradores; el mozo no cobra), abrir un turno es
 * caja.crear y crear una caja es caja.administrar (solo administradores). El local sale de la sesion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CajasYTurnosAccesoPorRolTest extends PedidosBase {

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

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        try {
            local = locales.findById(nuevoLocal("ct")).orElseThrow();
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
    }

    String tokenPara(String nombreRol) throws Exception {
        String login = "caja-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(18);
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

    String primeraCaja(String token) throws Exception {
        JsonNode cajas = MAPEADOR.readTree(mvc.perform(get("/cajas").header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return cajas.get(0).get("id").asText();
    }

    @Test
    void cajero_y_administradores_ven_las_cajas_y_abren_su_turno() throws Exception {
        for (String rol : new String[] {"cajero", "administrador de local", "administrador central"}) {
            String token = tokenPara(rol);
            String cajaId = primeraCaja(token);

            // Un turno por rol: se abre, aparece en la lista y como turno actual, y se cierra la caja para el siguiente
            // (aqui solo se comprueba el acceso; cada rol abre en una caja propia).
            String nueva = MAPEADOR.readTree(mvc.perform(post("/cajas").header("Authorization", tokenPara("administrador central"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Caja de " + rol + "\"}"))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
            mvc.perform(post("/cajas/" + nueva + "/turnos").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"fondoInicial\":15000}"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.fondoInicial").value(15000))
                    .andExpect(jsonPath("$.etiqueta").value("Caja de Test"))
                    .andExpect(jsonPath("$.estado").value("abierto")).andExpect(jsonPath("$.cajaId").value(nueva));
            mvc.perform(get("/turnos/actual").header("Authorization", token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.cajaId").value(nueva));
            mvc.perform(get("/cajas").header("Authorization", token)).andExpect(status().isOk());
            // La misma caja no abre un segundo turno ni el mismo cajero otro en otra caja.
            mvc.perform(post("/cajas/" + nueva + "/turnos").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"fondoInicial\":1}"))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CAJA_CON_TURNO_ABIERTO"));
            mvc.perform(post("/cajas/" + cajaId + "/turnos").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"fondoInicial\":1}"))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CAJERO_CON_TURNO_ABIERTO"));
        }
    }

    @Test
    void sin_turno_el_turno_actual_es_204() throws Exception {
        mvc.perform(get("/turnos/actual").header("Authorization", tokenPara("cajero"))).andExpect(status().isNoContent());
    }

    @Test
    void solo_los_administradores_crean_cajas() throws Exception {
        for (String rol : new String[] {"administrador de local", "administrador central"}) {
            mvc.perform(post("/cajas").header("Authorization", tokenPara(rol)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Barra " + rol.length() + "\"}"))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post("/cajas").header("Authorization", tokenPara("cajero")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Pirata\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mozo_cocina_y_motorizado_no_ven_ni_abren_cajas() throws Exception {
        String admin = tokenPara("administrador de local");
        String cajaId = primeraCaja(admin);
        for (String rol : new String[] {"mozo", "cocina", "motorizado"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/cajas").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(get("/turnos/actual").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(post("/cajas/" + cajaId + "/turnos").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"fondoInicial\":100}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/cajas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"X\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void sin_sesion_no_entra() throws Exception {
        mvc.perform(get("/cajas")).andExpect(status().isForbidden());
    }

    @Test
    void un_fondo_negativo_es_422_y_una_caja_ajena_es_404() throws Exception {
        String token = tokenPara("cajero");
        String cajaId = primeraCaja(token);
        mvc.perform(post("/cajas/" + cajaId + "/turnos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fondoInicial\":-5}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error").value("FONDO_INVALIDO"));
        mvc.perform(post("/cajas/" + Identificador.nuevo().valor() + "/turnos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fondoInicial\":5}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("CAJA_NO_ENCONTRADA"));
    }
}

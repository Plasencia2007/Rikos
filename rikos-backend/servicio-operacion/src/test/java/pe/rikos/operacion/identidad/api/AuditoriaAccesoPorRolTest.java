package pe.rikos.operacion.identidad.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.identidad.infraestructura.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hallazgo de revision: /auditoria (US-006) no tenia el equivalente a
 * CatalogoAccesoPorRolTest -- "un rol que nadie ejercita en las pruebas es
 * un rol que nadie sabe si funciona", la misma leccion del bug de
 * "administrador de local" del sprint 2. Un caso por rol.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditoriaAccesoPorRolTest {

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

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    private String tokenPara(String nombreRol) throws Exception {
        String login = "audrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(0, 6);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", nombreRol,
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals(nombreRol)).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));

        String cuerpo = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario.getNombreUsuario()
                                + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        return MAPEADOR.readTree(cuerpo).get("token").asText();
    }

    @Test
    void administrador_central_puede_ver_la_bitacora() throws Exception {
        mvc.perform(get("/auditoria").header("Authorization", "Bearer " + tokenPara("administrador central")))
                .andExpect(status().isOk());
    }

    @Test
    void administrador_de_local_puede_ver_la_bitacora() throws Exception {
        mvc.perform(get("/auditoria").header("Authorization", "Bearer " + tokenPara("administrador de local")))
                .andExpect(status().isOk());
    }

    @Test
    void cajero_no_puede_ver_la_bitacora() throws Exception {
        mvc.perform(get("/auditoria").header("Authorization", "Bearer " + tokenPara("cajero")))
                .andExpect(status().isForbidden());
    }

    @Test
    void mozo_no_puede_ver_la_bitacora() throws Exception {
        mvc.perform(get("/auditoria").header("Authorization", "Bearer " + tokenPara("mozo")))
                .andExpect(status().isForbidden());
    }

    @Test
    void cocina_no_puede_ver_la_bitacora() throws Exception {
        mvc.perform(get("/auditoria").header("Authorization", "Bearer " + tokenPara("cocina")))
                .andExpect(status().isForbidden());
    }

    @Test
    void motorizado_no_puede_ver_la_bitacora() throws Exception {
        mvc.perform(get("/auditoria").header("Authorization", "Bearer " + tokenPara("motorizado")))
                .andExpect(status().isForbidden());
    }
}

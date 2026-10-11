package pe.rikos.operacion.catalogo.api;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hallazgo de revision: /motivos (US-012) no tenia el equivalente a
 * CatalogoAccesoPorRolTest. "administrador de local" solo tiene
 * catalogo.ver -- el catalogo (motivo incluido) es central, no se edita
 * por local -- asi que puede leer pero no crear, a diferencia de otros
 * modulos donde si puede escribir.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MotivoAccesoPorRolTest {

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
        String login = "motrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(0, 6);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", nombreRol,
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals(nombreRol)).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));

        String cuerpo = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario.getNombreUsuario()
                                + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        return MAPEADOR.readTree(cuerpo).get("token").asText();
    }

    @Test
    void administrador_central_puede_ver_y_crear_motivos() throws Exception {
        String token = tokenPara("administrador central");
        mvc.perform(get("/motivos").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/motivos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"anulacion\",\"descripcion\":\"Prueba rol central\",\"requiereObservacion\":false}"))
                .andExpect(status().isCreated());
    }

    @Test
    void administrador_de_local_puede_ver_pero_no_crear_motivos() throws Exception {
        String token = tokenPara("administrador de local");
        mvc.perform(get("/motivos").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/motivos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"anulacion\",\"descripcion\":\"Prueba rol local\",\"requiereObservacion\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cajero_puede_ver_pero_no_crear_motivos() throws Exception {
        String token = tokenPara("cajero");
        mvc.perform(get("/motivos").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/motivos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"anulacion\",\"descripcion\":\"Prueba rol cajero\",\"requiereObservacion\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void motorizado_no_puede_ni_ver_motivos() throws Exception {
        mvc.perform(get("/motivos").header("Authorization", "Bearer " + tokenPara("motorizado")))
                .andExpect(status().isForbidden());
    }
}

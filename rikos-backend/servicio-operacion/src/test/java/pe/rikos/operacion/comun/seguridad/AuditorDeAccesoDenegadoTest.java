package pe.rikos.operacion.comun.seguridad;

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
import pe.rikos.operacion.comun.auditoria.Auditoria;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import pe.rikos.operacion.identidad.infraestructura.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caso critico 19 (13.1): "el intento denegado tambien se registra". Un
 * mozo (sin auditoria.ver) intenta abrir la bitacora -- 403 real, y queda
 * una fila de auditoria que lo prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditorDeAccesoDenegadoTest {

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
    private AuditoriaRepositorio auditorias;

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Test
    void unIntentoDenegadoQuedaAuditado() throws Exception {
        String login = "denegado-" + Identificador.nuevo().valor().substring(0, 8);
        Usuario mozo = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", "Mozo",
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rolMozo = roles.findAll().stream().filter(r -> r.getNombre().equals("mozo")).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(mozo, rolMozo, ADMIN_SEMBRADO_ID));

        String cuerpoLogin = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + login + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = MAPEADOR.readTree(cuerpoLogin).get("token").asText();

        mvc.perform(get("/auditoria").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        List<Auditoria> filas = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("acceso", "denegado");
        assertThat(filas).isNotEmpty();
        Auditoria ultima = filas.get(0);
        assertThat(ultima.getEvento()).isEqualTo("acceso_denegado");
        assertThat(ultima.getUsuarioId()).isEqualTo(mozo.getId());
        assertThat(ultima.getObservacion()).contains("GET").contains("/auditoria");
    }
}

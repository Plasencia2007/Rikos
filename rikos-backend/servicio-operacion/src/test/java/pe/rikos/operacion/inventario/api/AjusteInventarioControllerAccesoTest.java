package pe.rikos.operacion.inventario.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.identidad.infraestructura.Empresa;
import pe.rikos.operacion.identidad.infraestructura.EmpresaRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.RolRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRol;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AjusteInventarioControllerAccesoTest {

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
    private EmpresaRepositorio empresas;
    @Autowired
    private LocalRepositorio locales;

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    private Local local;

    @BeforeEach
    void setupDatos() {
        Empresa empresa = empresas.findAll().stream().findFirst().orElseGet(() ->
                empresas.save(new Empresa(Identificador.nuevo().valor(), "20123456789", "Rikos SAC", "Rikos", "Av Principal 123")));

        local = locales.findAll().stream().findFirst().orElseGet(() ->
                locales.save(new Local(Identificador.nuevo().valor(), empresa, "LOC-01", "Local Central", "Av Central 456", ADMIN_SEMBRADO_ID)));
    }

    private String tokenPara(String nombreRol) throws Exception {
        String login = "ajusterol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(0, 6);
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
    @DisplayName("Sin autenticación devuelve 403 Forbidden")
    void sinAutenticacion_devuelve403() throws Exception {
        mvc.perform(get("/locales/" + local.getId() + "/inventario/ajustes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Mozo sin permiso de inventario no puede consultar ni registrar ajustes")
    void mozo_devuelve403() throws Exception {
        String tokenMozo = tokenPara("mozo");
        mvc.perform(get("/locales/" + local.getId() + "/inventario/ajustes")
                        .header("Authorization", "Bearer " + tokenMozo))
                .andExpect(status().isForbidden());

        mvc.perform(post("/locales/" + local.getId() + "/inventario/ajustes")
                        .header("Authorization", "Bearer " + tokenMozo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Administrador de local puede consultar ajustes")
    void administradorDeLocal_puedeConsultar() throws Exception {
        String tokenAdminLocal = tokenPara("administrador de local");
        mvc.perform(get("/locales/" + local.getId() + "/inventario/ajustes")
                        .header("Authorization", "Bearer " + tokenAdminLocal))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Administrador central puede consultar ajustes")
    void administradorCentral_puedeConsultar() throws Exception {
        String tokenAdminCentral = tokenPara("administrador central");
        mvc.perform(get("/locales/" + local.getId() + "/inventario/ajustes")
                        .header("Authorization", "Bearer " + tokenAdminCentral))
                .andExpect(status().isOk());
    }
}

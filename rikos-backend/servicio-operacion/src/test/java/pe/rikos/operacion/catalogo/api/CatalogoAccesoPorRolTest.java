package pe.rikos.operacion.catalogo.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.identidad.infraestructura.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Una prueba por cada uno de los 6 roles del sistema, contra un endpoint
 * real de catalogo. La leccion del sprint anterior: "administrador de
 * local" recibio 403 en todo el panel durante meses porque todas las
 * pruebas anteriores entraban como administrador central. No vuelve a
 * pasar si cada rol tiene su propia prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogoAccesoPorRolTest {

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

    // Este modulo no trae jackson-databind al classpath de compilacion
    // (spring-boot-starter-webmvc, split de Boot 4, no lo arrastra --
    // hallazgo ya documentado en RegistradorAuditoria, sprint 1). Se evita
    // agregar la dependencia y se extrae el token a mano en vez de usar
    // ObjectMapper.
    private static final Pattern TOKEN_EN_JSON = Pattern.compile("\"token\":\"([^\"]+)\"");

    // usuario_rol.asignado_por es NOT NULL (a diferencia de creado_por/
    // actualizado_por en el resto de tablas, que si admiten null) -- usa el
    // id del admin sembrado en V9__datos_semilla.sql.
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    private String tokenPara(String nombreRol) throws Exception {
        String login = "test-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(0, 6);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", nombreRol,
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals(nombreRol)).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));

        String cuerpo = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario.getNombreUsuario()
                                + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        Matcher m = TOKEN_EN_JSON.matcher(cuerpo);
        if (!m.find()) {
            throw new IllegalStateException("No se encontro token en la respuesta de login: " + cuerpo);
        }
        return m.group(1);
    }

    @Test
    void administrador_central_puede_crear_productos() throws Exception {
        String token = tokenPara("administrador central");
        mvc.perform(get("/categorias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void administrador_de_local_puede_crear_productos() throws Exception {
        String token = tokenPara("administrador de local");
        mvc.perform(get("/categorias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void cajero_solo_puede_leer_no_crear() throws Exception {
        String token = tokenPara("cajero");
        mvc.perform(get("/categorias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(post("/categorias").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"x\",\"naturaleza\":\"insumo\",\"orden\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mozo_puede_leer_el_catalogo_pero_no_crear() throws Exception {
        // mozo tiene catalogo.ver sembrado (necesita ver el catalogo para
        // tomar pedidos) pero no catalogo.crear -- confirmado contra la
        // tabla rol_permiso real, no asumido.
        String token = tokenPara("mozo");
        mvc.perform(get("/categorias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(post("/categorias").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"x\",\"naturaleza\":\"insumo\",\"orden\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cocina_puede_leer_y_modificar_el_catalogo_pero_no_crear() throws Exception {
        // cocina tiene catalogo.ver + catalogo.modificar sembrados (marca
        // productos agotados) pero no catalogo.crear -- confirmado contra
        // la tabla rol_permiso real.
        String token = tokenPara("cocina");
        mvc.perform(get("/categorias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(post("/categorias").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"x\",\"naturaleza\":\"insumo\",\"orden\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void motorizado_no_tiene_acceso_ni_de_lectura() throws Exception {
        String token = tokenPara("motorizado");
        mvc.perform(get("/categorias").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}

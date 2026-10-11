package pe.rikos.operacion.catalogo.api;

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
import pe.rikos.operacion.catalogo.infraestructura.*;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.identidad.infraestructura.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-015 con los roles reales: cocina, administrador de local y central
 * marcan y reactivan (menu.marcar_agotado); mozo y cajero solo leen;
 * motorizado no entra. Y CA4 de punta a punta: lo que cocina marca en su
 * local no aparece agotado para el mozo de otro local.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DisponibilidadAccesoPorRolTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepositorio usuarios;
    @Autowired
    private RolRepositorio roles;
    @Autowired
    private UsuarioRolRepositorio usuarioRoles;
    @Autowired
    private UsuarioLocalRepositorio usuarioLocales;
    @Autowired
    private LocalRepositorio locales;
    @Autowired
    private CrearLocal crearLocal;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ProductoRepositorio productos;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    private static final String MOTIVO_SEMBRADO_ID = "01M39A0MT0000000000000D001";
    private static final String REACTIVAR = "{\"motivoId\":\"" + MOTIVO_SEMBRADO_ID + "\"}";

    private Local localA;
    private Local localB;
    private Producto aji;

    @BeforeEach
    void preparar() {
        localA = local("ra");
        localB = local("rb");
        CategoriaProducto pollo = categorias.findById("01M39A0RZ0000000000000S004").orElseThrow();
        UnidadMedida und = unidades.findAll().stream().filter(u -> u.getCodigo().equals("und")).findFirst()
                .orElseThrow();
        String codigo = "DRA-" + Identificador.nuevo().valor().substring(16);
        aji = productos.save(new Producto(Identificador.nuevo().valor(), codigo, "Ají " + codigo, pollo, und, true,
                false, false, 1500L, false, null));
    }

    private Local local(String prefijo) {
        String codigo = prefijo + Identificador.nuevo().valor().substring(20).toLowerCase();
        String id = crearLocal.ejecutar(codigo, "Local " + codigo, "Dirección", ADMIN_SEMBRADO_ID).valorOLanza().getId();
        return locales.findById(id).orElseThrow();
    }

    private String tokenPara(String nombreRol, Local local) throws Exception {
        // La parte aleatoria del ULID: los primeros caracteres son la marca de tiempo
        // y dos usuarios del mismo rol en la misma prueba saldrian con el mismo login.
        String login = "disrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(18);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", nombreRol,
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals(nombreRol)).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));
        usuarioLocales.save(new UsuarioLocal(usuario, local, true, ADMIN_SEMBRADO_ID));

        String cuerpo = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario.getNombreUsuario()
                                + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + MAPEADOR.readTree(cuerpo).get("token").asText();
    }

    private String agotados(String token) throws Exception {
        return mvc.perform(get("/disponibilidad/agotados").header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void cocina_administrador_de_local_y_central_marcan_y_reactivan_en_su_local() throws Exception {
        for (String rol : new String[] {"cocina", "administrador de local", "administrador central"}) {
            String token = tokenPara(rol, localA);
            mvc.perform(post("/disponibilidad/" + aji.getId() + "/agotar").header("Authorization", token))
                    .andExpect(status().isOk());
            assertThat(agotados(token)).contains(aji.getId());
            mvc.perform(post("/disponibilidad/" + aji.getId() + "/reactivar").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(REACTIVAR))
                    .andExpect(status().isOk());
            assertThat(agotados(token)).doesNotContain(aji.getId());
        }
    }

    @Test
    void mozo_y_cajero_leen_pero_no_marcan() throws Exception {
        for (String rol : new String[] {"mozo", "cajero"}) {
            String token = tokenPara(rol, localA);
            agotados(token);
            mvc.perform(post("/disponibilidad/" + aji.getId() + "/agotar").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/disponibilidad/" + aji.getId() + "/reactivar").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(REACTIVAR))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void motorizado_no_ve_la_disponibilidad() throws Exception {
        mvc.perform(get("/disponibilidad/agotados").header("Authorization", tokenPara("motorizado", localA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void lo_que_cocina_agota_en_su_local_sigue_disponible_en_otro() throws Exception {
        String cocinaA = tokenPara("cocina", localA);
        String mozoA = tokenPara("mozo", localA);
        String mozoB = tokenPara("mozo", localB);

        mvc.perform(post("/disponibilidad/" + aji.getId() + "/agotar").header("Authorization", cocinaA))
                .andExpect(status().isOk());

        assertThat(agotados(mozoA)).contains(aji.getId());
        assertThat(agotados(mozoB)).doesNotContain(aji.getId());
    }
}

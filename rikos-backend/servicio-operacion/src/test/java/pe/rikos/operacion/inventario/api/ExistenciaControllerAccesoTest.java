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
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExistenciaControllerAccesoTest {

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
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private ProductoRepositorio productos;

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    private Local local;
    private Producto producto;

    @BeforeEach
    void setupDatos() {
        Empresa empresa = empresas.findAll().stream().findFirst().orElseGet(() ->
                empresas.save(new Empresa(Identificador.nuevo().valor(), "20123456789", "Rikos SAC", "Rikos", "Av Principal 123")));

        local = locales.findAll().stream().findFirst().orElseGet(() ->
                locales.save(new Local(Identificador.nuevo().valor(), empresa, "LOC-01", "Local Central", "Av Central 456", ADMIN_SEMBRADO_ID)));

        CategoriaProducto cat = categorias.findAll().stream().findFirst().orElseGet(() ->
                categorias.save(new CategoriaProducto(Identificador.nuevo().valor(), "Insumos", "insumo", null, 1, ADMIN_SEMBRADO_ID)));

        UnidadMedida uni = unidades.findAll().stream().findFirst().orElseGet(() ->
                unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "kg", "Kilogramos", "masa")));

        producto = productos.findAll().stream().findFirst().orElseGet(() ->
                productos.save(new Producto(Identificador.nuevo().valor(), "INS-01", "Insumo Test", cat, uni,
                        false, true, false, null, true, ADMIN_SEMBRADO_ID)));
    }

    private String tokenPara(String nombreRol) throws Exception {
        String login = "invrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(0, 6);
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
    @DisplayName("Sin token JWT devuelve 403 Forbidden")
    void sinAutenticacion_devuelve403() throws Exception {
        mvc.perform(get("/locales/" + local.getId() + "/existencias"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Administrador central puede ver existencias, resumen y configurar stock mínimo")
    void administradorCentral_accesoCompleto() throws Exception {
        String token = tokenPara("administrador central");

        mvc.perform(get("/locales/" + local.getId() + "/existencias")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(get("/locales/" + local.getId() + "/existencias/resumen-alertas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProductos").isNumber());

        mvc.perform(put("/locales/" + local.getId() + "/existencias/" + producto.getId() + "/stock-minimo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockMinimo\": 12.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockMinimo").value(12.5));
    }

    @Test
    @DisplayName("Administrador de local puede ver existencias y configurar stock mínimo")
    void administradorDeLocal_puedeVerYConfigurar() throws Exception {
        String token = tokenPara("administrador de local");

        mvc.perform(get("/locales/" + local.getId() + "/existencias")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(put("/locales/" + local.getId() + "/existencias/" + producto.getId() + "/stock-minimo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockMinimo\": 8.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockMinimo").value(8.0));
    }

    @Test
    @DisplayName("Mozo no tiene permiso inventario.administrar y recibe 403 al fijar stock mínimo")
    void mozo_noPuedeConfigurarStockMinimo() throws Exception {
        String token = tokenPara("mozo");

        mvc.perform(put("/locales/" + local.getId() + "/existencias/" + producto.getId() + "/stock-minimo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockMinimo\": 5.0}"))
                .andExpect(status().isForbidden());
    }
}

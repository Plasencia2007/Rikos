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
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KardexControllerAccesoTest {

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
    @Autowired
    private MovimientoInventarioRepositorio movimientos;

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    private Local local;
    private Producto producto;
    private Usuario usuarioAdmin;

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

        // Un producto propio de la prueba: uno cualquiera de la base puede traer movimientos (por ejemplo, los datos de
        // prueba del perfil demo) y el kardex ya no tendria solo el de esta prueba.
        String codigo = "INS-K" + Identificador.nuevo().valor().substring(20);
        producto = productos.save(new Producto(Identificador.nuevo().valor(), codigo, "Insumo Test " + codigo, cat, uni,
                false, true, false, null, true, ADMIN_SEMBRADO_ID));

        usuarioAdmin = usuarios.findAll().stream().findFirst().orElseThrow();

        MovimientoInventario mov = new MovimientoInventario(
                Identificador.nuevo().valor(),
                local,
                producto,
                "compra",
                "entrada",
                new BigDecimal("5.000000"),
                uni,
                new BigDecimal("5.000000"),
                new BigDecimal("15.000000"),
                new BigDecimal("0.000000"),
                new BigDecimal("5.000000"),
                "compra",
                Identificador.nuevo().valor(),
                null,
                "Compra inicial",
                OffsetDateTime.now(),
                usuarioAdmin
        );
        movimientos.save(mov);
    }

    private String tokenPara(String nombreRol) throws Exception {
        String login = "kardexrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(0, 6);
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
        mvc.perform(get("/locales/" + local.getId() + "/inventario/kardex"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Mozo no tiene inventario.ver y recibe 403 Forbidden")
    void mozo_sinPermisoInventario_devuelve403() throws Exception {
        String token = tokenPara("mozo");

        mvc.perform(get("/locales/" + local.getId() + "/inventario/kardex")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Administrador central puede ver el Kardex y ve costos por tener inventario.ver_costos")
    void administradorCentral_veKardexConCostos() throws Exception {
        String token = tokenPara("administrador central");

        mvc.perform(get("/locales/" + local.getId() + "/inventario/kardex?productoId=" + producto.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.elementos[0].productoCodigo").value(producto.getCodigo()))
                .andExpect(jsonPath("$.elementos[0].costoUnitario").isNumber());
    }

    @Test
    @DisplayName("Administrador de local puede ver el Kardex con filtros")
    void administradorDeLocal_veKardexConFiltros() throws Exception {
        String token = tokenPara("administrador de local");

        mvc.perform(get("/locales/" + local.getId() + "/inventario/kardex?productoId=" + producto.getId() + "&origen=compra")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.elementos[0].origen").value("compra"));
    }
}

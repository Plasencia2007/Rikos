package pe.rikos.operacion.compras.api;

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
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.catalogo.infraestructura.ProveedorRepositorio;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompraControllerAccesoTest {

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
    private ProveedorRepositorio proveedores;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private ProductoRepositorio productos;

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    private Local local;
    private Proveedor proveedor;
    private Producto producto;

    @BeforeEach
    void setupDatos() {
        Empresa empresa = empresas.findAll().stream().findFirst().orElseGet(() ->
                empresas.save(new Empresa(Identificador.nuevo().valor(), "20123456789", "Rikos SAC", "Rikos", "Av Principal 123")));

        local = locales.findAll().stream().findFirst().orElseGet(() ->
                locales.save(new Local(Identificador.nuevo().valor(), empresa, "LOC-01", "Local Central", "Av Central 456", ADMIN_SEMBRADO_ID)));

        proveedor = proveedores.findAll().stream().findFirst().orElseGet(() ->
                proveedores.save(new Proveedor(Identificador.nuevo().valor(), "20999888777", "Distribuidora Mayorista SAC",
                        "Mayorista", "999888777", "ventas@mayorista.pe", "Av Industrial 100", "Carlos Ruiz", ADMIN_SEMBRADO_ID)));

        CategoriaProducto cat = categorias.findAll().stream().findFirst().orElseGet(() ->
                categorias.save(new CategoriaProducto(Identificador.nuevo().valor(), "Insumos", "insumo", null, 1, ADMIN_SEMBRADO_ID)));

        UnidadMedida uni = unidades.findAll().stream().findFirst().orElseGet(() ->
                unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "kg", "Kilogramos", "masa")));

        producto = productos.findAll().stream().findFirst().orElseGet(() ->
                productos.save(new Producto(Identificador.nuevo().valor(), "INS-01", "Insumo Test", cat, uni,
                        false, true, false, null, true, ADMIN_SEMBRADO_ID)));
    }

    private String tokenPara(String nombreRol) throws Exception {
        String login = "comprol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(0, 6);
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
        mvc.perform(get("/locales/" + local.getId() + "/compras"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Mozo no tiene permisos de compras y recibe 403 Forbidden")
    void mozo_noTienePermisoCompras_devuelve403() throws Exception {
        String tokenMozo = tokenPara("mozo");

        mvc.perform(get("/locales/" + local.getId() + "/compras")
                        .header("Authorization", "Bearer " + tokenMozo))
                .andExpect(status().isForbidden());

        mvc.perform(post("/locales/" + local.getId() + "/compras")
                        .header("Authorization", "Bearer " + tokenMozo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Administrador central puede listar y registrar compras")
    void administradorCentral_puedeListarYRegistrar() throws Exception {
        String tokenAdmin = tokenPara("administrador central");

        mvc.perform(get("/locales/" + local.getId() + "/compras")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        String payload = """
                {
                  "proveedorId": "%s",
                  "tipoDocumento": "factura",
                  "serie": "F001",
                  "numero": "%s",
                  "fechaDocumento": "%s",
                  "observacion": "Prueba integracion",
                  "confirmarInmediatamente": false,
                  "detalles": [
                    {
                      "productoId": "%s",
                      "cantidad": 5.0,
                      "precioUnitario": 1000
                    }
                  ]
                }
                """.formatted(proveedor.getId(), "INT" + (System.currentTimeMillis() % 100000), LocalDate.now(), producto.getId());

        mvc.perform(post("/locales/" + local.getId() + "/compras")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("borrador"))
                .andExpect(jsonPath("$.total").value(5000));
    }
}

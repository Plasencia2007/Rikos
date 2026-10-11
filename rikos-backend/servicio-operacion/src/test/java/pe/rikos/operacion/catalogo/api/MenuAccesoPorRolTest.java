package pe.rikos.operacion.catalogo.api;

import com.fasterxml.jackson.databind.JsonNode;
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
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-014 con los roles reales del sembrador: el administrador central
 * cambia el precio; mozo, cajero y cocina leen el menu (lo necesitan para
 * tomar el pedido) pero no lo cambian; motorizado no entra. Y el camino
 * lateral: cocina tiene catalogo.modificar, pero la ficha de producto no le
 * deja mover el precio del menu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MenuAccesoPorRolTest {

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
    private ProductoRepositorio productos;

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    private static final String PRECIO = "{\"precioVenta\":1300}";

    private String tokenPara(String nombreRol) throws Exception {
        String login = "menrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(0, 6);
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
    void administrador_central_lee_y_cambia_el_precio() throws Exception {
        String token = tokenPara("administrador central");

        String lista = mvc.perform(get("/menu/tipos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode tipos = MAPEADOR.readTree(lista);
        assertThat(tipos).hasSize(2);
        assertThat(tipos.get(1).get("tipo").asText()).isEqualTo("extra");
        assertThat(tipos.get(1).get("admiteTarifaReducida").asBoolean()).isFalse();

        String cambiado = mvc.perform(put("/menu/normal/precio").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(PRECIO))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(MAPEADOR.readTree(cambiado).get("precioVenta").asLong()).isEqualTo(1300L);
    }

    @Test
    void mozo_cajero_y_cocina_leen_el_menu_pero_no_cambian_el_precio() throws Exception {
        for (String rol : new String[] {"mozo", "cajero", "cocina", "administrador de local"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/menu/tipos").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
            mvc.perform(put("/menu/normal/precio").header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(PRECIO))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void motorizado_no_puede_ni_ver_el_menu() throws Exception {
        mvc.perform(get("/menu/tipos").header("Authorization", "Bearer " + tokenPara("motorizado")))
                .andExpect(status().isForbidden());
    }

    @Test
    void cocina_no_puede_mover_el_precio_del_menu_por_la_ficha_de_producto() throws Exception {
        var normal = productos.findByTipoMenu("normal").orElseThrow();
        String cuerpo = "{\"codigo\":\"" + normal.getCodigo() + "\",\"nombre\":\"Menú normal\",\"categoriaId\":\""
                + normal.getCategoria().getId() + "\",\"unidadBaseId\":\"" + normal.getUnidadBase().getId()
                + "\",\"esVendible\":true,\"esInsumo\":false,\"esSubreceta\":false,\"precioVenta\":100,"
                + "\"controlaStock\":false}";

        mvc.perform(put("/productos/" + normal.getId()).header("Authorization", "Bearer " + tokenPara("cocina"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
    }
}

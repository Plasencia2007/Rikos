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
import pe.rikos.operacion.catalogo.infraestructura.*;
import pe.rikos.operacion.identidad.infraestructura.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-013 con los roles reales: solo el administrador central programa
 * (menu.crear / modificar / desactivar); mozo, cajero, cocina y
 * administrador de local leen; motorizado no entra. CA6: el menu del dia no
 * se lee sin sesion. Semana en 2032 para no chocar con semanas reales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProgramacionMenuAccesoPorRolTest {

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
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    private static final String LUNES = "2032-03-01";
    private static final String JUEVES = "2032-03-04";

    private String tokenPara(String nombreRol) throws Exception {
        String login = "prgrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(0, 6);
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

    private Producto segundo() {
        CategoriaProducto res = categorias.findById("01M39A0RZ0000000000000S005").orElseThrow();
        UnidadMedida und = unidades.findAll().stream().filter(u -> u.getCodigo().equals("und")).findFirst()
                .orElseThrow();
        String codigo = "PRA-" + Identificador.nuevo().valor().substring(16);
        return productos.save(new Producto(Identificador.nuevo().valor(), codigo, "Ají de gallina " + codigo, res, und,
                true, false, false, 1500L, false, null));
    }

    private JsonNode json(String texto) throws Exception {
        return MAPEADOR.readTree(texto);
    }

    @Test
    void administrador_central_programa_confirma_y_el_pos_lo_ve() throws Exception {
        String token = "Bearer " + tokenPara("administrador central");
        Producto aji = segundo();

        String semana = mvc.perform(post("/menu/semanas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fechaInicio\":\"" + LUNES + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = json(semana).get("id").asText();
        assertThat(json(semana).get("dias")).hasSize(7);

        mvc.perform(post("/menu/semanas/" + id + "/opciones").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"" + JUEVES + "\",\"productoId\":\"" + aji.getId()
                                + "\",\"tipoMenu\":\"normal\",\"porcionesEstimadas\":35}"))
                .andExpect(status().isCreated());

        // Borrador: el POS todavia no lo ve.
        mvc.perform(get("/menu/dias/" + JUEVES).header("Authorization", token)).andExpect(status().isNotFound());

        String confirmada = mvc.perform(post("/menu/semanas/" + id + "/confirmar").header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json(confirmada).get("estado").asText()).isEqualTo("confirmado");

        String delDia = mvc.perform(get("/menu/dias/" + JUEVES).header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json(delDia).get("opciones").get(0).get("componente").asText()).isEqualTo("segundo");
        assertThat(json(delDia).get("opciones").get(0).get("porcionesEstimadas").asInt()).isEqualTo(35);

        // Confirmada: no se descarta.
        mvc.perform(delete("/menu/semanas/" + id).header("Authorization", token)).andExpect(status().isConflict());
    }

    @Test
    void mozo_cajero_cocina_y_administrador_de_local_leen_pero_no_programan() throws Exception {
        for (String rol : new String[] {"mozo", "cajero", "cocina", "administrador de local"}) {
            String token = "Bearer " + tokenPara(rol);
            mvc.perform(get("/menu/semanas").param("lunes", LUNES).header("Authorization", token))
                    .andExpect(status().isNotFound());
            mvc.perform(get("/menu/dias/" + JUEVES).header("Authorization", token)).andExpect(status().isNotFound());
            mvc.perform(post("/menu/semanas").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"fechaInicio\":\"" + LUNES + "\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/menu/semanas/x/opciones").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/menu/semanas/x/confirmar").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/menu/opciones/x").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(delete("/menu/semanas/x").header("Authorization", token)).andExpect(status().isForbidden());
        }
    }

    @Test
    void motorizado_no_ve_la_programacion() throws Exception {
        mvc.perform(get("/menu/semanas").param("lunes", LUNES).header("Authorization", "Bearer " + tokenPara("motorizado")))
                .andExpect(status().isForbidden());
    }

    @Test
    void el_menu_del_dia_no_es_publico() throws Exception {
        mvc.perform(get("/menu/dias/" + JUEVES)).andExpect(status().is4xxClientError());
    }
}

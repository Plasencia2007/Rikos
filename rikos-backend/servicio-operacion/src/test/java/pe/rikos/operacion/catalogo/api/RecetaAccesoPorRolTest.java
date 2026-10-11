package pe.rikos.operacion.catalogo.api;

import com.fasterxml.jackson.databind.JsonNode;
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
import pe.rikos.operacion.catalogo.aplicacion.ComposicionReceta.IngredienteEntrada;
import pe.rikos.operacion.catalogo.aplicacion.CrearReceta;
import pe.rikos.operacion.catalogo.infraestructura.*;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.infraestructura.*;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-010 CA6 con los roles reales del sembrador: el administrador central
 * escribe recetas; el de local y cocina solo las leen; cocina ademas NO ve
 * los costos (no tiene recetas.ver_costos); mozo ni entra.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecetaAccesoPorRolTest {

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
    private CrearReceta crearReceta;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private ProductoRepositorio productos;

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    private String recetaId;

    @BeforeEach
    void crearUnaReceta() {
        String sufijo = Identificador.nuevo().valor().substring(20);
        UnidadMedida kg = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "kr" + sufijo, "Kilo " + sufijo,
                "masa"));
        CategoriaProducto raiz = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), "Acceso recetas " + sufijo, "insumo", null, 1, null));
        CategoriaProducto hoja = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), raiz, "Acceso recetas hoja " + sufijo, 1, null));
        Producto papa = new Producto(Identificador.nuevo().valor(), "AR-P" + sufijo, "Papa " + sufijo, hoja, kg,
                false, true, false, null, true, null);
        papa.registrarCostoManual(new BigDecimal("2.50"), null);
        productos.save(papa);
        Producto pure = productos.save(new Producto(Identificador.nuevo().valor(), "AR-S" + sufijo, "Pure " + sufijo,
                hoja, kg, false, false, true, null, true, null));

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        try {
            recetaId = crearReceta.ejecutar(pure.getId(), BigDecimal.ONE, 4,
                    List.of(new IngredienteEntrada(papa.getId(), BigDecimal.ONE, kg.getId())), ADMIN_SEMBRADO_ID)
                    .valorOLanza().getId();
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
    }

    private String tokenPara(String nombreRol) throws Exception {
        String login = "recrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(0, 6);
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

    private JsonNode detalle(String token) throws Exception {
        String cuerpo = mvc.perform(get("/recetas/" + recetaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return MAPEADOR.readTree(cuerpo);
    }

    private static final String CUERPO_CREAR =
            "{\"productoId\":\"no-existe\",\"rendimiento\":1,\"porciones\":1,\"ingredientes\":[]}";
    private static final String CUERPO_ACTUALIZAR = "{\"rendimiento\":1,\"porciones\":1,\"ingredientes\":[]}";

    @Test
    void administrador_central_ve_costos_y_puede_crear_y_editar() throws Exception {
        String token = tokenPara("administrador central");

        JsonNode receta = detalle(token);
        assertThat(receta.get("costosVisibles").asBoolean()).isTrue();
        assertThat(receta.get("costoTotal").decimalValue()).isEqualByComparingTo("2.50");
        assertThat(receta.get("costoPorPorcion").decimalValue()).isEqualByComparingTo("0.625");

        // Pasa la seguridad y llega al caso de uso: el producto no existe.
        mvc.perform(post("/recetas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREAR))
                .andExpect(status().isNotFound());
        mvc.perform(put("/recetas/" + recetaId).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_ACTUALIZAR))
                .andExpect(status().is(422));
    }

    @Test
    void administrador_de_local_ve_costos_pero_no_escribe_recetas() throws Exception {
        String token = tokenPara("administrador de local");

        assertThat(detalle(token).get("costoTotal").decimalValue()).isEqualByComparingTo("2.50");
        mvc.perform(post("/recetas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREAR))
                .andExpect(status().isForbidden());
        mvc.perform(put("/recetas/" + recetaId).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_ACTUALIZAR))
                .andExpect(status().isForbidden());
    }

    @Test
    void cocina_ve_la_receta_sin_ningun_costo() throws Exception {
        String token = tokenPara("cocina");

        JsonNode receta = detalle(token);
        assertThat(receta.get("costosVisibles").asBoolean()).isFalse();
        assertThat(receta.get("costoTotal").isNull()).isTrue();
        assertThat(receta.get("costoPorPorcion").isNull()).isTrue();
        assertThat(receta.get("costoIncompleto").isNull()).isTrue();
        JsonNode ingrediente = receta.get("ingredientes").get(0);
        assertThat(ingrediente.get("cantidad").decimalValue()).isEqualByComparingTo("1");
        assertThat(ingrediente.get("costoUnitario").isNull()).isTrue();
        assertThat(ingrediente.get("costoLinea").isNull()).isTrue();

        String lista = mvc.perform(get("/recetas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (JsonNode resumen : MAPEADOR.readTree(lista)) {
            assertThat(resumen.get("costoPorPorcion").isNull()).isTrue();
        }

        mvc.perform(post("/recetas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREAR))
                .andExpect(status().isForbidden());
    }

    @Test
    void mozo_no_puede_ni_ver_recetas() throws Exception {
        mvc.perform(get("/recetas").header("Authorization", "Bearer " + tokenPara("mozo")))
                .andExpect(status().isForbidden());
    }
}

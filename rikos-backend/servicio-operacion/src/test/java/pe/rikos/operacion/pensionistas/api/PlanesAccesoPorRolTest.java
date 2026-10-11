package pe.rikos.operacion.pensionistas.api;

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
import pe.rikos.operacion.identidad.infraestructura.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 5, entrega 1, con los roles reales. Solo el administrador central
 * crea planes y cambia parametros; el de local, cajero y mozo los ven (caja
 * los necesita para cobrar); cocina y motorizado no entran.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanesAccesoPorRolTest {

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

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    private static String cuerpoPlan() {
        return "{\"nombre\":\"Plan prueba " + Identificador.nuevo().valor().substring(16)
                + "\",\"precioDesayuno\":500,\"precioAlmuerzo\":800,\"precioCena\":800}";
    }

    private static final String CAMBIO = "{\"precioDesayuno\":500,\"precioAlmuerzo\":900,\"precioCena\":800}";
    private static final String TAPER = "{\"valor\":\"100\"}";

    private String tokenPara(String nombreRol) throws Exception {
        String login = "plnrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(18);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", nombreRol,
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals(nombreRol)).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));

        String cuerpo = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario.getNombreUsuario()
                                + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + MAPEADOR.readTree(cuerpo).get("token").asText();
    }

    @Test
    void administrador_central_crea_planes_cambia_precios_y_parametros() throws Exception {
        String token = tokenPara("administrador central");

        String creado = mvc.perform(post("/planes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoPlan()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode plan = MAPEADOR.readTree(creado);
        assertThat(plan.get("versiones")).hasSize(1);
        assertThat(plan.get("versiones").get(0).get("estado").asText()).isEqualTo("vigente");

        // Hoy mismo: corrige la version que empezo hoy, sin crear otra.
        String cambiado = mvc.perform(put("/planes/" + plan.get("id").asText() + "/precios")
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(CAMBIO))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(MAPEADOR.readTree(cambiado).get("versiones").get(0).get("precioAlmuerzo").asLong()).isEqualTo(900L);

        mvc.perform(put("/parametros/taper_precio").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(TAPER))
                .andExpect(status().isOk());
        String parametros = mvc.perform(get("/parametros").header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(MAPEADOR.readTree(parametros).get("taperPrecio").asLong()).isEqualTo(100L);

        mvc.perform(put("/parametros/no_existe").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(TAPER))
                .andExpect(status().isNotFound());
        mvc.perform(put("/parametros/taper_precio").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":\"abc\"}"))
                .andExpect(status().is(422));
    }

    @Test
    void administrador_de_local_cajero_y_mozo_ven_pero_no_cambian_nada() throws Exception {
        for (String rol : new String[] {"administrador de local", "cajero", "mozo"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/planes").header("Authorization", token)).andExpect(status().isOk());
            mvc.perform(get("/parametros").header("Authorization", token)).andExpect(status().isOk());
            mvc.perform(post("/planes").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoPlan()))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/planes/x/precios").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(CAMBIO))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/parametros/taper_precio").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(TAPER))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void el_motor_de_precios_lo_consultan_quienes_ven_pensionistas_y_cocina_y_motorizado_no() throws Exception {
        String consulta = "{\"tipo\":\"menu_del_dia\",\"comida\":\"almuerzo\",\"precioNormal\":1200,"
                + "\"segundoDelDia\":false,\"paraLlevar\":false}";
        for (String rol : new String[] {"administrador central", "administrador de local", "cajero", "mozo"}) {
            String respuesta = mvc.perform(post("/precios/calculo").header("Authorization", tokenPara(rol))
                            .contentType(MediaType.APPLICATION_JSON).content(consulta))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(MAPEADOR.readTree(respuesta).get("precio").asLong()).isEqualTo(1200L);
        }
        for (String rol : new String[] {"cocina", "motorizado"}) {
            mvc.perform(post("/precios/calculo").header("Authorization", tokenPara(rol))
                            .contentType(MediaType.APPLICATION_JSON).content(consulta))
                    .andExpect(status().isForbidden());
        }
        // Datos que no son: 422, no 500.
        mvc.perform(post("/precios/calculo").header("Authorization", tokenPara("cajero"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"gratis\",\"comida\":\"almuerzo\"}"))
                .andExpect(status().is(422));
        mvc.perform(post("/precios/calculo").header("Authorization", tokenPara("cajero"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"menu_del_dia\",\"comida\":\"almuerzo\",\"planId\":\"no-existe\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cocina_y_motorizado_no_ven_ni_los_planes_ni_los_parametros() throws Exception {
        for (String rol : new String[] {"cocina", "motorizado"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/planes").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(get("/parametros").header("Authorization", token)).andExpect(status().isForbidden());
        }
    }
}

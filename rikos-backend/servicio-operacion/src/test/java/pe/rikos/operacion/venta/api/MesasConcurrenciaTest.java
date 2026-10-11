package pe.rikos.operacion.venta.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.identidad.infraestructura.*;
import pe.rikos.operacion.venta.aplicacion.ActualizarMesa;
import pe.rikos.operacion.venta.aplicacion.BloquearMesa;
import pe.rikos.operacion.venta.aplicacion.CambiarEstadoMesa;
import pe.rikos.operacion.venta.aplicacion.CrearMesa;
import pe.rikos.operacion.venta.aplicacion.LiberarMesa;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dos personas actuando a la vez sobre la misma mesa: el segundo en guardar choca con la
 * version (@Version) o con la restriccion unica del numero. Tiene que ser un 409 claro, no un 500.
 * Los casos de uso se sustituyen por dobles que lanzan lo que lanzaria la base en la carrera real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MesasConcurrenciaTest {

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @MockitoBean
    CrearMesa crearMesa;
    @MockitoBean
    ActualizarMesa actualizarMesa;
    @MockitoBean
    CambiarEstadoMesa cambiarEstadoMesa;
    @MockitoBean
    BloquearMesa bloquearMesa;
    @MockitoBean
    LiberarMesa liberarMesa;

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuarioRepositorio usuarios;
    @Autowired
    RolRepositorio roles;
    @Autowired
    UsuarioRolRepositorio usuarioRoles;
    @Autowired
    UsuarioLocalRepositorio usuarioLocales;
    @Autowired
    LocalRepositorio locales;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    CrearLocal crearLocal;

    String token;

    @BeforeEach
    void preparar() throws Exception {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        Local local;
        try {
            String codigo = "mc" + Identificador.nuevo().valor().substring(20).toLowerCase();
            local = locales.findById(crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID)
                    .valorOLanza().getId()).orElseThrow();
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
        String login = "mesconc-" + Identificador.nuevo().valor().substring(18);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", "admin local",
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals("administrador de local"))
                .findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));
        usuarioLocales.save(new UsuarioLocal(usuario, local, true, ADMIN_SEMBRADO_ID));
        String respuesta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + login + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        token = "Bearer " + MAPEADOR.readTree(respuesta).get("token").asText();
    }

    private static ObjectOptimisticLockingFailureException choqueDeVersion() {
        return new ObjectOptimisticLockingFailureException(pe.rikos.operacion.venta.infraestructura.Mesa.class, "x");
    }

    @Test
    void dos_altas_con_el_mismo_numero_a_la_vez_dan_409_numero_duplicado() throws Exception {
        when(crearMesa.ejecutar(any(), anyString())).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"mesa_numero_unico_por_local\""));

        mvc.perform(post("/mesas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("NUMERO_DUPLICADO"));
    }

    @Test
    void dos_mozos_bloqueando_la_misma_mesa_a_la_vez_dan_409_mesa_modificada() throws Exception {
        when(bloquearMesa.ejecutar(anyString(), anyString(), anyString())).thenThrow(choqueDeVersion());

        mvc.perform(post("/mesas/una-mesa/bloquear").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Reservada\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("MESA_MODIFICADA"));
    }

    @Test
    void liberar_editar_y_activar_a_la_vez_tambien_dan_409_mesa_modificada() throws Exception {
        when(liberarMesa.ejecutar(anyString(), anyString())).thenThrow(choqueDeVersion());
        when(actualizarMesa.ejecutar(anyString(), any(), anyString())).thenThrow(choqueDeVersion());
        when(cambiarEstadoMesa.ejecutar(anyString(), anyBoolean(), anyString())).thenThrow(choqueDeVersion());

        mvc.perform(post("/mesas/una-mesa/liberar").header("Authorization", token))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("MESA_MODIFICADA"));
        mvc.perform(put("/mesas/una-mesa").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"1\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("MESA_MODIFICADA"));
        mvc.perform(post("/mesas/una-mesa/desactivar").header("Authorization", token))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("MESA_MODIFICADA"));
        mvc.perform(post("/mesas/una-mesa/activar").header("Authorization", token))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("MESA_MODIFICADA"));
    }

    @Test
    void otra_violacion_de_integridad_no_se_disfraza_de_numero_duplicado() throws Exception {
        when(crearMesa.ejecutar(any(), anyString())).thenThrow(new DataIntegrityViolationException(
                "null value in column \"local_id\" violates not-null constraint"));

        mvc.perform(post("/mesas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"1\"}"))
                .andExpect(status().is5xxServerError());
    }
}

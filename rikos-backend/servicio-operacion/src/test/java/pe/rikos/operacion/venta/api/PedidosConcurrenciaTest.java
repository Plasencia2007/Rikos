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
import pe.rikos.operacion.venta.aplicacion.AbrirPedido;
import pe.rikos.operacion.venta.aplicacion.AgregarLinea;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.venta.aplicacion.ErrorPedido;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dos personas actuando a la vez sobre el mismo pedido o la misma mesa: el segundo en guardar choca con la
 * version o con una restriccion unica. Tiene que ser un 409 claro, no un 500 (igual que las mesas).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PedidosConcurrenciaTest {

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @MockitoBean
    AbrirPedido abrirPedido;
    @MockitoBean
    AgregarLinea agregarLinea;

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
    @Autowired
    pe.rikos.operacion.venta.infraestructura.PedidoRepositorio pedidos;
    @Autowired
    pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio personas;

    String token;
    Local local;

    @BeforeEach
    void preparar() throws Exception {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        try {
            String codigo = "pc" + Identificador.nuevo().valor().substring(20).toLowerCase();
            local = locales.findById(crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID)
                    .valorOLanza().getId()).orElseThrow();
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
        String login = "pedconc-" + Identificador.nuevo().valor().substring(18);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", "mozo",
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals("mozo")).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));
        usuarioLocales.save(new UsuarioLocal(usuario, local, true, ADMIN_SEMBRADO_ID));
        String respuesta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + login + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        token = "Bearer " + MAPEADOR.readTree(respuesta).get("token").asText();
    }

    private static final String ABRIR = "{\"canal\":\"para_llevar\",\"servicio\":\"almuerzo\"}";
    private static final String LINEA = "{\"personaId\":\"p\",\"tipo\":\"producto\",\"productoId\":\"x\",\"cantidad\":1}";

    @Test
    void dos_mozos_abriendo_a_la_vez_dan_409_numero_de_pedido_repetido() throws Exception {
        when(abrirPedido.ejecutar(any(), any(), any(), any(), anyInt(), any(), anyString())).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"pedido_numero_unico\""));

        mvc.perform(post("/pedidos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(ABRIR))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("NUMERO_DE_PEDIDO_REPETIDO"));
    }

    @Test
    void dos_mozos_abriendo_la_misma_mesa_a_la_vez_dan_409_mesa_ocupada() throws Exception {
        when(abrirPedido.ejecutar(any(), any(), any(), any(), anyInt(), any(), anyString())).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"ux_pedido_mesa_en_curso\""));

        mvc.perform(post("/pedidos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"mesa\",\"mesaId\":\"m\",\"servicio\":\"almuerzo\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("MESA_OCUPADA"));
    }

    @Test
    void un_choque_de_version_al_agregar_una_linea_da_409_pedido_modificado() throws Exception {
        when(agregarLinea.ejecutar(any(), anyString())).thenThrow(
                new ObjectOptimisticLockingFailureException(Pedido.class, "x"));

        mvc.perform(post("/pedidos/un-pedido/lineas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(LINEA))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("PEDIDO_MODIFICADO"));
    }

    @Test
    void otra_violacion_de_integridad_no_se_disfraza() throws Exception {
        when(abrirPedido.ejecutar(any(), any(), any(), any(), anyInt(), any(), anyString())).thenThrow(new DataIntegrityViolationException(
                "null value in column \"local_id\" violates not-null constraint"));

        mvc.perform(post("/pedidos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(ABRIR))
                .andExpect(status().is5xxServerError());
    }

    private Pedido pedidoReal() {
        Pedido real = pedidos.save(new Pedido(Identificador.nuevo().valor(), local.getId(), "0001",
                java.time.LocalDate.now(), "para_llevar", "almuerzo", null, ADMIN_SEMBRADO_ID));
        personas.save(new pe.rikos.operacion.venta.infraestructura.CuentaPersona(Identificador.nuevo().valor(),
                real.getId(), "Persona 1", 1));
        return real;
    }

    @Test
    void un_numero_repetido_se_reintenta_y_a_la_segunda_abre_el_pedido() throws Exception {
        Pedido real = pedidoReal();
        when(abrirPedido.ejecutar(any(), any(), any(), any(), anyInt(), any(), anyString())).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"pedido_numero_unico\""))
                .thenReturn(Resultado.<Pedido, ErrorPedido>exito(real));

        mvc.perform(post("/pedidos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(ABRIR))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(real.getId()));
    }

    @Test
    void un_reenvio_concurrente_del_mismo_id_se_reintenta_y_devuelve_el_pedido_que_ya_existe() throws Exception {
        Pedido real = pedidoReal();
        when(abrirPedido.ejecutar(any(), any(), any(), any(), anyInt(), any(), anyString())).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"pedido_pkey\""))
                .thenReturn(Resultado.<Pedido, ErrorPedido>exito(real));

        mvc.perform(post("/pedidos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + real.getId() + "\",\"canal\":\"para_llevar\",\"servicio\":\"almuerzo\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(real.getId()));
    }

    @Test
    void dos_aperturas_a_la_vez_sobre_la_misma_mesa_la_perdedora_se_reintenta_y_entra_al_pedido_que_gano() throws Exception {
        Pedido real = pedidoReal();
        when(abrirPedido.ejecutar(any(), any(), any(), any(), anyInt(), any(), anyString())).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"ux_pedido_mesa_en_curso\""))
                .thenReturn(Resultado.<Pedido, ErrorPedido>exito(real));

        mvc.perform(post("/pedidos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"mesa\",\"mesaId\":\"m\",\"servicio\":\"almuerzo\"}"))
                .andExpect(status().is2xxSuccessful()).andExpect(jsonPath("$.id").value(real.getId()));
    }
}

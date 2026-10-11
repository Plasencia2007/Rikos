package pe.rikos.operacion.pensionistas.api;

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
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.identidad.infraestructura.*;
import pe.rikos.operacion.pensionistas.aplicacion.Comida;
import pe.rikos.operacion.pensionistas.aplicacion.CrearPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.CrearPlanTarifario;
import pe.rikos.operacion.pensionistas.aplicacion.DatosPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.DatosResponsable;
import pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-018 con los roles reales. Administrador central: todo, incluido el saldo
 * inicial. Administrador de local: ve, ajusta y fija el tope, pero el saldo
 * inicial no. Cajero y mozo solo ven la cuenta. Cocina y motorizado no entran.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CuentaAccesoPorRolTest {

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    private static final String MOTIVO_AJUSTE_SEMBRADO_ID = "01M39A0MT0000000000000D002";

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
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CrearPlanTarifario crearPlan;
    @Autowired
    private CrearPensionista crearPensionista;
    @Autowired
    private CrearLocal crearLocal;

    private String pensionistaId;
    private Local local;

    @BeforeEach
    void preparar() {
        // Crear el plan, el local y el pensionista se audita: hace falta un usuario en el contexto solo aqui.
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        try {
            String codigo = "cu" + Identificador.nuevo().valor().substring(20).toLowerCase();
            local = locales.findById(crearLocal.ejecutar(codigo, "Local " + codigo, "Dirección", ADMIN_SEMBRADO_ID)
                    .valorOLanza().getId()).orElseThrow();
            String planId = crearPlan.ejecutar("Plan cuenta " + Identificador.nuevo().valor().substring(16), null,
                    new PreciosPlan(500L, 800L, 800L), null, ADMIN_SEMBRADO_ID).valorOLanza().getId();
            String dni = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
            var datos = new DatosPensionista(dni, "Ana", "Cuenta", null, null, null, null, null, null, planId,
                    Set.of(Comida.ALMUERZO), null,
                    new DatosResponsable("Rosa Cuenta", "12345678", "999", "r@c.pe", "Madre", "Av. 1"));
            pensionistaId = crearPensionista.ejecutar(datos, ADMIN_SEMBRADO_ID).valorOLanza().pensionista().getId();
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
    }

    private String tokenPara(String nombreRol) throws Exception {
        String login = "cuenrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(18);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", nombreRol,
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals(nombreRol)).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));
        usuarioLocales.save(new UsuarioLocal(usuario, local, true, ADMIN_SEMBRADO_ID));

        String respuesta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario.getNombreUsuario()
                                + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + MAPEADOR.readTree(respuesta).get("token").asText();
    }

    private static String saldoInicial() {
        return "{\"importe\":5000,\"signo\":\"cargo\",\"descripcion\":\"Cuaderno\"}";
    }

    private static String ajuste() {
        return "{\"importe\":300,\"signo\":\"credito\",\"motivoId\":\"" + MOTIVO_AJUSTE_SEMBRADO_ID
                + "\",\"observacion\":\"Cobro de más\"}";
    }

    @Test
    void administrador_central_hace_todo_y_ve_el_libro_con_el_local_de_cada_movimiento() throws Exception {
        String token = tokenPara("administrador central");

        mvc.perform(post("/pensionistas/" + pensionistaId + "/cuenta/saldo-inicial").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(saldoInicial()))
                .andExpect(status().isCreated());
        mvc.perform(post("/pensionistas/" + pensionistaId + "/cuenta/ajustes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(ajuste()))
                .andExpect(status().isCreated());
        mvc.perform(put("/pensionistas/" + pensionistaId + "/tope").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"limite\":10000}"))
                .andExpect(status().isOk());

        String cuerpo = mvc.perform(get("/pensionistas/" + pensionistaId + "/cuenta").header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode cuenta = MAPEADOR.readTree(cuerpo);
        assertThat(cuenta.get("resumen").get("deuda").asLong()).isEqualTo(4700L);
        assertThat(cuenta.get("resumen").get("tope").get("origen").asText()).isEqualTo("fijado");
        assertThat(cuenta.get("resumen").get("tope").get("monto").asLong()).isEqualTo(10000L);
        assertThat(cuenta.get("resumen").get("superaTope").asBoolean()).isFalse();
        assertThat(cuenta.get("movimientos")).hasSize(2);
        assertThat(cuenta.get("movimientos").get(0).get("localNombre").asText()).isEqualTo(local.getNombre());
        assertThat(cuenta.get("movimientos").get(0).get("motivo").asText()).isNotBlank();
    }

    @Test
    void administrador_de_local_ajusta_y_fija_tope_pero_no_carga_el_saldo_inicial() throws Exception {
        String token = tokenPara("administrador de local");

        mvc.perform(post("/pensionistas/" + pensionistaId + "/cuenta/saldo-inicial").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(saldoInicial()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/pensionistas/" + pensionistaId + "/cuenta/ajustes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(ajuste()))
                .andExpect(status().isCreated());
        mvc.perform(put("/pensionistas/" + pensionistaId + "/tope").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"limite\":null}"))
                .andExpect(status().isOk());
        mvc.perform(get("/pensionistas/" + pensionistaId + "/cuenta").header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    void cajero_y_mozo_ven_la_cuenta_pero_no_la_tocan() throws Exception {
        for (String rol : new String[] {"cajero", "mozo"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/pensionistas/" + pensionistaId + "/cuenta").header("Authorization", token))
                    .andExpect(status().isOk());
            mvc.perform(post("/pensionistas/" + pensionistaId + "/cuenta/saldo-inicial")
                            .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                            .content(saldoInicial()))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/pensionistas/" + pensionistaId + "/cuenta/ajustes").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(ajuste()))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/pensionistas/" + pensionistaId + "/tope").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"limite\":100}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void cocina_y_motorizado_no_ven_la_cuenta() throws Exception {
        for (String rol : new String[] {"cocina", "motorizado"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/pensionistas/" + pensionistaId + "/cuenta").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void los_errores_dan_codigos_claros() throws Exception {
        String token = tokenPara("administrador central");
        mvc.perform(get("/pensionistas/no-existe/cuenta").header("Authorization", token))
                .andExpect(status().isNotFound());
        mvc.perform(post("/pensionistas/" + pensionistaId + "/cuenta/saldo-inicial").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"importe\":100,\"signo\":\"mitad\"}"))
                .andExpect(status().is(422));
        mvc.perform(post("/pensionistas/" + pensionistaId + "/cuenta/saldo-inicial").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(saldoInicial()))
                .andExpect(status().isCreated());
        // Una sola vez por cuenta: 409.
        mvc.perform(post("/pensionistas/" + pensionistaId + "/cuenta/saldo-inicial").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(saldoInicial()))
                .andExpect(status().isConflict());
        // Ajuste sin motivo: 422.
        mvc.perform(post("/pensionistas/" + pensionistaId + "/cuenta/ajustes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"importe\":100,\"signo\":\"cargo\"}"))
                .andExpect(status().is(422));
        // Tope en cero: 422.
        mvc.perform(put("/pensionistas/" + pensionistaId + "/tope").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"limite\":0}"))
                .andExpect(status().is(422));
    }

    @Test
    void la_lista_de_cuentas_la_ven_quienes_ven_pensionistas() throws Exception {
        for (String rol : new String[] {"administrador central", "administrador de local", "cajero", "mozo"}) {
            String cuerpo = mvc.perform(get("/pensionistas/cuentas").header("Authorization", tokenPara(rol)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            JsonNode lista = MAPEADOR.readTree(cuerpo);
            assertThat(lista.isArray()).isTrue();
            assertThat(lista.findValuesAsText("pensionistaId")).contains(pensionistaId);
            // Nunca el PIN ni su hash.
            assertThat(cuerpo.toLowerCase()).doesNotContain("pin").doesNotContain("hash");
        }
        for (String rol : new String[] {"cocina", "motorizado"}) {
            mvc.perform(get("/pensionistas/cuentas").header("Authorization", tokenPara(rol)))
                    .andExpect(status().isForbidden());
        }
    }
}

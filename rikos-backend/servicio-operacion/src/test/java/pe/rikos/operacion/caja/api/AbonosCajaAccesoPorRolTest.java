package pe.rikos.operacion.caja.api;

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
import pe.rikos.operacion.identidad.infraestructura.*;
import pe.rikos.operacion.pensionistas.aplicacion.Comida;
import pe.rikos.operacion.pensionistas.aplicacion.CrearPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.CrearPlanTarifario;
import pe.rikos.operacion.pensionistas.aplicacion.DatosPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.DatosResponsable;
import pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.venta.aplicacion.PedidosBase;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AbonosCajaAccesoPorRolTest extends PedidosBase {

    private static final ObjectMapper MAPEADOR = new ObjectMapper();

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
    CrearPlanTarifario crearPlan;
    @Autowired
    CrearPensionista crearPensionista;

    Local local;
    Pensionista pensionista;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        try {
            local = locales.findById(nuevoLocal("ab")).orElseThrow();
            ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, local.getId());
            String planId = crearPlan.ejecutar("Plan Test Abonos REST " + Identificador.nuevo().valor().substring(18), null,
                    new PreciosPlan(800L, 1200L, 1000L), null, ADMIN_SEMBRADO_ID).valorOLanza().getId();
            pensionista = crearPensionista.ejecutar(new DatosPensionista("12345678", "Estudiante", "REST",
                    "987654321", "alumno@correo.com", "UNI", "20240002", "Sistemas", "3", planId,
                    Set.of(Comida.ALMUERZO), "semanal", new DatosResponsable("Padre Test", "11223344", "999888777",
                    "padre@correo.com", "Padre", "Av. Central 456")), ADMIN_SEMBRADO_ID).valorOLanza().pensionista();
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
    }

    String tokenPara(String nombreRol) throws Exception {
        String login = "abono-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(18);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", nombreRol,
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals(nombreRol)).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));
        usuarioLocales.save(new UsuarioLocal(usuario, local, true, ADMIN_SEMBRADO_ID));
        String respuesta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + login + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + MAPEADOR.readTree(respuesta).get("token").asText();
    }

    String abrirTurno(String token) throws Exception {
        JsonNode cajas = MAPEADOR.readTree(mvc.perform(get("/cajas").header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String cajaId = cajas.get(0).get("id").asText();
        mvc.perform(post("/cajas/" + cajaId + "/turnos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fondoInicial\":10000}"))
                .andExpect(status().isCreated());
        return cajaId;
    }

    @Test
    void cajero_con_turno_abierto_registra_abono_y_lo_lista() throws Exception {
        String tokenCajero = tokenPara("cajero");
        abrirTurno(tokenCajero);

        // 1. Registrar abono en efectivo
        String body = """
                {
                    "pensionistaId": "%s",
                    "medio": "efectivo",
                    "monto": 5000,
                    "observacion": "Abono REST efectivo"
                }
                """.formatted(pensionista.getId());

        mvc.perform(post("/turnos/actual/abonos")
                        .header("Authorization", tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.abono.monto").value(5000))
                .andExpect(jsonPath("$.abono.estado").value("aprobado"))
                .andExpect(jsonPath("$.pensionistaDni").value("12345678"))
                .andExpect(jsonPath("$.constancia.leyenda").value("Constancia interna de abono a cuenta corriente. No constituye comprobante de pago."));

        // 2. Registrar abono digital pendiente
        String bodyDigital = """
                {
                    "pensionistaId": "%s",
                    "medio": "yape",
                    "monto": 3500,
                    "numeroOperacion": "998877",
                    "cuentaDestino": "Yape RIKO'S",
                    "observacion": "Abono REST yape"
                }
                """.formatted(pensionista.getId());

        mvc.perform(post("/turnos/actual/abonos")
                        .header("Authorization", tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyDigital))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.abono.monto").value(3500))
                .andExpect(jsonPath("$.abono.estado").value("pendiente"))
                .andExpect(jsonPath("$.abono.numeroOperacion").value("998877"));

        // 3. Listar abonos del turno
        mvc.perform(get("/turnos/actual/abonos")
                        .header("Authorization", tokenCajero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].pensionistaDni").value("12345678"));
    }

    @Test
    void mozo_no_puede_registrar_abonos() throws Exception {
        String tokenMozo = tokenPara("mozo");

        String body = """
                {
                    "pensionistaId": "%s",
                    "medio": "efectivo",
                    "monto": 5000
                }
                """.formatted(pensionista.getId());

        mvc.perform(post("/turnos/actual/abonos")
                        .header("Authorization", tokenMozo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}

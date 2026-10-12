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
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.infraestructura.EmpresaRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.RolRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocal;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRol;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;
import pe.rikos.operacion.pensionistas.aplicacion.CargarSaldoInicial;
import pe.rikos.operacion.pensionistas.aplicacion.Comida;
import pe.rikos.operacion.pensionistas.aplicacion.CrearPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.CrearPlanTarifario;
import pe.rikos.operacion.pensionistas.aplicacion.DatosPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.DatosResponsable;
import pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan;
import pe.rikos.operacion.pensionistas.aplicacion.Signo;
import pe.rikos.operacion.pensionistas.infraestructura.Abono;
import pe.rikos.operacion.pensionistas.infraestructura.AbonoRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.venta.aplicacion.PedidosBase;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AbonoControllerAccesoTest extends PedidosBase {

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
    AbonoRepositorio abonos;
    @Autowired
    MotivoRepositorio motivos;
    @Autowired
    CargarSaldoInicial cargarSaldoInicial;
    @Autowired
    CrearPlanTarifario crearPlan;
    @Autowired
    CrearPensionista crearPensionista;

    Local local;
    Pensionista pensionista;
    Motivo motivoRechazo;

    @BeforeEach
    void setup() {
        local = locales.findAll().get(0);
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, local.getId());

        String planId = crearPlan.ejecutar("Plan Test Abono Controller " + Identificador.nuevo().valor().substring(18), null,
                new PreciosPlan(800L, 1200L, 1000L), null, ADMIN_SEMBRADO_ID).valorOLanza().getId();

        pensionista = crearPensionista.ejecutar(new DatosPensionista("55667788", "Pensionista", "Prueba",
                "987654321", "pensionista.ctrl@correo.com", "UNI", "20240055", "Sistemas", "4", planId,
                Set.of(Comida.ALMUERZO), "semanal", new DatosResponsable("Padre Ctrl", "11223344", "999888777",
                "padre.ctrl@correo.com", "Padre", "Av. Central 123")), ADMIN_SEMBRADO_ID).valorOLanza().pensionista();

        cargarSaldoInicial.ejecutar(pensionista.getId(), 10000L, Signo.CARGO, LocalDate.now(),
                "Deuda inicial", ADMIN_SEMBRADO_ID).valorOLanza();

        motivoRechazo = motivos.findById("01M49A0MT0000000000000R001").orElseGet(() ->
                motivos.save(new Motivo("01M49A0MT0000000000000R001", "rechazo_abono",
                        "Operación no encontrada en cuenta bancaria", false)));
    }

    private Abono crearPendiente(long monto, String op) {
        Abono a = Abono.digitalPendiente(
                Identificador.nuevo().valor(),
                pensionista.getId(),
                local.getId(),
                Abono.MEDIO_YAPE,
                monto,
                LocalDate.now(),
                op,
                "Yape RIKO'S",
                null,
                Abono.ORIGEN_CAJA,
                ADMIN_SEMBRADO_ID,
                "Abono prueba api",
                OffsetDateTime.now()
        );
        return abonos.save(a);
    }

    private String tokenPara(String nombreRol) throws Exception {
        String uid = Identificador.nuevo().valor();
        String passPlana = "clave123";
        String login = "usr_" + nombreRol.replace(" ", "") + "_" + uid.substring(20).toLowerCase();
        Usuario u = usuarios.save(new Usuario(uid, login, "Usuario", nombreRol,
                null, null, null, passwordEncoder.encode(passPlana), null));

        Rol r = roles.findAll().stream().filter(rol -> rol.getNombre().equals(nombreRol)).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(u, r, ADMIN_SEMBRADO_ID));
        usuarioLocales.save(new UsuarioLocal(u, local, true, ADMIN_SEMBRADO_ID));

        String loginBody = """
                {
                    "nombreUsuario": "%s",
                    "contrasena": "%s"
                }
                """.formatted(u.getNombreUsuario(), passPlana);

        var resLogin = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = MAPEADOR.readTree(resLogin.getResponse().getContentAsString());
        return "Bearer " + json.get("token").asText();
    }

    @Test
    void admin_central_puede_listar_aprobar_y_rechazar_abonos_digitales() throws Exception {
        String tokenAdmin = tokenPara("administrador central");

        Abono abono1 = crearPendiente(4000L, "OP-APROB-1");
        Abono abono2 = crearPendiente(2500L, "OP-RECH-2");

        // 1. Listar pendientes
        mvc.perform(get("/pensionistas/abonos/pendientes")
                        .header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 2. Conteo de pendientes
        mvc.perform(get("/pensionistas/abonos/conteo-pendientes")
                        .header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendientes").isNumber());

        // 3. Aprobar abono 1
        mvc.perform(post("/pensionistas/abonos/" + abono1.getId() + "/aprobar")
                        .header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abono.estado").value("aprobado"))
                .andExpect(jsonPath("$.nuevoSaldo").value(6000)); // 10000 - 4000 = 6000

        // 4. Rechazar abono 2 con motivo
        String bodyRechazo = """
                {
                    "motivoId": "%s",
                    "observacion": "No aparece en el extracto"
                }
                """.formatted(motivoRechazo.getId());

        mvc.perform(post("/pensionistas/abonos/" + abono2.getId() + "/rechazar")
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyRechazo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("rechazado"))
                .andExpect(jsonPath("$.motivoRechazoId").value(motivoRechazo.getId()));
    }

    @Test
    void cajero_no_puede_aprobar_ni_rechazar_abonos_digitales() throws Exception {
        String tokenCajero = tokenPara("cajero");
        Abono abono = crearPendiente(3000L, "OP-CAJ-TEST");

        // Intento de aprobación por cajero -> 403 Forbidden (CA1)
        mvc.perform(post("/pensionistas/abonos/" + abono.getId() + "/aprobar")
                        .header("Authorization", tokenCajero))
                .andExpect(status().isForbidden());

        // Intento de rechazo por cajero -> 403 Forbidden (CA1)
        String bodyRechazo = """
                {
                    "motivoId": "%s"
                }
                """.formatted(motivoRechazo.getId());

        mvc.perform(post("/pensionistas/abonos/" + abono.getId() + "/rechazar")
                        .header("Authorization", tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyRechazo))
                .andExpect(status().isForbidden());
    }
}

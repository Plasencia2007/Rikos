package pe.rikos.operacion.pensionistas.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.pensionistas.aplicacion.EmitirPreTicket;
import pe.rikos.operacion.pensionistas.aplicacion.ErrorPreTicket;
import pe.rikos.operacion.pensionistas.aplicacion.FalloPreTicket;
import pe.rikos.operacion.pensionistas.aplicacion.PreTicketEmitido;
import pe.rikos.operacion.pensionistas.aplicacion.SolicitudEmitirPreTicket;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KioscoControllerTest {

    private static final ObjectMapper MAPEADOR = new ObjectMapper();

    @Mock
    private EmitirPreTicket emitirPreTicket;

    @InjectMocks
    private KioscoController controller;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void emitePreTicketExitosoRetorna200() throws Exception {
        OffsetDateTime ahora = OffsetDateTime.now();
        PreTicketEmitido emitido = new PreTicketEmitido(
                "ticket-1", "381920", "pen-1", "Carlos Mendoza Ramos", "72345678",
                "local-1", "San Isidro", "almuerzo", LocalDate.now(), ahora, ahora.plusHours(4), false
        );

        when(emitirPreTicket.ejecutar(any(SolicitudEmitirPreTicket.class)))
                .thenReturn(Resultado.exito(emitido));

        KioscoController.EmitirPreTicketRequest request =
                new KioscoController.EmitirPreTicketRequest("sanisidro", "72345678", "1234", null);

        mvc.perform(post("/kiosco/pre-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPEADOR.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("381920"))
                .andExpect(jsonPath("$.pensionistaNombre").value("Carlos Mendoza Ramos"))
                .andExpect(jsonPath("$.servicio").value("almuerzo"))
                .andExpect(jsonPath("$.reutilizado").value(false))
                .andExpect(jsonPath("$.mensajeLegal").isNotEmpty());
    }

    @Test
    void credencialesInvalidasRetorna401ConIntentosRestantes() throws Exception {
        when(emitirPreTicket.ejecutar(any(SolicitudEmitirPreTicket.class)))
                .thenReturn(Resultado.fracaso(FalloPreTicket.credencialesInvalidas(3)));

        KioscoController.EmitirPreTicketRequest request =
                new KioscoController.EmitirPreTicketRequest("sanisidro", "72345678", "9999", null);

        mvc.perform(post("/kiosco/pre-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPEADOR.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("CREDENCIALES_INVALIDAS"))
                .andExpect(jsonPath("$.intentosRestantes").value(3));
    }

    @Test
    void pensionistaBloqueadoRetorna423ConSegundosRestantes() throws Exception {
        when(emitirPreTicket.ejecutar(any(SolicitudEmitirPreTicket.class)))
                .thenReturn(Resultado.fracaso(FalloPreTicket.bloqueado(840L)));

        KioscoController.EmitirPreTicketRequest request =
                new KioscoController.EmitirPreTicketRequest("sanisidro", "72345678", "1234", null);

        mvc.perform(post("/kiosco/pre-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPEADOR.writeValueAsString(request)))
                .andExpect(status().is(423))
                .andExpect(jsonPath("$.error").value("PENSIONISTA_BLOQUEADO"))
                .andExpect(jsonPath("$.segundosRestantes").value(840));
    }

    @Test
    void fueraDeHorarioRetorna409Conflict() throws Exception {
        when(emitirPreTicket.ejecutar(any(SolicitudEmitirPreTicket.class)))
                .thenReturn(Resultado.fracaso(FalloPreTicket.de(ErrorPreTicket.FUERA_DE_HORARIO)));

        KioscoController.EmitirPreTicketRequest request =
                new KioscoController.EmitirPreTicketRequest("sanisidro", "72345678", "1234", null);

        mvc.perform(post("/kiosco/pre-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPEADOR.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("FUERA_DE_HORARIO"));
    }
}

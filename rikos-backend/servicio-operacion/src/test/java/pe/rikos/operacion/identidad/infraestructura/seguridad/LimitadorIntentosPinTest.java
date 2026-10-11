package pe.rikos.operacion.identidad.infraestructura.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.identidad.infraestructura.Empresa;
import pe.rikos.operacion.identidad.infraestructura.EmpresaRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DT-039: el contador debe sobrevivir a un "reinicio" (una nueva instancia
 * del limitador leyendo la misma fila en la base, no un mapa en memoria que
 * se perdería) y debe distinguir dispositivos del mismo local.
 */
@SpringBootTest
@Transactional
class LimitadorIntentosPinTest {

    @Autowired
    private LimitadorIntentosPin limitador;
    @Autowired
    private BloqueoPinRepositorio bloqueos;
    @Autowired
    private LocalRepositorio locales;
    @Autowired
    private EmpresaRepositorio empresas;
    @Autowired
    private PropiedadesSeguridad propiedades;

    private String crearLocal() {
        Empresa empresa = empresas.findAll().get(0);
        String codigo = "lim-" + Identificador.nuevo().valor().substring(0, 14);
        Local local = new Local(Identificador.nuevo().valor(), empresa, codigo,
                "Local de prueba", "Direccion de prueba", "01M2QZ606763Q7XC00M8VD7WM1");
        return locales.save(local).getId();
    }

    @Test
    void seBloqueaTrasElUmbralDeIntentosYQuedaPersistidoEnLaTabla() {
        String localId = crearLocal();
        int umbral = propiedades.getPin().getUmbralIntentos();

        for (int i = 0; i < umbral - 1; i++) {
            limitador.registrarFallo(localId, "tablet-1");
            assertThat(limitador.estaBloqueado(localId, "tablet-1")).isFalse();
        }
        limitador.registrarFallo(localId, "tablet-1");

        assertThat(limitador.estaBloqueado(localId, "tablet-1")).isTrue();
        assertThat(limitador.segundosRestantes(localId, "tablet-1")).isPresent();

        BloqueoPin fila = bloqueos.findByLocalIdAndDispositivo(localId, "tablet-1").orElseThrow();
        assertThat(fila.getIntentosFallidos()).isEqualTo(umbral);
        assertThat(fila.getBloqueadoHasta()).isNotNull();
    }

    @Test
    void bloqueaSoloElDispositivoQueFallo_LaTabletDeAlLadoSigueLibre() {
        String localId = crearLocal();
        int umbral = propiedades.getPin().getUmbralIntentos();

        for (int i = 0; i < umbral; i++) {
            limitador.registrarFallo(localId, "tablet-1");
        }

        assertThat(limitador.estaBloqueado(localId, "tablet-1")).isTrue();
        assertThat(limitador.estaBloqueado(localId, "tablet-2")).isFalse();
    }

    @Test
    void unExitoLimpiaElContadorDeEseDispositivo() {
        String localId = crearLocal();
        limitador.registrarFallo(localId, "tablet-1");
        limitador.registrarFallo(localId, "tablet-1");

        limitador.registrarExito(localId, "tablet-1");

        assertThat(limitador.estaBloqueado(localId, "tablet-1")).isFalse();
        BloqueoPin fila = bloqueos.findByLocalIdAndDispositivo(localId, "tablet-1").orElseThrow();
        assertThat(fila.getIntentosFallidos()).isZero();
    }

    @Test
    void unDispositivoSinNombreUsaLaHuellaDesconocidaEnVezDeFallar() {
        String localId = crearLocal();

        limitador.registrarFallo(localId, null);

        assertThat(bloqueos.findByLocalIdAndDispositivo(localId, "desconocido")).isPresent();
    }

    /**
     * Un User-Agent real de Chrome/Edge pasa de 120 caracteres y la columna de la clave del bloqueo mide 120: antes el
     * INSERT fallaba (500) y el fallo no se contaba. Ahora la huella se recorta y el bloqueo funciona igual.
     */
    @Test
    void unDispositivoMasLargoQueLaColumnaSeRecortaYElBloqueoSigueFuncionando() {
        String localId = crearLocal();
        String agenteLargo = "Mozilla/5.0 ".repeat(40);
        int umbral = propiedades.getPin().getUmbralIntentos();

        for (int i = 0; i < umbral; i++) {
            limitador.registrarFallo(localId, agenteLargo);
        }

        assertThat(limitador.estaBloqueado(localId, agenteLargo)).isTrue();
        assertThat(limitador.segundosRestantes(localId, agenteLargo)).isPresent();
    }
}

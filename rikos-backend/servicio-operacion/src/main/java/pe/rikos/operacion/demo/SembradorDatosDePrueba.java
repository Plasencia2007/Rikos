package pe.rikos.operacion.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.pensionistas.aplicacion.HuellaPinPensionista;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Carga datos de prueba al arrancar con el perfil "demo" (ver package-info).
 *
 * Por que no una migracion de Flyway: las migraciones son las mismas en todos los
 * entornos y nunca se editan; los datos de prueba cambian cuando el equipo quiere
 * y no deben llegar nunca a la nube. Por eso viven en demo/datos-de-prueba.sql,
 * fuera de db/migration, y no tocan el historial de Flyway.
 *
 * Idempotente: IDs fijos y ON CONFLICT DO NOTHING, asi que corre en cada arranque
 * sin duplicar nada. Lo unico que depende de la fecha es el menu: si la semana
 * actual no tiene programacion, la crea confirmada.
 *
 * Los hashes no se escriben en el SQL: se calculan aqui con el mismo codificador
 * y la misma huella de PIN que usa el sistema, asi las credenciales de prueba
 * funcionan igual que las reales.
 */
@Component
@Profile("demo & !nube")
public class SembradorDatosDePrueba implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SembradorDatosDePrueba.class);

    /** Mismo ULID fijo que V9__datos_semilla.sql. */
    private static final String ADMIN_SEMILLA_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    private static final String SCRIPT = "demo/datos-de-prueba.sql";
    private static final String CLAVE_DE_PRUEBA = "demo123";
    private static final int PORCIONES_ESTIMADAS = 30;

    /** El menu de prueba de cada dia: producto, componente y tipo de menu. */
    private static final List<String[]> OPCIONES_DEL_DIA = List.of(
            new String[]{"01DEM0PR000000000000000021", "entrada", "normal"},
            new String[]{"01DEM0PR000000000000000022", "entrada", "normal"},
            new String[]{"01DEM0PR000000000000000023", "entrada", "normal"},
            new String[]{"01DEM0PR000000000000000024", "segundo", "normal"},
            new String[]{"01DEM0PR000000000000000025", "segundo", "normal"},
            new String[]{"01DEM0PR000000000000000026", "segundo", "normal"},
            new String[]{"01DEM0PR000000000000000027", "segundo", "extra"},
            new String[]{"01DEM0PR000000000000000028", "refresco", "normal"},
            new String[]{"01DEM0PR000000000000000029", "postre", "normal"});

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaccion;
    private final PasswordEncoder codificador;
    private final HuellaPinPensionista huellas;
    private final Clock reloj;

    public SembradorDatosDePrueba(DataSource dataSource, JdbcTemplate jdbc, PlatformTransactionManager transacciones,
                                  PasswordEncoder codificador, HuellaPinPensionista huellas, Clock reloj) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        this.transaccion = new TransactionTemplate(transacciones);
        this.codificador = codificador;
        this.huellas = huellas;
        this.reloj = reloj;
    }

    @Override
    public void run(ApplicationArguments args) {
        sembrar();
        log.warn("DATOS DE PRUEBA cargados (perfil demo). Usuarios mozo1/mozo2 (PIN 1111/2222), cajero1, cocina1 y "
                + "adminlocal con contrasena {}; pensionistas con PIN 111111, 222222 y 333333. Todo es inventado.",
                CLAVE_DE_PRUEBA);
    }

    /** Todo en una transaccion: o quedan todos los datos de prueba o ninguno. */
    public void sembrar() {
        transaccion.executeWithoutResult(estado -> {
            ejecutarScript();
            sembrarMenuDeLaSemana(LocalDate.now(reloj));
        });
    }

    private void ejecutarScript() {
        String sql = leerScript();
        for (Map.Entry<String, String> valor : valoresCalculados().entrySet()) {
            sql = sql.replace("{{" + valor.getKey() + "}}", valor.getValue());
        }
        Connection conexion = DataSourceUtils.getConnection(dataSource);
        try {
            ScriptUtils.executeSqlScript(conexion,
                    new EncodedResource(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
        } finally {
            DataSourceUtils.releaseConnection(conexion, dataSource);
        }
    }

    private Map<String, String> valoresCalculados() {
        return Map.of(
                "HASH_CLAVE", codificador.encode(CLAVE_DE_PRUEBA),
                "HASH_PIN_MOZO_1", codificador.encode("1111"),
                "HASH_PIN_MOZO_2", codificador.encode("2222"),
                "HASH_PIN_PENS_1", codificador.encode("111111"),
                "HASH_PIN_PENS_2", codificador.encode("222222"),
                "HASH_PIN_PENS_3", codificador.encode("333333"),
                "HUELLA_PIN_PENS_1", huellas.de("111111"),
                "HUELLA_PIN_PENS_2", huellas.de("222222"),
                "HUELLA_PIN_PENS_3", huellas.de("333333"));
    }

    private String leerScript() {
        try {
            return new String(new ClassPathResource(SCRIPT).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + SCRIPT, e);
        }
    }

    /**
     * La semana (lunes a domingo) que contiene a hoy, confirmada, con el mismo menu
     * cada dia. Si alguien ya programo esa semana o alguno de sus dias, no se toca.
     */
    private void sembrarMenuDeLaSemana(LocalDate hoy) {
        LocalDate lunes = hoy.with(DayOfWeek.MONDAY);
        Integer semanas = jdbc.queryForObject("SELECT COUNT(*) FROM semana_menu WHERE fecha_inicio = ?", Integer.class, lunes);
        if (semanas != null && semanas > 0) {
            return;
        }
        String semanaId = Identificador.nuevo().valor();
        jdbc.update("""
                INSERT INTO semana_menu (id, fecha_inicio, fecha_fin, estado, confirmada_en, confirmada_por, version, creado_en, creado_por)
                VALUES (?, ?, ?, 'confirmado', NOW(), ?, 0, NOW(), ?)""",
                semanaId, lunes, lunes.plusDays(6), ADMIN_SEMILLA_ID, ADMIN_SEMILLA_ID);
        for (int dia = 0; dia < 7; dia++) {
            LocalDate fecha = lunes.plusDays(dia);
            Integer dias = jdbc.queryForObject("SELECT COUNT(*) FROM dia_menu WHERE fecha = ?", Integer.class, fecha);
            if (dias != null && dias > 0) {
                continue;
            }
            String diaId = Identificador.nuevo().valor();
            jdbc.update("INSERT INTO dia_menu (id, semana_menu_id, fecha, observacion) VALUES (?, ?, ?, 'Datos de prueba')",
                    diaId, semanaId, fecha);
            for (String[] opcion : OPCIONES_DEL_DIA) {
                jdbc.update("""
                        INSERT INTO opcion_menu (id, dia_menu_id, producto_id, componente, tipo_menu, porciones_estimadas, activo)
                        VALUES (?, ?, ?, ?, ?, ?, TRUE)""",
                        Identificador.nuevo().valor(), diaId, opcion[0], opcion[1], opcion[2], PORCIONES_ESTIMADAS);
            }
        }
    }
}

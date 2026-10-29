package pe.rikos.operacion.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.pensionistas.aplicacion.HuellaPinPensionista;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El sembrador corre sin el perfil "demo" (se arma a mano), dentro de la
 * transaccion de la prueba: todo lo que siembra se deshace al terminar.
 */
@SpringBootTest
@Transactional
class SembradorDatosDePruebaTest {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transacciones;
    @Autowired
    private PasswordEncoder codificador;
    @Autowired
    private HuellaPinPensionista huellas;
    @Autowired
    private Clock reloj;

    private SembradorDatosDePrueba sembrador;

    @BeforeEach
    void armar() {
        sembrador = new SembradorDatosDePrueba(dataSource, jdbc, transacciones, codificador, huellas, reloj);
    }

    private int contar(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    @Test
    void sembrarDosVecesNoDuplicaNada() {
        sembrador.sembrar();
        sembrador.sembrar();

        assertThat(contar("SELECT COUNT(*) FROM usuario WHERE id LIKE '01DEM0EM%'")).isEqualTo(5);
        assertThat(contar("SELECT COUNT(*) FROM mesa WHERE id LIKE '01DEM0ME%'")).isEqualTo(11);
        assertThat(contar("SELECT COUNT(*) FROM producto WHERE id LIKE '01DEM0PR%'")).isEqualTo(28);
        assertThat(contar("SELECT COUNT(*) FROM pensionista WHERE id LIKE '01DEM0PE%'")).isEqualTo(3);
        assertThat(contar("SELECT COUNT(*) FROM movimiento_inventario WHERE id LIKE '01DEM0MV%'")).isEqualTo(13);
        // 7 dias x 3 servicios; si alguien ya habia cargado un horario, ese se respeta y no se duplica.
        assertThat(contar("""
                SELECT COUNT(*) FROM (SELECT servicio, dia_semana FROM horario_servicio
                                      WHERE local_id = '01M2QZ60D35B90CW00EJA15VFC' GROUP BY servicio, dia_semana) t"""))
                .isEqualTo(21);
    }

    @Test
    void lasCredencialesDePruebaFuncionanComoLasReales() {
        sembrador.sembrar();

        String pinMozo = jdbc.queryForObject("SELECT hash_pin FROM usuario WHERE nombre_usuario = 'mozo1'", String.class);
        String claveCajero = jdbc.queryForObject("SELECT hash_contrasena FROM usuario WHERE nombre_usuario = 'cajero1'", String.class);
        String huella = jdbc.queryForObject("SELECT huella_pin FROM pensionista WHERE dni = '70000001'", String.class);

        assertThat(codificador.matches("1111", pinMozo)).isTrue();
        assertThat(codificador.matches("demo123", claveCajero)).isTrue();
        assertThat(huella).isEqualTo(huellas.de("111111"));
    }

    @Test
    void cadaPensionistaTieneResponsableYCuentaAbierta() {
        sembrador.sembrar();

        assertThat(contar("SELECT COUNT(*) FROM responsable WHERE pensionista_id LIKE '01DEM0PE%'")).isEqualTo(3);
        assertThat(contar("SELECT COUNT(*) FROM cuenta_pensionista WHERE pensionista_id LIKE '01DEM0PE%' AND estado = 'abierta'"))
                .isEqualTo(3);
    }

    @Test
    void lasRecetasQuedanCosteadasYElStockCuadraConElKardex() {
        sembrador.sembrar();

        assertThat(contar("SELECT COUNT(*) FROM receta WHERE id LIKE '01DEM0RE%' AND costo_total IS NULL")).isZero();
        assertThat(contar("""
                SELECT COUNT(*) FROM existencia e JOIN movimiento_inventario m ON m.id = e.ultimo_movimiento_id
                WHERE m.id LIKE '01DEM0MV%' AND e.stock_actual <> m.stock_nuevo""")).isZero();
    }

    @Test
    void hoyHayMenuProgramado() {
        sembrador.sembrar();

        assertThat(contar("""
                SELECT COUNT(*) FROM opcion_menu o JOIN dia_menu d ON d.id = o.dia_menu_id
                WHERE d.fecha = ? AND o.activo""", LocalDate.now(reloj))).isPositive();
    }
}

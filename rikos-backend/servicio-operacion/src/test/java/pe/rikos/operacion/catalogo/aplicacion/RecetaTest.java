package pe.rikos.operacion.catalogo.aplicacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.ComposicionReceta.IngredienteEntrada;
import pe.rikos.operacion.catalogo.infraestructura.*;
import pe.rikos.operacion.comun.auditoria.Auditoria;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-010. El escenario es el de la demo del sprint: un seco que lleva un
 * aderezo (subreceta) medido en gramos, cuando el aderezo rinde en kilos.
 *
 *   cebolla  4.00 / kg      aceite 10.00 / l
 *   aderezo: 1 kg cebolla + 0.5 l aceite = 9.00, rinde 1.5 kg -> 6.00 / kg
 *   seco:    300 g aderezo (0.3 kg x 6.00 = 1.80) + 0.2 kg cebolla (0.80)
 *            = 2.60, una porcion
 */
@SpringBootTest
@Transactional
class RecetaTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    private CrearReceta crearReceta;
    @Autowired
    private ActualizarReceta actualizarReceta;
    @Autowired
    private RegistrarCostoManual registrarCostoManual;
    @Autowired
    private CrearConversion crearConversion;
    @Autowired
    private EliminarProducto eliminarProducto;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private ProductoRepositorio productos;
    @Autowired
    private RecetaRepositorio recetas;
    @Autowired
    private AuditoriaRepositorio auditorias;
    @Autowired
    private EntityManager em;

    private UnidadMedida kg;
    private UnidadMedida g;
    private UnidadMedida litro;
    private UnidadMedida porcion;
    private CategoriaProducto hoja;
    private Producto cebolla;
    private Producto aceite;
    private Producto aderezo;
    private Producto seco;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        kg = unidad("kg", "masa");
        g = unidad("g", "masa");
        litro = unidad("l", "volumen");
        porcion = unidad("po", "unidad");
        String sufijo = Identificador.nuevo().valor().substring(18);
        CategoriaProducto raiz = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), "Recetas test " + sufijo, "insumo", null, 1, null));
        hoja = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), raiz, "Recetas hoja test " + sufijo, 1, null));

        cebolla = insumo("Cebolla", kg, "4.00");
        aceite = insumo("Aceite", litro, "10.00");
        aderezo = producto("Aderezo", kg, false, true);
        seco = producto("Seco", porcion, true, false);
        crearConversion.ejecutar(aderezo.getId(), kg.getId(), g.getId(), new BigDecimal("1000"), null);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    @Test
    void calcula_el_costo_por_porcion_con_una_subreceta_medida_en_otra_unidad() {
        Receta receta = crearAderezoYSeco();

        assertThat(recetas.findByProductoIdAndActivoTrue(aderezo.getId()).orElseThrow().getCostoTotal())
                .isEqualByComparingTo("9.00");
        assertThat(receta.getCostoTotal()).isEqualByComparingTo("2.60");
        assertThat(receta.getCostoPorPorcion()).isEqualByComparingTo("2.60");
    }

    @Test
    void divide_el_costo_total_entre_las_porciones() {
        Receta receta = crearReceta.ejecutar(aderezo.getId(), new BigDecimal("1.5"), 10,
                List.of(new IngredienteEntrada(cebolla.getId(), new BigDecimal("1"), kg.getId()),
                        new IngredienteEntrada(aceite.getId(), new BigDecimal("0.5"), litro.getId())),
                ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(receta.getCostoPorPorcion()).isEqualByComparingTo("0.90");
    }

    @Test
    void editar_el_costo_de_un_insumo_recalcula_en_cascada_hasta_el_plato() {
        Receta recetaSeco = crearAderezoYSeco();

        registrarCostoManual.ejecutar(cebolla.getId(), new BigDecimal("5.00"), ADMIN_SEMBRADO_ID);
        em.flush();
        em.clear();

        // aderezo: 5.00 + 5.00 = 10.00 -> 6.666667 / kg
        // seco:    0.3 x 6.666667 + 0.2 x 5.00 = 3.00
        Receta recargada = recetas.findById(recetaSeco.getId()).orElseThrow();
        assertThat(recargada.getCostoTotal().setScale(2, RoundingMode.HALF_UP)).isEqualByComparingTo("3.00");
    }

    @Test
    void cambiar_la_subreceta_recalcula_la_receta_que_la_usa() {
        Receta recetaSeco = crearAderezoYSeco();
        Receta recetaAderezo = recetas.findByProductoIdAndActivoTrue(aderezo.getId()).orElseThrow();

        // Mismo aderezo, pero ahora rinde 2 kg: 9.00 / 2 = 4.50 / kg
        actualizarReceta.ejecutar(recetaAderezo.getId(), new BigDecimal("2"), 10,
                List.of(new IngredienteEntrada(cebolla.getId(), new BigDecimal("1"), kg.getId()),
                        new IngredienteEntrada(aceite.getId(), new BigDecimal("0.5"), litro.getId())),
                ADMIN_SEMBRADO_ID);
        em.flush();
        em.clear();

        // seco: 0.3 x 4.50 + 0.80 = 2.15
        assertThat(recetas.findById(recetaSeco.getId()).orElseThrow().getCostoTotal())
                .isEqualByComparingTo("2.15");
    }

    @Test
    void editar_una_receta_conservando_sus_ingredientes_no_choca_con_la_restriccion_unica() {
        crearAderezoYSeco();
        Receta recetaAderezo = recetas.findByProductoIdAndActivoTrue(aderezo.getId()).orElseThrow();

        Resultado<Receta, FalloReceta> resultado = actualizarReceta.ejecutar(recetaAderezo.getId(),
                new BigDecimal("1.5"), 10,
                List.of(new IngredienteEntrada(aceite.getId(), new BigDecimal("0.4"), litro.getId()),
                        new IngredienteEntrada(cebolla.getId(), new BigDecimal("1.2"), kg.getId())),
                ADMIN_SEMBRADO_ID);
        em.flush();
        em.clear();

        assertThat(resultado.esExito()).isTrue();
        Receta recargada = recetas.findById(recetaAderezo.getId()).orElseThrow();
        assertThat(recargada.getIngredientes()).extracting(i -> i.getProducto().getId())
                .containsExactly(aceite.getId(), cebolla.getId());
        // 0.4 x 10 + 1.2 x 4 = 8.80
        assertThat(recargada.getCostoTotal()).isEqualByComparingTo("8.80");
    }

    @Test
    void un_insumo_sin_costo_deja_la_receta_con_costo_nulo_no_en_cero() {
        Producto sal = insumo("Sal", kg, null);

        Receta receta = crearReceta.ejecutar(aderezo.getId(), new BigDecimal("1"), 1,
                List.of(new IngredienteEntrada(cebolla.getId(), new BigDecimal("1"), kg.getId()),
                        new IngredienteEntrada(sal.getId(), new BigDecimal("0.01"), kg.getId())),
                ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(receta.getCostoTotal()).isNull();
        assertThat(receta.getCostoPorPorcion()).isNull();
    }

    @Test
    void rechaza_una_unidad_sin_conversion_y_dice_cual_ingrediente() {
        Resultado<Receta, FalloReceta> resultado = crearReceta.ejecutar(seco.getId(), BigDecimal.ONE, 1,
                List.of(new IngredienteEntrada(aceite.getId(), new BigDecimal("30"), g.getId())),
                ADMIN_SEMBRADO_ID);

        assertThat(fallo(resultado)).isEqualTo(FalloReceta.en(ErrorReceta.SIN_CONVERSION, aceite.getId()));
    }

    @Test
    void convierte_en_los_dos_sentidos() {
        // El seco usa el aderezo (base kg) en g: la conversion "1 kg = 1000 g"
        // se recorre al reves y divide. Aca es al derecho: la pasta tiene base
        // g y se pide en kg -- multiplica.
        Producto pasta = producto("Pasta de aji", g, false, true);
        crearConversion.ejecutar(pasta.getId(), kg.getId(), g.getId(), new BigDecimal("1000"), null);
        crearReceta.ejecutar(pasta.getId(), new BigDecimal("500"), 1,
                List.of(new IngredienteEntrada(aceite.getId(), new BigDecimal("0.1"), litro.getId())),
                ADMIN_SEMBRADO_ID);

        Receta receta = crearReceta.ejecutar(seco.getId(), BigDecimal.ONE, 1,
                List.of(new IngredienteEntrada(pasta.getId(), new BigDecimal("0.25"), kg.getId())),
                ADMIN_SEMBRADO_ID).valorOLanza();

        // pasta: 1.00 / 500 g = 0.002 / g; 0.25 kg = 250 g -> 0.50
        assertThat(receta.getCostoTotal()).isEqualByComparingTo("0.50");
    }

    @Test
    void rechaza_una_receta_que_se_contiene_a_si_misma() {
        Resultado<Receta, FalloReceta> resultado = crearReceta.ejecutar(aderezo.getId(), BigDecimal.ONE, 1,
                List.of(new IngredienteEntrada(aderezo.getId(), BigDecimal.ONE, kg.getId())), ADMIN_SEMBRADO_ID);

        assertThat(fallo(resultado)).isEqualTo(FalloReceta.en(ErrorReceta.CICLO, aderezo.getId()));
    }

    @Test
    void rechaza_un_ciclo_indirecto_entre_subrecetas() {
        Producto base = producto("Base de aderezo", kg, false, true);
        crearReceta.ejecutar(aderezo.getId(), BigDecimal.ONE, 1,
                List.of(new IngredienteEntrada(base.getId(), BigDecimal.ONE, kg.getId())), ADMIN_SEMBRADO_ID);

        // base -> aderezo -> base
        Resultado<Receta, FalloReceta> resultado = crearReceta.ejecutar(base.getId(), BigDecimal.ONE, 1,
                List.of(new IngredienteEntrada(aderezo.getId(), BigDecimal.ONE, kg.getId())), ADMIN_SEMBRADO_ID);

        assertThat(fallo(resultado)).isEqualTo(FalloReceta.en(ErrorReceta.CICLO, aderezo.getId()));
    }

    @Test
    void rechaza_un_producto_que_no_es_vendible_ni_subreceta() {
        Resultado<Receta, FalloReceta> resultado = crearReceta.ejecutar(cebolla.getId(), BigDecimal.ONE, 1,
                List.of(new IngredienteEntrada(aceite.getId(), BigDecimal.ONE, litro.getId())), ADMIN_SEMBRADO_ID);

        assertThat(fallo(resultado)).isEqualTo(FalloReceta.de(ErrorReceta.PRODUCTO_NO_ADMITE_RECETA));
    }

    @Test
    void rechaza_como_ingrediente_un_plato_que_no_es_insumo_ni_subreceta() {
        Resultado<Receta, FalloReceta> resultado = crearReceta.ejecutar(aderezo.getId(), BigDecimal.ONE, 1,
                List.of(new IngredienteEntrada(seco.getId(), BigDecimal.ONE, porcion.getId())), ADMIN_SEMBRADO_ID);

        assertThat(fallo(resultado)).isEqualTo(FalloReceta.en(ErrorReceta.INGREDIENTE_INVALIDO, seco.getId()));
    }

    @Test
    void rechaza_una_segunda_receta_activa_para_el_mismo_producto() {
        crearAderezoYSeco();

        Resultado<Receta, FalloReceta> resultado = crearReceta.ejecutar(seco.getId(), BigDecimal.ONE, 1,
                List.of(new IngredienteEntrada(cebolla.getId(), BigDecimal.ONE, kg.getId())), ADMIN_SEMBRADO_ID);

        assertThat(fallo(resultado)).isEqualTo(FalloReceta.de(ErrorReceta.RECETA_YA_EXISTE));
    }

    @Test
    void crear_y_editar_una_receta_quedan_en_la_bitacora_con_el_antes_y_el_despues() {
        Receta receta = crearReceta.ejecutar(aderezo.getId(), new BigDecimal("1.5"), 10,
                List.of(new IngredienteEntrada(cebolla.getId(), BigDecimal.ONE, kg.getId())),
                ADMIN_SEMBRADO_ID).valorOLanza();
        actualizarReceta.ejecutar(receta.getId(), new BigDecimal("1.5"), 10,
                List.of(new IngredienteEntrada(cebolla.getId(), new BigDecimal("2"), kg.getId())),
                ADMIN_SEMBRADO_ID);

        List<Auditoria> filas = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("receta", receta.getId());
        assertThat(filas).hasSize(2);
        assertThat(filas).allMatch(f -> f.getEvento().equals("cambio_receta"));
        assertThat(filas).anyMatch(f -> f.getValorAnterior() != null
                && f.getValorAnterior().contains(cebolla.getNombre() + ": 1 ")
                && f.getValorNuevo().contains(cebolla.getNombre() + ": 2 "));
    }

    @Test
    void no_se_puede_borrar_un_insumo_que_esta_en_una_receta() {
        crearAderezoYSeco();

        Resultado<Void, ErrorCatalogo> resultado = eliminarProducto.ejecutar(cebolla.getId());

        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error()).isEqualTo(ErrorCatalogo.TIENE_HISTORIAL);
    }

    private Receta crearAderezoYSeco() {
        crearReceta.ejecutar(aderezo.getId(), new BigDecimal("1.5"), 10,
                List.of(new IngredienteEntrada(cebolla.getId(), new BigDecimal("1"), kg.getId()),
                        new IngredienteEntrada(aceite.getId(), new BigDecimal("0.5"), litro.getId())),
                ADMIN_SEMBRADO_ID).valorOLanza();
        return crearReceta.ejecutar(seco.getId(), BigDecimal.ONE, 1,
                List.of(new IngredienteEntrada(aderezo.getId(), new BigDecimal("300"), g.getId()),
                        new IngredienteEntrada(cebolla.getId(), new BigDecimal("0.2"), kg.getId())),
                ADMIN_SEMBRADO_ID).valorOLanza();
    }

    private static FalloReceta fallo(Resultado<Receta, FalloReceta> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, FalloReceta>) resultado).error();
    }

    private UnidadMedida unidad(String prefijo, String tipo) {
        String codigo = prefijo + Identificador.nuevo().valor().substring(20);
        return unidades.save(new UnidadMedida(Identificador.nuevo().valor(), codigo, "Unidad " + codigo, tipo));
    }

    private Producto insumo(String nombre, UnidadMedida base, String costo) {
        Producto p = producto(nombre, base, false, false);
        if (costo != null) {
            p.registrarCostoManual(new BigDecimal(costo), null);
        }
        return productos.save(p);
    }

    private Producto producto(String nombre, UnidadMedida base, boolean vendible, boolean subreceta) {
        String codigo = "RT-" + Identificador.nuevo().valor().substring(16);
        return productos.save(new Producto(Identificador.nuevo().valor(), codigo, nombre + " " + codigo, hoja, base,
                vendible, !vendible && !subreceta, subreceta, vendible ? 1000L : null, true, null));
    }
}

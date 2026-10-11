package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.*;
import pe.rikos.operacion.comun.auditoria.Auditoria;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-015. El local es siempre el activo del token (ContextoDeSeguridadDePrueba
 * lo pone en "details", igual que FiltroAutenticacionJwt): ningun caso de
 * uso recibe el local como parametro.
 */
@SpringBootTest
@Transactional
class DisponibilidadTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    private static final String MOTIVO_SEMBRADO_ID = "01M39A0MT0000000000000D001";

    @Autowired
    private MarcarAgotado marcarAgotado;
    @Autowired
    private ReactivarProducto reactivarProducto;
    @Autowired
    private ConsultaDisponibilidad consulta;
    @Autowired
    private CrearLocal crearLocal;
    @Autowired
    private CrearMotivo crearMotivo;
    @Autowired
    private CambiarEstadoMotivo cambiarEstadoMotivo;
    @Autowired
    private ProductoRepositorio productos;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private AuditoriaRepositorio auditorias;

    private String localA;
    private String localB;
    private Producto aji;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = local("da");
        localB = local("db");
        aji = producto("Ají de gallina", "01M39A0RZ0000000000000S004", true);
        enLocal(localA);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    @Test
    void marcar_agotado_lo_oculta_en_ese_local_y_queda_en_la_bitacora() {
        Resultado<DisponibilidadProducto, ErrorDisponibilidad> resultado = marcarAgotado.ejecutar(aji.getId());

        assertThat(resultado.esExito()).isTrue();
        assertThat(consulta.estaAgotado(localA, aji.getId())).isTrue();
        assertThat(consulta.agotadosDelLocal(localA)).extracting(d -> d.getProducto().getId()).contains(aji.getId());
        List<Auditoria> filas = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("producto", aji.getId());
        assertThat(filas).anyMatch(f -> f.getEvento().equals("marcado_agotado") && localA.equals(f.getLocalId()));
    }

    @Test
    void en_otro_local_sigue_disponible() {
        marcarAgotado.ejecutar(aji.getId());

        assertThat(consulta.estaAgotado(localB, aji.getId())).isFalse();
        assertThat(consulta.agotadosDelLocal(localB)).isEmpty();
    }

    @Test
    void reactivar_exige_motivo_de_disponibilidad_y_registra_quien_cuando_y_por_que() {
        marcarAgotado.ejecutar(aji.getId());

        assertThat(error(reactivarProducto.ejecutar(aji.getId(), null, null)))
                .isEqualTo(ErrorDisponibilidad.MOTIVO_OBLIGATORIO);

        Resultado<DisponibilidadProducto, ErrorDisponibilidad> resultado =
                reactivarProducto.ejecutar(aji.getId(), MOTIVO_SEMBRADO_ID, null);

        assertThat(resultado.esExito()).isTrue();
        assertThat(consulta.estaAgotado(localA, aji.getId())).isFalse();
        DisponibilidadProducto fila = resultado.valorOLanza();
        assertThat(fila.getMarcadoPor()).isEqualTo(ADMIN_SEMBRADO_ID);
        assertThat(fila.getMarcadoEn()).isNotNull();
        assertThat(fila.getMotivo().getId()).isEqualTo(MOTIVO_SEMBRADO_ID);
        assertThat(auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("producto", aji.getId()))
                .anyMatch(f -> f.getEvento().equals("reactivacion") && MOTIVO_SEMBRADO_ID.equals(f.getMotivoId())
                        && ADMIN_SEMBRADO_ID.equals(f.getUsuarioId()));
    }

    @Test
    void el_motivo_tiene_que_ser_de_disponibilidad_y_estar_activo() {
        marcarAgotado.ejecutar(aji.getId());
        Motivo deAnulacion = crearMotivo.ejecutar("anulacion", "Prueba anulación disp", false).valorOLanza();
        Motivo inactivo = crearMotivo.ejecutar("disponibilidad", "Prueba inactivo disp", false).valorOLanza();
        cambiarEstadoMotivo.ejecutar(inactivo.getId(), false);

        assertThat(error(reactivarProducto.ejecutar(aji.getId(), deAnulacion.getId(), null)))
                .isEqualTo(ErrorDisponibilidad.MOTIVO_INVALIDO);
        assertThat(error(reactivarProducto.ejecutar(aji.getId(), inactivo.getId(), null)))
                .isEqualTo(ErrorDisponibilidad.MOTIVO_INVALIDO);
    }

    @Test
    void si_el_motivo_pide_observacion_se_exige() {
        marcarAgotado.ejecutar(aji.getId());
        Motivo conObservacion = crearMotivo.ejecutar("disponibilidad", "Llegó mercadería disp", true).valorOLanza();

        assertThat(error(reactivarProducto.ejecutar(aji.getId(), conObservacion.getId(), "  ")))
                .isEqualTo(ErrorDisponibilidad.OBSERVACION_OBLIGATORIA);
        DisponibilidadProducto fila = reactivarProducto.ejecutar(aji.getId(), conObservacion.getId(),
                "Llegaron 5 kg de pollo").valorOLanza();
        assertThat(fila.getObservacion()).isEqualTo("Llegaron 5 kg de pollo");
    }

    @Test
    void no_marca_dos_veces_ni_reactiva_lo_que_no_esta_agotado() {
        assertThat(error(reactivarProducto.ejecutar(aji.getId(), MOTIVO_SEMBRADO_ID, null)))
                .isEqualTo(ErrorDisponibilidad.NO_ESTA_AGOTADO);
        marcarAgotado.ejecutar(aji.getId());
        assertThat(error(marcarAgotado.ejecutar(aji.getId()))).isEqualTo(ErrorDisponibilidad.YA_AGOTADO);
    }

    @Test
    void el_ciclo_completo_se_puede_repetir() {
        marcarAgotado.ejecutar(aji.getId());
        reactivarProducto.ejecutar(aji.getId(), MOTIVO_SEMBRADO_ID, null);

        assertThat(marcarAgotado.ejecutar(aji.getId()).esExito()).isTrue();
        assertThat(consulta.estaAgotado(localA, aji.getId())).isTrue();
        assertThat(reactivarProducto.ejecutar(aji.getId(), MOTIVO_SEMBRADO_ID, null).esExito()).isTrue();
        assertThat(consulta.estaAgotado(localA, aji.getId())).isFalse();
    }

    @Test
    void solo_se_marcan_productos_vendibles() {
        Producto cebolla = producto("Cebolla", "01M39A0RZ0000000000000S021", false);

        assertThat(error(marcarAgotado.ejecutar(cebolla.getId()))).isEqualTo(ErrorDisponibilidad.PRODUCTO_NO_VENDIBLE);
        assertThat(error(marcarAgotado.ejecutar("no-existe"))).isEqualTo(ErrorDisponibilidad.PRODUCTO_NO_ENCONTRADO);
    }

    @Test
    void sin_local_activo_no_se_marca_nada() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);

        assertThat(error(marcarAgotado.ejecutar(aji.getId()))).isEqualTo(ErrorDisponibilidad.SIN_LOCAL_ACTIVO);
    }

    private static ErrorDisponibilidad error(Resultado<?, ErrorDisponibilidad> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorDisponibilidad>) resultado).error();
    }

    private void enLocal(String localId) {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localId);
    }

    private String local(String prefijo) {
        String codigo = prefijo + Identificador.nuevo().valor().substring(20).toLowerCase();
        return crearLocal.ejecutar(codigo, "Local " + codigo, "Dirección de prueba", ADMIN_SEMBRADO_ID)
                .valorOLanza().getId();
    }

    private Producto producto(String nombre, String categoriaId, boolean vendible) {
        CategoriaProducto categoria = categorias.findById(categoriaId).orElseThrow();
        UnidadMedida und = unidades.findAll().stream().filter(u -> u.getCodigo().equals("und")).findFirst()
                .orElseThrow();
        String codigo = "DSP-" + Identificador.nuevo().valor().substring(16);
        return productos.save(new Producto(Identificador.nuevo().valor(), codigo, nombre + " " + codigo, categoria, und,
                vendible, !vendible, false, vendible ? 1500L : null, false, null));
    }
}

package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ActivarProveedorTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @BeforeEach
    void autenticar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    @Autowired
    private CrearProveedor crearProveedor;
    @Autowired
    private DesactivarProveedor desactivarProveedor;
    @Autowired
    private ActivarProveedor activarProveedor;

    @Test
    void reactiva_un_proveedor_previamente_desactivado() {
        Proveedor proveedor = crearProveedor.ejecutar(
                null, "Proveedor Activar Test", null, null, null, null, null, null).valorOLanza();
        desactivarProveedor.ejecutar(proveedor.getId(), null);

        Resultado<Proveedor, ErrorCatalogo> resultado = activarProveedor.ejecutar(proveedor.getId(), null);

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().isActivo()).isTrue();
    }

    @Test
    void falla_con_no_encontrado_si_el_proveedor_no_existe() {
        Resultado<Proveedor, ErrorCatalogo> resultado = activarProveedor.ejecutar("no-existe", null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.NO_ENCONTRADO);
    }
}

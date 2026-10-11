package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.catalogo.infraestructura.ProveedorRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EliminarProveedorTest {

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
    private EliminarProveedor eliminarProveedor;
    @Autowired
    private ProveedorRepositorio proveedores;

    @Test
    void borra_un_proveedor_que_nunca_se_uso() {
        Proveedor proveedor = crearProveedor.ejecutar(
                null, "Proveedor Eliminar Test", null, null, null, null, null, null).valorOLanza();

        Resultado<Void, ErrorCatalogo> resultado = eliminarProveedor.ejecutar(proveedor.getId());

        assertThat(resultado.esExito()).isTrue();
        assertThat(proveedores.findById(proveedor.getId())).isEmpty();
    }

    @Test
    void falla_con_no_encontrado_si_el_proveedor_no_existe() {
        Resultado<Void, ErrorCatalogo> resultado = eliminarProveedor.ejecutar("no-existe");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.NO_ENCONTRADO);
    }
}

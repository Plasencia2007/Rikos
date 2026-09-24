package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CrearProveedorTest {

    @Autowired
    private CrearProveedor crearProveedor;

    @Test
    void crea_un_proveedor_sin_ruc_mercado_de_abastos() {
        Resultado<Proveedor, ErrorCatalogo> resultado = crearProveedor.ejecutar(
                null, "Don Pepe Abarrotes", null, "987654321", null, "Mercado Central puesto 12", "Pepe", null);

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().getRuc()).isNull();
    }

    @Test
    void rechaza_un_ruc_que_no_tiene_exactamente_11_digitos() {
        Resultado<Proveedor, ErrorCatalogo> resultado = crearProveedor.ejecutar(
                "12345", "Proveedor Mal Ruc SAC", null, null, null, null, null, null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.RUC_INVALIDO);
    }

    @Test
    void rechaza_ruc_duplicado_cuando_no_es_nulo() {
        crearProveedor.ejecutar("20123456780", "Proveedor Uno SAC", null, null, null, null, null, null);

        Resultado<Proveedor, ErrorCatalogo> resultado =
                crearProveedor.ejecutar("20123456780", "Proveedor Dos SAC", null, null, null, null, null, null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.RUC_DUPLICADO);
    }
}

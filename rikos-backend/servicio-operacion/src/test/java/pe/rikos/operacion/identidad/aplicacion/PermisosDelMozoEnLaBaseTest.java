package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.identidad.infraestructura.RolPermisoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.RolRepositorio;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 6 (spec 3.10): el mozo reorganiza cuentas (venta.modificar) y bloquea mesas. El sembrador solo
 * agrega a un rol existente los permisos NUEVOS del catalogo, asi que venta.modificar (que ya existia)
 * no llega solo a una base ya sembrada: lo otorga la migracion V31. Esta prueba mira la BASE, no el
 * mapa de valores por defecto, que es lo que un mozo real usa.
 */
@SpringBootTest
@Transactional
class PermisosDelMozoEnLaBaseTest {

    @Autowired
    RolRepositorio roles;
    @Autowired
    RolPermisoRepositorio rolPermisos;

    @Test
    void el_rol_mozo_de_la_base_puede_modificar_pedidos_y_bloquear_mesas_pero_no_administrar_ni_anular() {
        String mozoId = roles.findByNombre("mozo").orElseThrow().getId();

        List<String> codigos = rolPermisos.findByRolId(mozoId).stream().map(rp -> rp.getPermiso().getCodigo()).toList();

        assertThat(codigos).contains("venta.ver", "venta.crear", "venta.modificar", "venta.bloquear_mesa");
        assertThat(codigos).doesNotContain("venta.administrar", "venta.anular", "venta.reabrir");
    }
}

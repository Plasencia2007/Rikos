package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Permiso;
import pe.rikos.operacion.identidad.infraestructura.PermisoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.RolPermiso;
import pe.rikos.operacion.identidad.infraestructura.RolPermisoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.RolRepositorio;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los roles del sistema se pueden editar, y "restaurar predeterminados" necesita saber
 * cuales eran. Ademas el sembrador, que corre en cada arranque, no puede volver a
 * poner un permiso que el administrador quito a proposito.
 */
@SpringBootTest
@Transactional
class PermisosPredeterminadosTest {

    @Autowired
    private ObtenerPermisosPredeterminados obtenerPredeterminados;
    @Autowired
    private SembradorCatalogoPermisos sembrador;
    @Autowired
    private RolRepositorio roles;
    @Autowired
    private RolPermisoRepositorio rolPermisos;
    @Autowired
    private PermisoRepositorio permisos;

    private Rol rol(String nombre) {
        return roles.findByNombre(nombre).orElseThrow();
    }

    private Set<String> codigos(List<String> ids) {
        return permisos.findByIdIn(ids).stream().map(Permiso::getCodigo).collect(Collectors.toSet());
    }

    @Test
    void elCajeroVieneConSusQuincePermisosDeFabrica() {
        Resultado<List<String>, ErrorRol> resultado = obtenerPredeterminados.ejecutar(rol("cajero").getId());

        Set<String> codigos = codigos(resultado.valorOLanza());
        // 15: los 14 originales + venta.bloquear_mesa (sprint 6, US-027: el cajero tambien bloquea mesas).
        assertThat(codigos).hasSize(15).contains("caja.anular", "caja.reabrir", "venta.crear", "facturacion.crear",
                "venta.bloquear_mesa");
    }

    @Test
    void unRolPersonalizadoNoTienePredeterminados() {
        Rol propio = roles.save(new Rol(pe.rikos.contratos.Identificador.nuevo().valor(), "rol propio predeterminados", null, false,
                "#2563eb"));

        assertThat(obtenerPredeterminados.ejecutar(propio.getId()).valorOLanza()).isEmpty();
    }

    @Test
    void unRolInexistenteDevuelveNoEncontrado() {
        Resultado<List<String>, ErrorRol> resultado = obtenerPredeterminados.ejecutar("no-existe");

        assertThat(resultado.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<List<String>, ErrorRol>) resultado).error()).isEqualTo(ErrorRol.NO_ENCONTRADO);
    }

    @Test
    void elSembradorNoRestituyeUnPermisoQueSeQuito() {
        Rol cajero = rol("cajero");
        RolPermiso anular = rolPermisos.findByRolId(cajero.getId()).stream()
                .filter(rp -> rp.getPermiso().getCodigo().equals("caja.anular")).findFirst().orElseThrow();
        rolPermisos.delete(anular);
        rolPermisos.flush();

        sembrador.run(null);

        Set<String> actuales = rolPermisos.findByRolId(cajero.getId()).stream()
                .map(rp -> rp.getPermiso().getCodigo()).collect(Collectors.toSet());
        assertThat(actuales).doesNotContain("caja.anular");
    }
}

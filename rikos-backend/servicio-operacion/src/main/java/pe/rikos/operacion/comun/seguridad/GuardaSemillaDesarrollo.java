package pe.rikos.operacion.comun.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;

/**
 * V9__datos_semilla.sql crea el usuario "admin" / "cambiar123" -- marcado
 * ahi mismo como DESARROLLO -- sin ninguna condicion de perfil: la misma
 * migracion corre igual en "nube" (DT-015 / application-nube.yaml) que en
 * "local". Partir la migracion por perfil es mas riesgoso de lo que
 * resuelve (los entornos ya migrados quedarian con un historial de Flyway
 * que no coincide con las ubicaciones nuevas).
 *
 * P-29: la primera version de esto frenaba el arranque con una excepcion
 * si detectaba el usuario semilla en "nube" -- pero la migracion V9 y este
 * guarda corren en el MISMO arranque, asi que un despliegue nuevo a nube
 * nunca habria podido levantar la primera vez (huevo y gallina: alguien
 * tendria que borrar la fila a mano contra produccion antes del primer
 * arranque exitoso). Corregido: en vez de frenar el arranque, DESACTIVA
 * el usuario semilla (mismo camino que CambiarEstadoLocal/EliminarLocal
 * usan para "sacar de circulacion sin romper nada") y deja una advertencia
 * bien visible en el log. El despliegue funciona, la cuenta admin/
 * cambiar123 queda inutilizable de entrada, y no se toca el historial de
 * Flyway.
 */
@Component
@Profile("nube")
public class GuardaSemillaDesarrollo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GuardaSemillaDesarrollo.class);

    /** Mismo ULID fijo que V9__datos_semilla.sql y ADMIN_SEMBRADO_ID de los tests. */
    static final String ID_ADMIN_SEMILLA = "01M2QZ606763Q7XC00M8VD7WM1";

    private final UsuarioRepositorio usuarios;

    public GuardaSemillaDesarrollo(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public void run(ApplicationArguments args) {
        usuarios.findById(ID_ADMIN_SEMILLA)
                .filter(Usuario::isActivo)
                .ifPresent(admin -> {
                    admin.desactivar(null); // lo hizo el sistema al arrancar, no un actor humano
                    usuarios.save(admin);
                    log.warn("SEGURIDAD: el usuario semilla de DESARROLLO (admin / cambiar123, {}) estaba "
                            + "presente en un arranque con perfil 'nube' -- se desactivo automaticamente. "
                            + "Creá un administrador real y borrá esta fila cuando puedas.", ID_ADMIN_SEMILLA);
                });
    }
}

package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.pensionistas.infraestructura.ParametroGlobal;
import pe.rikos.operacion.pensionistas.infraestructura.ParametroGlobalRepositorio;

import java.util.Objects;

/**
 * Cambia un parametro global (solo el administrador central). Cada cambio
 * queda en la bitacora como "cambio_parametro", con el valor anterior y el
 * nuevo; repetir el mismo valor no escribe nada.
 */
@Service
public class CambiarParametro {

    private final ParametroGlobalRepositorio parametros;
    private final RegistradorAuditoria auditoria;

    public CambiarParametro(ParametroGlobalRepositorio parametros, RegistradorAuditoria auditoria) {
        this.parametros = parametros;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<ParametroGlobal, ErrorParametro> ejecutar(ClaveParametro clave, String valor, String actorId) {
        String limpio = valor == null || valor.isBlank() ? null : valor.trim();
        if (!esValido(clave, limpio)) {
            return Resultado.fracaso(ErrorParametro.VALOR_INVALIDO);
        }
        ParametroGlobal parametro = parametros.findByClave(clave.codigo()).orElse(null);
        if (parametro == null) {
            return Resultado.fracaso(ErrorParametro.NO_ENCONTRADO);
        }
        String anterior = parametro.getValor();
        if (Objects.equals(anterior, limpio)) {
            return Resultado.exito(parametro);
        }
        parametro.cambiar(limpio, actorId);
        ParametroGlobal guardado = parametros.save(parametro);
        auditoria.registrar("cambio_parametro", "parametro_global", parametro.getId(),
                RegistradorAuditoria.campos("clave", clave.codigo(), "valor", anterior),
                RegistradorAuditoria.campos("clave", clave.codigo(), "valor", limpio));
        return Resultado.exito(guardado);
    }

    private static boolean esValido(ClaveParametro clave, String valor) {
        return switch (clave) {
            // Vacio = "sin monto"; si hay valor, centimos enteros y positivos.
            case TAPER_PRECIO -> valor == null || esCentimosPositivos(valor);
            // Siempre hay una regla: no admite quedar vacia.
            case SEGUNDO_CONSUMO_REGLA -> valor != null && ReglaSegundoConsumo.desde(valor).isPresent();
        };
    }

    private static boolean esCentimosPositivos(String valor) {
        try {
            return Long.parseLong(valor) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

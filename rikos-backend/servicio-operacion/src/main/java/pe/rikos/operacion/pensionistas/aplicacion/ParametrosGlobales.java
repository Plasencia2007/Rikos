package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.pensionistas.infraestructura.ParametroGlobal;
import pe.rikos.operacion.pensionistas.infraestructura.ParametroGlobalRepositorio;

import java.util.List;
import java.util.Optional;

/**
 * Lectura tipada de los parametros globales. Es lo que el motor de precios
 * (entrega 2) y la caja consultan; nadie mas lee `parametro_global` a mano.
 */
@Component
public class ParametrosGlobales {

    private final ParametroGlobalRepositorio parametros;

    public ParametrosGlobales(ParametroGlobalRepositorio parametros) {
        this.parametros = parametros;
    }

    /** Centimos por menu; vacio si el duenio todavia no lo definio (entonces no se cobra taper). */
    @Transactional(readOnly = true)
    public Optional<Long> taperPrecio() {
        return parametros.findByClave(ClaveParametro.TAPER_PRECIO.codigo())
                .map(ParametroGlobal::getValor)
                .filter(v -> !v.isBlank())
                .map(Long::valueOf);
    }

    @Transactional(readOnly = true)
    public ReglaSegundoConsumo reglaSegundoConsumo() {
        return parametros.findByClave(ClaveParametro.SEGUNDO_CONSUMO_REGLA.codigo())
                .map(ParametroGlobal::getValor)
                .flatMap(ReglaSegundoConsumo::desde)
                .orElse(ReglaSegundoConsumo.PRECIO_DEL_PLAN);
    }

    @Transactional(readOnly = true)
    public List<ParametroGlobal> todos() {
        return parametros.findAll();
    }
}

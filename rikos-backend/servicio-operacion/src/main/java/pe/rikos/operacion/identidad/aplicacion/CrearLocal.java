package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Empresa;
import pe.rikos.operacion.identidad.infraestructura.EmpresaRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;

import java.util.regex.Pattern;

/**
 * US-001 CA1: crear un local NO crea copias del catalogo, recetas, precios,
 * planes ni menu -- esos son centrales y no llevan local_id (2.2).
 */
@Service
public class CrearLocal {

    // El codigo es el subdominio del POS de ese local (sanisidro.rikos.pe,
    // DT-029): minusculas, numeros y guiones, sin espacios ni tildes ni ñ,
    // por la misma restriccion de nombres del propio modelo (1.3).
    private static final Pattern FORMATO_CODIGO = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    private final LocalRepositorio locales;
    private final EmpresaRepositorio empresas;

    public CrearLocal(LocalRepositorio locales, EmpresaRepositorio empresas) {
        this.locales = locales;
        this.empresas = empresas;
    }

    @Transactional
    public Resultado<Local, ErrorLocal> ejecutar(String codigo, String nombre, String direccion, String actorId) {

        if (!FORMATO_CODIGO.matcher(codigo).matches()) {
            return Resultado.fracaso(ErrorLocal.CODIGO_FORMATO_INVALIDO);
        }

        if (locales.existsByCodigo(codigo)) {
            return Resultado.fracaso(ErrorLocal.CODIGO_DUPLICADO);
        }

        // Existe una sola empresa (3.1): la sembro el sprint 1, no se elige por API.
        Empresa empresa = empresas.findAll().get(0);

        Local local = new Local(Identificador.nuevo().valor(), empresa, codigo, nombre, direccion, actorId);
        return Resultado.exito(locales.save(local));
    }
}

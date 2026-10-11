package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;

/**
 * Extraido de UnidadController, que hacia el chequeo + save directo contra
 * el repositorio sin @Transactional -- inofensivo mientras UnidadMedida no
 * tenga ninguna relacion perezosa, pero no seguia el patron del resto del
 * catalogo (CrearCategoria, CrearProveedor, etc.).
 */
@Service
public class CrearUnidad {

    private final UnidadMedidaRepositorio unidades;

    public CrearUnidad(UnidadMedidaRepositorio unidades) {
        this.unidades = unidades;
    }

    @Transactional
    public Resultado<UnidadMedida, ErrorCatalogo> ejecutar(String codigo, String nombre, String tipo) {
        if (unidades.existsByCodigo(codigo)) {
            return Resultado.fracaso(ErrorCatalogo.CODIGO_DUPLICADO);
        }
        UnidadMedida creada = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), codigo, nombre, tipo));
        return Resultado.exito(creada);
    }
}

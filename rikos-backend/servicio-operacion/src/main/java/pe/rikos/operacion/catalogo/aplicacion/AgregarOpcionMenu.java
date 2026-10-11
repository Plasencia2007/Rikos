package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.*;

import java.time.LocalDate;

/**
 * US-013 CA1, CA2, CA5: agrega una opcion a un dia de la semana, en
 * borrador o confirmada. El componente sale de la categoria del producto
 * (el usuario no lo elige, modelo 6.3). Nunca toca el catalogo maestro.
 */
@Service
public class AgregarOpcionMenu {

    private final SemanaMenuRepositorio semanas;
    private final DiaMenuRepositorio dias;
    private final OpcionMenuRepositorio opciones;
    private final ProductoRepositorio productos;
    private final ConsultaMenu consulta;

    public AgregarOpcionMenu(SemanaMenuRepositorio semanas, DiaMenuRepositorio dias, OpcionMenuRepositorio opciones,
                             ProductoRepositorio productos, ConsultaMenu consulta) {
        this.semanas = semanas;
        this.dias = dias;
        this.opciones = opciones;
        this.productos = productos;
        this.consulta = consulta;
    }

    @Transactional
    public Resultado<OpcionMenu, ErrorProgramacion> ejecutar(String semanaId, LocalDate fecha, String productoId,
            String tipoMenu, Integer porcionesEstimadas) {
        SemanaMenu semana = semanas.findById(semanaId).orElse(null);
        if (semana == null) {
            return Resultado.fracaso(ErrorProgramacion.NO_ENCONTRADO);
        }
        if (fecha == null || !semana.contiene(fecha)) {
            return Resultado.fracaso(ErrorProgramacion.FECHA_FUERA_DE_SEMANA);
        }
        if (consulta.estaCerrado(fecha)) {
            return Resultado.fracaso(ErrorProgramacion.DIA_CERRADO);
        }
        Producto producto = productoId == null ? null : productos.findById(productoId).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorProgramacion.PRODUCTO_NO_ENCONTRADO);
        }
        String componente = producto.getCategoria().getComponenteMenu();
        if (!producto.isActivo() || !"componente_menu".equals(producto.getCategoria().getNaturaleza())
                || componente == null) {
            return Resultado.fracaso(ErrorProgramacion.PRODUCTO_NO_ES_DEL_MENU);
        }
        var validacion = ReglasOpcionMenu.validar(componente, tipoMenu, porcionesEstimadas);
        if (validacion != null) {
            return Resultado.fracaso(validacion);
        }
        DiaMenu dia = dias.findByFecha(fecha)
                .orElseGet(() -> dias.save(new DiaMenu(Identificador.nuevo().valor(), semana, fecha)));
        if (opciones.existsByDiaIdAndProductoId(dia.getId(), productoId)) {
            return Resultado.fracaso(ErrorProgramacion.OPCION_REPETIDA);
        }
        return Resultado.exito(opciones.save(new OpcionMenu(Identificador.nuevo().valor(), dia, producto, componente,
                tipoMenu, porcionesEstimadas)));
    }
}

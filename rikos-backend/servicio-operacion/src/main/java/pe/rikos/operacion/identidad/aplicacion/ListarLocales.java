package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;

import java.util.List;

@Service
public class ListarLocales {

    private final LocalRepositorio locales;

    public ListarLocales(LocalRepositorio locales) {
        this.locales = locales;
    }

    @Transactional(readOnly = true)
    public List<Local> ejecutar() {
        return locales.findAllByOrderByNombreAsc();
    }
}

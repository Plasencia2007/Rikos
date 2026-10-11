package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionista;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionistaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Responsable;
import pe.rikos.operacion.pensionistas.infraestructura.ResponsableRepositorio;

import java.util.List;
import java.util.Optional;

/** Lecturas de fichas. Ninguna devuelve el PIN ni la contrasena del portal en claro: no existen en claro. */
@Component
public class ConsultaPensionistas {

    private final PensionistaRepositorio pensionistas;
    private final ResponsableRepositorio responsables;
    private final CuentaPensionistaRepositorio cuentas;

    public ConsultaPensionistas(PensionistaRepositorio pensionistas, ResponsableRepositorio responsables,
                                CuentaPensionistaRepositorio cuentas) {
        this.pensionistas = pensionistas;
        this.responsables = responsables;
        this.cuentas = cuentas;
    }

    /** Activos e inactivos, por apellido: la pantalla filtra. */
    @Transactional(readOnly = true)
    public List<Pensionista> listar() {
        return pensionistas.findAllByOrderByApellidosAscNombresAsc();
    }

    /** Por nombre completo o DNI (contiene). */
    @Transactional(readOnly = true)
    public List<Pensionista> buscar(String texto) {
        return texto == null || texto.isBlank() ? listar() : pensionistas.buscar(texto.trim());
    }

    @Transactional(readOnly = true)
    public Optional<Pensionista> pensionista(String id) {
        return pensionistas.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Responsable> responsable(String pensionistaId) {
        return responsables.findByPensionistaId(pensionistaId);
    }

    @Transactional(readOnly = true)
    public Optional<CuentaPensionista> cuenta(String pensionistaId) {
        return cuentas.findByPensionistaId(pensionistaId);
    }
}

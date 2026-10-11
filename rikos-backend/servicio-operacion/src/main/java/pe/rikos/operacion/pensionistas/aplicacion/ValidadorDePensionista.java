package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Component;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifarioRepositorio;

import java.util.Set;
import java.util.regex.Pattern;

/** Reglas comunes al alta y a la edicion de una ficha. Devuelve el primer error, o null si todo esta bien. */
@Component
class ValidadorDePensionista {

    private static final Pattern DNI = Pattern.compile("^[0-9]{8}$");
    private static final Set<String> FRECUENCIAS = Set.of("semanal", "quincenal", "mensual");

    private final PensionistaRepositorio pensionistas;
    private final PlanTarifarioRepositorio planes;

    ValidadorDePensionista(PensionistaRepositorio pensionistas, PlanTarifarioRepositorio planes) {
        this.pensionistas = pensionistas;
        this.planes = planes;
    }

    /** idExcluido: el propio pensionista al editar (conservar su DNI no es un duplicado); null al crear. */
    ErrorPensionista validar(DatosPensionista d, String idExcluido) {
        if (d.dni() == null || !DNI.matcher(d.dni()).matches()) {
            return ErrorPensionista.DNI_INVALIDO;
        }
        boolean duplicado = idExcluido == null ? pensionistas.existsByDni(d.dni())
                : pensionistas.existsByDniAndIdNot(d.dni(), idExcluido);
        if (duplicado) {
            return ErrorPensionista.DNI_DUPLICADO;
        }
        if (vacio(d.nombres()) || vacio(d.apellidos())) {
            return ErrorPensionista.NOMBRES_OBLIGATORIOS;
        }
        if (d.planId() == null || !planes.existsById(d.planId())) {
            return ErrorPensionista.PLAN_NO_ENCONTRADO;
        }
        if (d.comidas() == null || d.comidas().isEmpty()) {
            return ErrorPensionista.SIN_COMIDAS;
        }
        if (d.frecuenciaAbono() != null && !FRECUENCIAS.contains(d.frecuenciaAbono())) {
            return ErrorPensionista.FRECUENCIA_INVALIDA;
        }
        DatosResponsable r = d.responsable();
        if (r == null) {
            return ErrorPensionista.RESPONSABLE_OBLIGATORIO;
        }
        if (vacio(r.nombreCompleto()) || vacio(r.telefono()) || vacio(r.correo()) || vacio(r.parentesco())
                || vacio(r.direccion())) {
            return ErrorPensionista.RESPONSABLE_INCOMPLETO;
        }
        if (r.dni() == null || !DNI.matcher(r.dni()).matches()) {
            return ErrorPensionista.RESPONSABLE_DNI_INVALIDO;
        }
        return null;
    }

    static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    /** Recorta y convierte "" en null: los opcionales vacios no se guardan como texto en blanco. */
    static String limpio(String texto) {
        return vacio(texto) ? null : texto.trim();
    }
}

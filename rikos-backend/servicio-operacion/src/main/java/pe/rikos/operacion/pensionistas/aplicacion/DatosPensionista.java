package pe.rikos.operacion.pensionistas.aplicacion;

import java.util.Set;

/**
 * Lo que la pantalla manda al dar de alta o editar una ficha. No incluye
 * credenciales ni estado: el PIN lo genera el sistema y el estado tiene su
 * propio caso de uso. comidas = las habituales, con las que se calcula el tope
 * (no limitan lo que puede consumir). frecuenciaAbono opcional.
 */
public record DatosPensionista(String dni, String nombres, String apellidos, String telefono, String correo,
                               String institucion, String codigoEstudiante, String carrera, String ciclo,
                               String planId, Set<Comida> comidas, String frecuenciaAbono,
                               DatosResponsable responsable) {
}

package pe.rikos.operacion.pensionistas.aplicacion;

/** Los seis datos del responsable; todos obligatorios (US-016 CA2). */
public record DatosResponsable(String nombreCompleto, String dni, String telefono, String correo, String parentesco,
                               String direccion) {
}

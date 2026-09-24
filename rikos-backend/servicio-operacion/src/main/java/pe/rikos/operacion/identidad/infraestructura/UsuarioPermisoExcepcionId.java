package pe.rikos.operacion.identidad.infraestructura;

import java.io.Serializable;
import java.util.Objects;

// Los nombres de campo deben coincidir con los campos @Id de UsuarioPermisoExcepcion
// (usuarioId, permisoId): asi lo exige @IdClass.
public class UsuarioPermisoExcepcionId implements Serializable {

    private String usuarioId;
    private String permisoId;

    public UsuarioPermisoExcepcionId() {
    }

    public UsuarioPermisoExcepcionId(String usuarioId, String permisoId) {
        this.usuarioId = usuarioId;
        this.permisoId = permisoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioPermisoExcepcionId that)) return false;
        return Objects.equals(usuarioId, that.usuarioId) && Objects.equals(permisoId, that.permisoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, permisoId);
    }
}

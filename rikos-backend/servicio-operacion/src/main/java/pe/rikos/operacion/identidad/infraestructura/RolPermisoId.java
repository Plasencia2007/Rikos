package pe.rikos.operacion.identidad.infraestructura;

import java.io.Serializable;
import java.util.Objects;

// Los nombres de campo deben coincidir con los campos @Id de RolPermiso
// (rolId, permisoId): asi lo exige @IdClass.
public class RolPermisoId implements Serializable {

    private String rolId;
    private String permisoId;

    public RolPermisoId() {
    }

    public RolPermisoId(String rolId, String permisoId) {
        this.rolId = rolId;
        this.permisoId = permisoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolPermisoId that)) return false;
        return Objects.equals(rolId, that.rolId) && Objects.equals(permisoId, that.permisoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rolId, permisoId);
    }
}

package pe.rikos.operacion.identidad.infraestructura;

import java.io.Serializable;
import java.util.Objects;

// Los nombres de campo deben coincidir con los campos @Id de UsuarioRol
// (usuarioId, rolId): asi lo exige @IdClass.
public class UsuarioRolId implements Serializable {

    private String usuarioId;
    private String rolId;

    public UsuarioRolId() {
    }

    public UsuarioRolId(String usuarioId, String rolId) {
        this.usuarioId = usuarioId;
        this.rolId = rolId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioRolId that)) return false;
        return Objects.equals(usuarioId, that.usuarioId) && Objects.equals(rolId, that.rolId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, rolId);
    }
}

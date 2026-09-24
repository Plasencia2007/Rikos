package pe.rikos.operacion.identidad.infraestructura;

import java.io.Serializable;
import java.util.Objects;

// Los nombres de campo deben coincidir con los campos @Id de UsuarioLocal
// (usuarioId, localId): asi lo exige @IdClass.
public class UsuarioLocalId implements Serializable {

    private String usuarioId;
    private String localId;

    public UsuarioLocalId() {
    }

    public UsuarioLocalId(String usuarioId, String localId) {
        this.usuarioId = usuarioId;
        this.localId = localId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioLocalId that)) return false;
        return Objects.equals(usuarioId, that.usuarioId) && Objects.equals(localId, that.localId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, localId);
    }
}

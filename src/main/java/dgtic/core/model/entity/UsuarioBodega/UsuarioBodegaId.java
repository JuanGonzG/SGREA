package dgtic.core.model.entity.UsuarioBodega;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UsuarioBodegaId implements Serializable {
    private Integer idUsuario;
    private Integer idBodega;

    // Implementación de equals y hashCode para la clave compuesta
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioBodegaId)) return false;

        UsuarioBodegaId that = (UsuarioBodegaId) o;

        return Objects.equals(idUsuario, that.idUsuario)
                && Objects.equals(idBodega, that.idBodega);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuario, idBodega);
    }
}


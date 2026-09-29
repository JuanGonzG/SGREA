package dgtic.core.model.entity.UsuarioBodega;

import dgtic.core.model.entity.BodegaEntity;
import dgtic.core.model.entity.UsuarioEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuariobodega")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioBodegaEntity {
    @EmbeddedId
    private UsuarioBodegaId id;

    // Relación N:1 con Usuario
    @ManyToOne
    @MapsId("idUsuario")
    @JoinColumn(name = "id_usuario")
    private UsuarioEntity usuario;

    // Relación N:1 con Bodega
    @ManyToOne
    @MapsId("idBodega")
    @JoinColumn(name = "id_bodega")
    private BodegaEntity bodega;
}

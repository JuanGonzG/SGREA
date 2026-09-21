package dgtic.core.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bodega")
@Getter
@Setter
@Builder
//@ToString(exclude = "productos")
@NoArgsConstructor
@AllArgsConstructor
public class BodegaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bodega")
    private Integer idBodega;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 255)
    private String descripcion;
}

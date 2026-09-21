package dgtic.core.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "producto")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo;

    @Column(nullable = false, length = 255)
    private String urlImagen;

    // Relación N:1 con Bodega --> muchos productos pertenecen a una bodega
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bodega")
    private BodegaEntity bodega;
}

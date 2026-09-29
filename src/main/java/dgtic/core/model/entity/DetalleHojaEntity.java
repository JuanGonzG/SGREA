package dgtic.core.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "detallehoja",
        // Definición de la restricción de unicidad para evitar duplicados de id_hoja y id_producto
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_detalle_hoja_producto",
                        columnNames = {"id_hoja", "id_producto"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleHojaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    // Relación N:1 con HojaProduccion --> muchos detalles pueden pertenecer a una hoja de producción
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hoja", nullable = false)
    private HojaProduccionEntity hojaProduccionEntity;

    // Relación N:1 con Producto --> muchos detalles pueden referirse a un producto
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private ProductoEntity producto;

    @Column(name = "cantidad_solicitada", nullable = false)
    private Integer cantidadSolicitada;

    @Column(name = "cantidad_surtida")
    private Integer cantidadSurtida;

    @Column(name = "cantidad_devuelta", nullable = false)
    private Integer cantidadDevuelta;

    // Método para inicializar cantidadDevuelta antes de persistir la entidad
    @PrePersist
    private void inicializarCantidadDevuelta() {
        if (cantidadDevuelta == null) {
            cantidadDevuelta = 0;
        }
    }
}

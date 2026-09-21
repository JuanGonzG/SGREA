package dgtic.core.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hojaproduccion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HojaProduccionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hoja")
    private Integer idHoja;

    @Column(name = "nombre_proyecto", nullable = false, length = 100)
    private String nombreProyecto;

    @Column(name="cliente", nullable = false, length = 100)
    private String cliente;

    @Column(name = "fecha_salida", nullable = false)
    private LocalDateTime fechaSalida;

    @Column(name = "fecha_estimada_regreso")
    private LocalDateTime fechaEstimadaRegreso;

    // Relación N:1 con BodegaEntity --> muchas hojas de producción pueden estar asociadas a una bodega
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bodega", nullable = false)
    private BodegaEntity bodega;

    // Relación N:1 con EstadoHojaEntity --> muchas hojas de producción pueden tener un estado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_hoja", nullable = false)
    private EstadoHojaEntity estadoHoja;
}

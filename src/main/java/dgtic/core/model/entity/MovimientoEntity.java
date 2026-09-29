package dgtic.core.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer idMovimiento;

    // Relación N:1 con TipoMovimientoEntity --> muchos movimientos pueden estar asociados a un tipo de movimiento
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_movimiento", nullable = false)
    private TipoMovimientoEntity tipoMovimiento;

    // Relación N:1 con ConjuntoEntity --> muchos movimientos pueden estar asociados a un conjunto
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_conjunto", nullable = false)
    private ConjuntoEntity conjunto;

    // Relación N:1 con HojaProduccionEntity --> muchos movimientos pueden estar asociados a una hoja de producción
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_hoja", nullable = false)
    private HojaProduccionEntity hojaProduccion;

    // Relación N:1 con DetalleHojaEntity --> muchos movimientos pueden estar asociados a un detalle de hoja
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_detalle")
    private DetalleHojaEntity detalleHoja;

    // Relación N:1 con HojaContenedorEntity --> muchos movimientos pueden estar asociados a una hoja de contenedor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hoja_contenedor")
    private HojaContenedorEntity hojaContenedor;

    // Relación N:1 con UsuarioEntity --> muchos movimientos pueden estar asociados a un usuario
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 255)
    private String observaciones;
}

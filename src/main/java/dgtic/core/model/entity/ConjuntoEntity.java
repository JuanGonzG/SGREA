package dgtic.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "conjunto")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConjuntoEntity {
    @Id
    @Column(name = "id_conjunto", length = 50)
    private String idConjunto;

    // Relación N:1 con Producto --> muchos conjuntos pueden referirse a un producto
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private ProductoEntity producto;

    // Relación N:1 con EstadoConjunto --> muchos conjuntos pueden tener un mismo estado
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estado_conjunto", nullable = false)
    private EstadoConjuntoEntity estadoConjunto;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

    @Column(length = 255)
    private String observaciones;
}

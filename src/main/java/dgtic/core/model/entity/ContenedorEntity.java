package dgtic.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "contenedor")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContenedorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contenedor")
    private Integer idContenedor;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false)
    private Integer capacidad;

    // Relación N:1 con EstadoContenedor --> muchos contenedores pueden tener el mismo estado
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estado_contenedor", nullable = false)
    private EstadoContenedorEntity estadoContenedor;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

    @Column(length = 255)
    private String observaciones;
}

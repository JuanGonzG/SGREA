package dgtic.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estadoconjunto")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoConjuntoEntity {
    @Id
    @Column(name = "id_estado_conjunto")
    private Integer idEstadoConjunto;

    @Column(nullable = false, length = 100)
    private String nombre;
}

package dgtic.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "estadohoja")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoHojaEntity {
    @Id
    @Column(name = "id_estado_hoja")
    private Integer idEstadoHoja;

    @Column(nullable = false, length = 50)
    private String nombre;
}

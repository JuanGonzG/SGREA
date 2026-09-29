package dgtic.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "tipomovimiento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoMovimientoEntity {
    @Id
    @Column(name = "id_tipo_movimiento")
    private Integer idTipoMovimiento;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;
}

package dgtic.core.model.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoMovimientoDTO {
    private Integer idTipoMovimiento;
    private String nombre;
}

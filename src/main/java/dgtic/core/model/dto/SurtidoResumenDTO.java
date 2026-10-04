package dgtic.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurtidoResumenDTO {
    private Integer totalSolicitado;
    private Integer totalSurtido;
    private Integer totalPendiente;
    private Integer porcentaje;
    private Boolean completo;
}

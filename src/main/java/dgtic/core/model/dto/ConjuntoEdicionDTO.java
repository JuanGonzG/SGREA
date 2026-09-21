package dgtic.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConjuntoEdicionDTO {
    private String idConjunto;
    private Integer idEstadoConjunto;
    private String observaciones;
}

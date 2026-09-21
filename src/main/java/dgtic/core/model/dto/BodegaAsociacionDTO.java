package dgtic.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BodegaAsociacionDTO {
    private Integer idBodega;
    private String nombre;
    private String descripcion;
    private Boolean asociado;
}

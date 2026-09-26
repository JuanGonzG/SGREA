package dgtic.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContenedorEdicionDTO {
    private Integer capacidad;
    private Integer idEstadoContenedor;
    private String observaciones;
}

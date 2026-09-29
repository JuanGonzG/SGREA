package dgtic.core.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoEntradaDTO {
    @NotNull(message = "La hoja de producción es obligatoria")
    private Integer idHoja;

    @NotNull(message = "El conjunto es obligatorio")
    private String idConjunto;

    private Integer idHojaContenedor;

    @Size(max = 255, message = "Las observaciones no pueden superar 255 caracteres")
    private String observaciones;
}

package dgtic.core.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HojaContenedorCierreDTO {
    @NotNull(message = "La asignación HojaContenedor es obligatoria")
    private Integer idHojaContenedor;
}

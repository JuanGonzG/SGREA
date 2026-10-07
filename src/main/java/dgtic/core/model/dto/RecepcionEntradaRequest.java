package dgtic.core.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecepcionEntradaRequest {
    @NotBlank(message = "El código del conjunto es obligatorio")
    @Size(max = 50, message = "El código del conjunto no puede superar 50 caracteres")
    private String codigoConjunto;

    private Integer idHojaContenedor;
}

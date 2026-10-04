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
public class SurtidoAsignarContenedorRequest {
    @NotBlank(message = "El código del contenedor es obligatorio")
    @Size(max = 50, message = "El código del contenedor no puede superar 50 caracteres")
    private String codigo;
}

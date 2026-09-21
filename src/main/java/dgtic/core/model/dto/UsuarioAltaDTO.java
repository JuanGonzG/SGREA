package dgtic.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// DTO para la creación de un nuevo usuario (Nunca devolver en respuestas)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioAltaDTO {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
    private String nombre;

    @NotBlank(message = "El username es obligatorio")
    @Size(max = 255, message = "El username no puede exceder 255 caracteres")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 255, message = "La contraseña debe tener entre 8 y 255 caracteres")
    private String password;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activo;

    @NotNull(message = "El rol es obligatorio")
    @Valid
    private RolDTO rol;

    @NotNull(message = "Debe indicar al menos una bodega")
    @Size(min = 1, message = "El usuario debe tener al menos una bodega")
    private List<Integer> idsBodegas;
}

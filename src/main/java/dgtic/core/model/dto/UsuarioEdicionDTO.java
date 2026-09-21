package dgtic.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// DTO para la edición de un usuario existente (Contraseña opcional, nunca devolver en respuestas)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioEdicionDTO {
    @NotNull(message = "El identificador del usuario es obligatorio")
    private Integer idUsuario;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
    private String nombre;

    @NotBlank(message = "El username es obligatorio")
    @Size(max = 255, message = "El username no puede exceder 255 caracteres")
    private String username;

    @Pattern(regexp = "^$|^.{8,255}$", message = "La contraseña debe tener entre 8 y 255 caracteres")
    private String password;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activo;

    @NotNull(message = "El rol es obligatorio")
    @Valid
    private RolDTO rol;
}

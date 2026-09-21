package dgtic.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioDTO {
    private Integer idUsuario;
    private String nombre;
    private String username;
    private Boolean activo;
    private RolDTO rol;
    private List<BodegaDTO> bodegas;
}

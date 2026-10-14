package dgtic.core.model.dto.operacion;
import dgtic.core.model.dto.catalogo.*;
import dgtic.core.model.dto.seguridad.*;
import dgtic.core.model.dto.operacion.*;
import dgtic.core.model.dto.hh.surtido.*;
import dgtic.core.model.dto.hh.recepcion.*;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HojaContenedorAsignacionDTO {
    @NotNull(message = "La hoja de producción es obligatoria")
    private Integer idHoja;

    @NotNull(message = "El contenedor es obligatorio")
    private Integer idContenedor;

    @Size(max = 255, message = "Las observaciones no pueden superar 255 caracteres")
    private String observaciones;
}

package dgtic.core.model.dto.operacion;
import dgtic.core.model.dto.catalogo.*;
import dgtic.core.model.dto.seguridad.*;
import dgtic.core.model.dto.operacion.*;
import dgtic.core.model.dto.hh.surtido.*;
import dgtic.core.model.dto.hh.recepcion.*;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HojaContenedorLiberacionDTO {
    @NotNull(message = "La asignación HojaContenedor es obligatoria")
    private Integer idHojaContenedor;
}

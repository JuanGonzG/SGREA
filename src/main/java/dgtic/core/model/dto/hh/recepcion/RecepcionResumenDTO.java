package dgtic.core.model.dto.hh.recepcion;
import dgtic.core.model.dto.catalogo.*;
import dgtic.core.model.dto.seguridad.*;
import dgtic.core.model.dto.operacion.*;
import dgtic.core.model.dto.hh.surtido.*;
import dgtic.core.model.dto.hh.recepcion.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecepcionResumenDTO {
    private Integer totalSurtido;
    private Integer totalDevuelto;
    private Integer totalPendiente;
    private Integer porcentaje;
    private Boolean completo;
}

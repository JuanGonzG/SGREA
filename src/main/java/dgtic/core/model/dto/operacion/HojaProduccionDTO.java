package dgtic.core.model.dto.operacion;
import dgtic.core.model.dto.catalogo.*;
import dgtic.core.model.dto.seguridad.*;
import dgtic.core.model.dto.operacion.*;
import dgtic.core.model.dto.hh.surtido.*;
import dgtic.core.model.dto.hh.recepcion.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HojaProduccionDTO {
    private Integer idHoja;
    private BodegaDTO bodega;
    private String nombreProyecto;
    private String cliente;
    private LocalDateTime fechaSalida;
    private LocalDateTime fechaEstimadaRegreso;
    private EstadoHojaDTO estadoHoja;
}

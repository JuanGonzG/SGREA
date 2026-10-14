package dgtic.core.model.dto.hh.surtido;
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
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurtidoEstadoDTO {
    private Integer idHoja;
    private String nombreProyecto;
    private String cliente;
    private LocalDateTime fechaSalida;
    private Integer totalSolicitado;
    private Integer totalSurtido;
    private Integer totalPendiente;
    private Integer porcentaje;
    private Boolean completo;
    private SurtidoContenedorDTO contenedorActivo;
    private List<SurtidoDetalleDTO> detalles;
    private List<SurtidoMovimientoDTO> ultimosMovimientos;
}

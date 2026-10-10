package dgtic.core.model.dto.reporte;

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
public class ReporteOperacionHojaDTO {
    private Integer idHoja;
    private String nombreProyecto;
    private String cliente;
    private String estado;
    private LocalDateTime fechaSalida;
    private LocalDateTime fechaEstimadaRegreso;
    private LocalDateTime inicioOperacion;
    private LocalDateTime finOperacion;
    private Integer totalSolicitado;
    private Integer totalSurtido;
    private Integer totalDevuelto;
    private List<ReporteOperacionDetalleDTO> detalles;
    private List<ReporteMovimientoDTO> movimientos;
    private List<ReporteContenedorDTO> contenedores;
}

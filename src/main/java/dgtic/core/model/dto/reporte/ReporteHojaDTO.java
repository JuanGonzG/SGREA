package dgtic.core.model.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteHojaDTO {
    private Integer idHoja;
    private String nombreProyecto;
    private String cliente;
    private Integer idEstado;
    private String estado;
    private LocalDateTime fechaSalida;
    private LocalDateTime fechaEstimadaRegreso;
    private Integer totalSolicitado;
    private Integer totalSurtido;
    private Integer totalDevuelto;
    private Integer porcentajeSurtido;
    private Integer porcentajeRecepcion;
}

package dgtic.core.model.dto.monitoreo;

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
public class MonitoreoEstadoDTO {
    private Integer idHoja;
    private String nombreProyecto;
    private String cliente;
    private LocalDateTime fechaSalida;
    private LocalDateTime fechaEstimadaRegreso;
    private Integer idEstado;
    private String estado;
    private Integer totalSolicitado;
    private Integer totalSurtido;
    private Integer totalDevuelto;
    private Integer pendienteSurtir;
    private Integer pendienteDevolver;
    private Integer porcentajeSurtido;
    private Integer porcentajeRecepcion;
    private LocalDateTime inicioSurtido;
    private LocalDateTime inicioRecepcion;
    private LocalDateTime ultimaActividad;
    private String ultimoOperador;
    private List<MonitoreoDetalleDTO> detalles;
    private List<MonitoreoContenedorDTO> contenedores;
    private List<MonitoreoMovimientoDTO> movimientos;
}

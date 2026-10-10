package dgtic.core.model.dto.monitoreo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoreoHojaDTO {
    private Integer idHoja;
    private String nombreProyecto;
    private String cliente;
    private Integer idEstado;
    private String estado;
    private Integer total;
    private Integer procesado;
    private Integer pendiente;
    private Integer porcentaje;
    private LocalDateTime inicioOperacion;
    private LocalDateTime ultimaActividad;
    private String ultimoOperador;
    private Integer contenedoresPendientes;
}

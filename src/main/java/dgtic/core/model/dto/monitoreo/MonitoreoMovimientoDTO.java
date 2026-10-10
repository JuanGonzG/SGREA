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
public class MonitoreoMovimientoDTO {
    private Integer idMovimiento;
    private Integer idTipoMovimiento;
    private String tipoMovimiento;
    private String codigoConjunto;
    private String producto;
    private String codigoContenedor;
    private LocalDateTime fecha;
    private Integer idUsuario;
    private String usuario;
}

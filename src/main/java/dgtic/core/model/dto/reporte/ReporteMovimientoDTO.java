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
public class ReporteMovimientoDTO {
    private Integer idMovimiento;
    private LocalDateTime fecha;
    private Integer idTipoMovimiento;
    private String tipoMovimiento;
    private Integer idHoja;
    private String proyecto;
    private String codigoConjunto;
    private Integer idProducto;
    private String producto;
    private String codigoContenedor;
    private Integer idUsuario;
    private String usuario;
    private String observaciones;
}

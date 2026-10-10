package dgtic.core.model.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteOperacionDetalleDTO {
    private Integer idDetalle;
    private Integer idProducto;
    private String producto;
    private Integer cantidadSolicitada;
    private Integer cantidadSurtida;
    private Integer cantidadDevuelta;
}

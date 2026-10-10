package dgtic.core.model.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteConjuntoFiltroDTO {
    private Integer idProducto;
    private Integer idEstadoConjunto;
    private String codigoConjunto;
}

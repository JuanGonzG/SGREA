package dgtic.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleHojaDTO {
    private Integer idDetalleHoja;
    private HojaProduccionDTO hojaProduccion;
    private ProductoDTO producto;
    private Integer cantidadSolicitada;
    private Integer cantidadSurtida;
}

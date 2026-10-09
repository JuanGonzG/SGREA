package dgtic.core.model.dto.monitoreo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoreoDetalleDTO {
    private Integer idDetalle;
    private Integer idProducto;
    private String producto;
    private Integer cantidadSolicitada;
    private Integer cantidadSurtida;
    private Integer cantidadDevuelta;
    private Integer pendienteSurtir;
    private Integer pendienteDevolver;
    private Integer porcentajeSurtido;
    private Integer porcentajeRecepcion;
}

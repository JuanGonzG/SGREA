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
public class ReporteHojaDetalleDTO {
    private Integer idHoja;
    private String nombreProyecto;
    private String cliente;
    private String estado;
    private LocalDateTime fechaSalida;
    private LocalDateTime fechaEstimadaRegreso;
    private Integer totalSolicitado;
    private Integer totalSurtido;
    private Integer totalDevuelto;
    private Integer porcentajeSurtido;
    private Integer porcentajeRecepcion;
    private List<Detalle> detalles;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Detalle {
        private Integer idProducto;
        private String producto;
        private Integer cantidadSolicitada;
        private Integer cantidadSurtida;
        private Integer cantidadDevuelta;
        private Integer pendienteSurtir;
        private Integer pendienteDevolver;
    }
}

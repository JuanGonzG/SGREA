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
public class ReporteConjuntoDTO {
    private String codigoConjunto;
    private Integer idProducto;
    private String producto;
    private Integer idEstado;
    private String estado;
    private LocalDateTime fechaAlta;
    private String observaciones;
}

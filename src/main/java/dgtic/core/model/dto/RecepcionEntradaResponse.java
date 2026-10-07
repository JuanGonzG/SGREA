package dgtic.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecepcionEntradaResponse {
    private String mensaje;
    private RecepcionMovimientoDTO movimiento;
    private RecepcionDetalleDTO detalle;
    private RecepcionContenedorDTO contenedor;
    private RecepcionResumenDTO hoja;
    private String estadoHoja;
    private Boolean recepcionCompleta;
}

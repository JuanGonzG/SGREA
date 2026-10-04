package dgtic.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurtidoSalidaResponse {
    private String mensaje;
    private SurtidoMovimientoDTO movimiento;
    private SurtidoDetalleDTO detalle;
    private SurtidoContenedorDTO contenedor;
    private SurtidoResumenDTO hoja;
}

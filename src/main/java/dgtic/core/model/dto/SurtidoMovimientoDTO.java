package dgtic.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurtidoMovimientoDTO {
    private Integer idMovimiento;
    private String codigoConjunto;
    private String producto;
    private String codigoContenedor;
    private LocalDateTime fecha;
    private String usuario;
}

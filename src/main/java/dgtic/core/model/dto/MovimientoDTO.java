package dgtic.core.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoDTO {
    private Integer idMovimiento;
    private TipoMovimientoDTO tipoMovimiento;
    private ConjuntoDTO conjunto;
    private HojaProduccionDTO hojaProduccion;
    private DetalleHojaDTO detalleHoja;
    private HojaContenedorDTO hojaContenedor;
    private UsuarioDTO usuario;
    private LocalDateTime fecha;
    private String observaciones;
}

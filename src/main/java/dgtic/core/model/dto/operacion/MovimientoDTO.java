package dgtic.core.model.dto.operacion;
import dgtic.core.model.dto.catalogo.*;
import dgtic.core.model.dto.seguridad.*;
import dgtic.core.model.dto.operacion.*;
import dgtic.core.model.dto.hh.surtido.*;
import dgtic.core.model.dto.hh.recepcion.*;

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

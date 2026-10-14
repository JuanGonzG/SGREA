package dgtic.core.model.dto.hh.surtido;
import dgtic.core.model.dto.catalogo.*;
import dgtic.core.model.dto.seguridad.*;
import dgtic.core.model.dto.operacion.*;
import dgtic.core.model.dto.hh.surtido.*;
import dgtic.core.model.dto.hh.recepcion.*;

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

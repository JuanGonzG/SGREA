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
public class SurtidoCerrarCargaResponse {
    private String mensaje;
    private SurtidoContenedorDTO contenedor;
    private Boolean requiereNuevoContenedor;
    private Boolean surtidoCompleto;
}

package dgtic.core.model.dto.hh.recepcion;
import dgtic.core.model.dto.catalogo.*;
import dgtic.core.model.dto.seguridad.*;
import dgtic.core.model.dto.operacion.*;
import dgtic.core.model.dto.hh.surtido.*;
import dgtic.core.model.dto.hh.recepcion.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecepcionLiberarContenedorResponse {
    private String mensaje;
    private RecepcionContenedorDTO contenedor;
    private List<RecepcionContenedorDTO> contenedoresPendientes;
    private Boolean recepcionCompleta;
}

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
public class SurtidoContenedorDTO {
    private Integer idHojaContenedor;
    private Integer idContenedor;
    private String codigo;
    private Integer capacidad;
    private Integer ocupacion;
    private Boolean disponible;
    private Integer porcentaje;
    private Boolean cargaCerrada;
    private Boolean lleno;
}

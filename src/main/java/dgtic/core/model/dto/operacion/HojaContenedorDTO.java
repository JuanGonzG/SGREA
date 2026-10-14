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
public class HojaContenedorDTO {
    private Integer idHojaContenedor;
    private HojaProduccionDTO hojaProduccion;
    private ContenedorDTO contenedor;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaCierreCarga;
    private LocalDateTime fechaLiberacion;
    private UsuarioDTO usuarioAsignacion;
    private UsuarioDTO usuarioCierre;
    private UsuarioDTO usuarioLiberacion;
    private String observaciones;
}

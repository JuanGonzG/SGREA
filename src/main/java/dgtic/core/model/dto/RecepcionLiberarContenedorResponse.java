package dgtic.core.model.dto;

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

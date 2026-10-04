package dgtic.core.model.dto;

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

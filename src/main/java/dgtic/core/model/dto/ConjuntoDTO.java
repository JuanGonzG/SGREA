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
public class ConjuntoDTO {
    private String idConjunto;
    private ProductoDTO producto;
    private EstadoConjuntoDTO estadoConjunto;
    private LocalDateTime fechaAlta;
    private String observaciones;
}

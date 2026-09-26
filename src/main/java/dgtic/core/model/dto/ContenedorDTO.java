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
public class ContenedorDTO {
    private Integer idContenedor;
    private String codigo;
    private Integer capacidad;
    private EstadoContenedorDTO estadoContenedor;
    private LocalDateTime fechaAlta;
    private String observaciones;
}

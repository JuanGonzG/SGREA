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
public class RecepcionContenedorDTO {
    private Integer idHojaContenedor;
    private Integer idContenedor;
    private String codigo;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaCierreCarga;
    private LocalDateTime fechaLiberacion;
    private Boolean liberado;
    private Boolean puedeRecibirEntradas;
}

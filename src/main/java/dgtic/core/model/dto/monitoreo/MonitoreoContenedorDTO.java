package dgtic.core.model.dto.monitoreo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoreoContenedorDTO {
    private Integer idHojaContenedor;
    private Integer idContenedor;
    private String codigo;
    private Integer capacidad;
    private Integer ocupacion;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaCierreCarga;
    private LocalDateTime fechaLiberacion;
    private Boolean cargaCerrada;
    private Boolean liberado;
}

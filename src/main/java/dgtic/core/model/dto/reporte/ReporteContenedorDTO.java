package dgtic.core.model.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteContenedorDTO {
    private Integer idHojaContenedor;
    private Integer idContenedor;
    private String codigoContenedor;
    private Integer capacidad;
    private Integer idHoja;
    private String proyecto;
    private LocalDateTime fechaAsignacion;
    private String usuarioAsignacion;
    private LocalDateTime fechaCierreCarga;
    private String usuarioCierre;
    private LocalDateTime fechaLiberacion;
    private String usuarioLiberacion;
    private Integer cantidadSalidas;
    private Integer porcentajeUtilizacion;
}

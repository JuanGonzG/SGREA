package dgtic.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoDTO {
    private Integer idProducto;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private String urlImagen;
    private BodegaDTO bodega;
}

package dgtic.core.service;

import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.operacion.DetalleHojaDTO;
import dgtic.core.model.dto.operacion.HojaProduccionDTO;
import dgtic.core.model.dto.catalogo.ProductoDTO;
import dgtic.core.model.entity.DetalleHojaEntity;
import dgtic.core.model.entity.HojaProduccionEntity;
import dgtic.core.model.entity.ProductoEntity;
import dgtic.core.repository.DetalleHojaRepository;
import dgtic.core.repository.MovimientoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DetalleHojaService {
    @Autowired
    private DetalleHojaRepository detalleHojaRepository;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private HojaProduccionService hojaProduccionService;

    @Autowired
    private MovimientoRepository movimientoRepository;

    // Obtener todos los detalles de hoja de producción
    public List<DetalleHojaDTO> getDetallesByHojaId(Integer idHoja) {
        return detalleHojaRepository.findByHojaProduccionEntity_IdHoja(idHoja)
                .stream()
                .map(detalle -> Mapper.toDetalleHojaDTO(detalle))
                .toList();
    }

    // Obtener un detalle de hoja de producción por su ID
    public DetalleHojaDTO getById(Integer idDetalleHoja) {
        return detalleHojaRepository.findById(idDetalleHoja)
                .map(detalle -> Mapper.toDetalleHojaDTO(detalle))
                .orElse(null);
    }

    // Verificar si un producto ya está asociado a una hoja, excluyendo el detalle actual al editar.
    public boolean existeProductoEnHoja(Integer idHoja, Integer idProducto, Integer idDetalleActual) {
        if (idDetalleActual == null) {
            return detalleHojaRepository.existsByHojaProduccionEntity_IdHojaAndProducto_IdProducto(
                    idHoja, idProducto);
        }
        return detalleHojaRepository.existsByHojaProduccionEntity_IdHojaAndProducto_IdProductoAndIdDetalleNot(
                idHoja, idProducto, idDetalleActual);
    }

    // Guardar o actualizar un detalle de hoja de producción
    public DetalleHojaDTO addDetalleHoja(DetalleHojaDTO detalleHojaDTO) {
        // Crear la entidad DetalleHojaEntity a partir del DTO
        DetalleHojaEntity detalleHojaEntity = Mapper.toDetalleHojaEntity(detalleHojaDTO);
        // Guardar la entidad en la base de datos
        DetalleHojaEntity detalleGuardado = detalleHojaRepository.save(detalleHojaEntity);
        log.info("Detalle de Hoja {} idDetalle={}, idHoja={}, idProducto={}",
                detalleHojaDTO.getIdDetalleHoja() == null ? "creado" : "actualizado",
                detalleGuardado.getIdDetalle(),
                detalleGuardado.getHojaProduccionEntity().getIdHoja(),
                detalleGuardado.getProducto().getIdProducto());
        return Mapper.toDetalleHojaDTO(detalleGuardado);
    }

    // Eliminar un detalle de hoja de producción por su ID
    public Boolean deleteDetalleHojaById(Integer idDetalleHoja) {
        // Verificar si el detalle existe
        if (detalleHojaRepository.existsById(idDetalleHoja)) {
            // Verificar si el detalle tiene movimientos asociados
            if (movimientoRepository.existsByDetalleHoja_IdDetalle(idDetalleHoja)) {
                log.warn("Eliminación de detalle rechazada por historial idDetalle={}", idDetalleHoja);
                throw new IllegalStateException(
                        "No se puede eliminar el detalle porque tiene historial de movimientos.");
            }
            detalleHojaRepository.deleteById(idDetalleHoja);
            log.info("Detalle de Hoja eliminado idDetalle={}", idDetalleHoja);
            return true;
        }
        log.warn("Eliminación de detalle rechazada: detalle inexistente idDetalle={}", idDetalleHoja);
        return false;
    }
}

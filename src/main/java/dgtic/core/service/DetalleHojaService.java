package dgtic.core.service;

import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.DetalleHojaDTO;
import dgtic.core.model.dto.HojaProduccionDTO;
import dgtic.core.model.dto.ProductoDTO;
import dgtic.core.model.entity.DetalleHojaEntity;
import dgtic.core.model.entity.HojaProduccionEntity;
import dgtic.core.model.entity.ProductoEntity;
import dgtic.core.repository.DetalleHojaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DetalleHojaService {
    @Autowired
    private DetalleHojaRepository detalleHojaRepository;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private HojaProduccionService hojaProduccionService;

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
        return Mapper.toDetalleHojaDTO(detalleGuardado);
    }

    // Eliminar un detalle de hoja de producción por su ID
    public Boolean deleteDetalleHojaById(Integer idDetalleHoja) {
        if (detalleHojaRepository.existsById(idDetalleHoja)) {
            detalleHojaRepository.deleteById(idDetalleHoja);
            return true;
        }
        return false;
    }
}

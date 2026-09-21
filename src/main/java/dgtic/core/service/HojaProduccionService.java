package dgtic.core.service;

import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.EstadoHojaDTO;
import dgtic.core.model.dto.HojaProduccionDTO;
import dgtic.core.repository.DetalleHojaRepository;
import dgtic.core.repository.EstadoHojaRepository;
import dgtic.core.repository.HojaProduccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HojaProduccionService {
    @Autowired
    private HojaProduccionRepository hojaProduccionRepository;
    @Autowired
    private EstadoHojaRepository estadoHojaRepository;
    @Autowired
    private DetalleHojaRepository detalleHojaRepository;

    // Obtener todas las hojas de producción de una bodega específica
    public List<HojaProduccionDTO> getHojasProduccionByBodegaId(Integer idBodega) {
        return hojaProduccionRepository.findByBodega_IdBodega(idBodega)
                .stream()
                .map(hoja -> Mapper.toHojaProduccionDTO(hoja))
                .toList();
    }

    // Obtener una hoja de producción por su ID
    public HojaProduccionDTO getById(Integer idHoja) {
        return hojaProduccionRepository.findById(idHoja)
                .map(hoja -> Mapper.toHojaProduccionDTO(hoja))
                .orElse(null);
    }

    // Guardar o actualizar una hoja de producción
    public HojaProduccionDTO addHojaProduccion(HojaProduccionDTO hojaProduccionDTO) {
        return Mapper.toHojaProduccionDTO(hojaProduccionRepository.save(Mapper.toHojaProduccionEntity(hojaProduccionDTO)));
    }

    // Eliminar una hoja de producción por su ID
    public Boolean deleteHojaProduccionById(Integer idHoja) {
        if (hojaProduccionRepository.existsById(idHoja)) {
            hojaProduccionRepository.deleteById(idHoja);
            return true;
        }
        return false;
    }

    // Eliminar una hoja de producción junto con sus detalles
    @Transactional
    public void deleteHojaProduccionConDetalles(Integer idHoja) {
        detalleHojaRepository.deleteByHojaProduccionEntity_IdHoja(idHoja);
        hojaProduccionRepository.deleteById(idHoja);
    }

    // Obtener el catálogo de estados de hoja
    public List<EstadoHojaDTO> getCatalogoEstadoHoja() {
        return estadoHojaRepository.findAll()
                .stream()
                .map(estado -> Mapper.toEstadoHojaDTO(estado))
                .toList();
    }
}

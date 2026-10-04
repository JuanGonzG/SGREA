package dgtic.core.service;

import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.EstadoHojaDTO;
import dgtic.core.model.dto.HojaProduccionDTO;
import dgtic.core.model.entity.EstadoHojaEntity;
import dgtic.core.model.entity.HojaProduccionEntity;
import dgtic.core.repository.DetalleHojaRepository;
import dgtic.core.repository.EstadoHojaRepository;
import dgtic.core.repository.HojaProduccionRepository;
import dgtic.core.repository.HojaContenedorRepository;
import dgtic.core.repository.MovimientoRepository;
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

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private HojaContenedorRepository hojaContenedorRepository;

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
    // Bloquear una hoja de producción
    @Transactional
    public void bloquearHoja(Integer idHoja, Integer idBodega) {
        // Obtener la hoja de producción con bloqueo para evitar condiciones de carrera
        HojaProduccionEntity hoja = hojaProduccionRepository.findByIdForUpdate(idHoja)
                .orElseThrow(() -> new IllegalArgumentException("La hoja de producción no existe."));
        // Validar que la hoja pertenezca a la bodega activa
        if (hoja.getBodega() == null || !idBodega.equals(hoja.getBodega().getIdBodega())) {
            throw new IllegalArgumentException("La hoja no pertenece a la bodega activa.");
        }
        // Validar que la hoja esté en estado "Creada" antes de bloquearla
        if (hoja.getEstadoHoja() == null || !Integer.valueOf(1).equals(hoja.getEstadoHoja().getIdEstadoHoja())) {
            throw new IllegalArgumentException("Solo se pueden bloquear hojas en estado Creada.");
        }
        // Validar que la hoja tenga detalles antes de bloquearla
        if (!detalleHojaRepository.findByHojaProduccionEntity_IdHoja(idHoja).iterator().hasNext()) {
            throw new IllegalArgumentException("No se puede bloquear una hoja sin detalles.");
        }
        // Cambiar el estado de la hoja a "Por surtir"
        EstadoHojaEntity estadoPorSurtir = estadoHojaRepository.findById(2)
                .orElseThrow(() -> new IllegalStateException("No existe el estado de hoja Por surtir."));
        hoja.setEstadoHoja(estadoPorSurtir);
        hojaProduccionRepository.save(hoja);
    }
    // Obtener un estado de hoja por su ID
    public EstadoHojaDTO getEstadoHojaById(Integer idEstadoHoja) {
        return estadoHojaRepository.findById(idEstadoHoja)
                .map(Mapper::toEstadoHojaDTO)
                .orElse(null);
    }

    // Eliminar una hoja de producción por su ID
    public Boolean deleteHojaProduccionById(Integer idHoja) {
        // Validar si la hoja de producción existe antes de eliminarla
        if (hojaProduccionRepository.existsById(idHoja)) {
            // Validar que no tenga dependencias históricas antes de eliminarla
            validarSinDependenciasHistoricas(idHoja);
            // Eliminar la hoja de producción
            hojaProduccionRepository.deleteById(idHoja);
            return true;
        }
        return false;
    }

    // Eliminar una hoja de producción junto con sus detalles
    @Transactional
    public void deleteHojaProduccionConDetalles(Integer idHoja) {
        // Validar que no tenga dependencias históricas antes de eliminarla
        validarSinDependenciasHistoricas(idHoja);
        // Eliminar los detalles de la hoja de producción
        detalleHojaRepository.deleteByHojaProduccionEntity_IdHoja(idHoja);
        // Eliminar la hoja de producción
        hojaProduccionRepository.deleteById(idHoja);
    }

    private void validarSinDependenciasHistoricas(Integer idHoja) {
        // Validar que no tenga movimientos o asignaciones de contenedor antes de eliminarla
        if (movimientoRepository.existsByHojaProduccion_IdHoja(idHoja)
                || hojaContenedorRepository.existsByHojaProduccion_IdHoja(idHoja)) {
            throw new IllegalStateException(
                    "No se puede eliminar la hoja porque tiene movimientos o asignaciones de contenedor.");
        }
    }

    // Obtener el catálogo de estados de hoja
    public List<EstadoHojaDTO> getCatalogoEstadoHoja() {
        return estadoHojaRepository.findAll()
                .stream()
                .map(estado -> Mapper.toEstadoHojaDTO(estado))
                .toList();
    }
}

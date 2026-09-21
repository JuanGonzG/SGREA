package dgtic.core.service;

import dgtic.core.repository.BodegaRepository;
import dgtic.core.repository.HojaProduccionRepository;
import dgtic.core.repository.ProductoRepository;
import dgtic.core.repository.SesionRepository;
import dgtic.core.repository.UsuarioBodegaRepository;
import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.BodegaDTO;
import dgtic.core.model.entity.BodegaEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BodegaService {
    @Autowired
    private BodegaRepository bodegaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private HojaProduccionRepository hojaProduccionRepository;

    @Autowired
    private UsuarioBodegaRepository usuarioBodegaRepository;

    @Autowired
    private SesionRepository sesionRepository;

    // Validar los datos de una bodega
    private void validar(BodegaDTO bodegaDTO) {
        // Validar que el nombre y la descripción no sean nulos o vacíos
        if (bodegaDTO == null || bodegaDTO.getNombre() == null || bodegaDTO.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la bodega es obligatorio.");
        }
        // Validar que la descripción no sea nula o vacía
        if (bodegaDTO.getDescripcion() == null || bodegaDTO.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("La descripción de la bodega es obligatoria.");
        }
        // Validar la longitud del nombre y la descripción
        if (bodegaDTO.getNombre().trim().length() > 100) {
            throw new IllegalArgumentException("El nombre no puede superar 100 caracteres.");
        }
        // Validar la longitud de la descripción
        if (bodegaDTO.getDescripcion().trim().length() > 255) {
            throw new IllegalArgumentException("La descripción no puede superar 255 caracteres.");
        }
    }

    // Obtener todas las bodegas
    public List<BodegaDTO> getBodegas() {
        return bodegaRepository.findAll().stream().map(Mapper::toBodegaDTO).toList();
    }

    // Obtener una bodega por su ID
    public Optional<BodegaDTO> getBodegaById(Integer idBodega) {
        return bodegaRepository.findById(idBodega).map(Mapper::toBodegaDTO);
    }

    // Guardar o actualizar una bodega
    public BodegaDTO save(BodegaDTO bodegaDTO) {
        // Validar los datos de la bodega
        validar(bodegaDTO);
        String nombre = bodegaDTO.getNombre().trim();
        Integer idBodega = bodegaDTO.getIdBodega();
        // Validar que la bodega exista si se está actualizando
        if (idBodega != null && !bodegaRepository.existsById(idBodega)) {
            throw new IllegalArgumentException("La bodega no existe.");
        }
        // Validar que no exista otra bodega con el mismo nombre (ignorando mayúsculas y minúsculas)
        boolean nombreDuplicado = idBodega == null
                ? bodegaRepository.existsByNombreIgnoreCase(nombre)
                : bodegaRepository.existsByNombreIgnoreCaseAndIdBodegaNot(nombre, idBodega);
        // Lanzar una excepción si ya existe una bodega con el mismo nombre
        if (nombreDuplicado) {
            throw new IllegalArgumentException("Ya existe una bodega con ese nombre.");
        }
        // Guardar la bodega en la base de datos
        bodegaDTO.setNombre(nombre);
        bodegaDTO.setDescripcion(bodegaDTO.getDescripcion().trim());
        BodegaEntity bodega = bodegaRepository.save(Mapper.toBodegaEntity(bodegaDTO));
        return Mapper.toBodegaDTO(bodega);
    }

    // Eliminar una bodega
    public void delete(Integer idBodega) {
        // Validar que la bodega exista antes de eliminarla
        if (!bodegaRepository.existsById(idBodega)) {
            throw new IllegalArgumentException("La bodega no existe.");
        }
        // Verificar si la bodega tiene información relacionada en otras tablas antes de eliminarla
        if (productoRepository.existsByBodega_IdBodega(idBodega)
                || hojaProduccionRepository.existsByBodega_IdBodega(idBodega)
                || usuarioBodegaRepository.existsByBodega_IdBodega(idBodega)
                || sesionRepository.existsByBodega_IdBodega(idBodega)) {
            throw new IllegalStateException("No se puede eliminar la bodega porque tiene información relacionada.");
        }
        // Eliminar la bodega de la base de datos
        bodegaRepository.deleteById(idBodega);
    }

}

package dgtic.core.service;

import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.HojaContenedorAsignacionDTO;
import dgtic.core.model.dto.HojaContenedorDTO;
import dgtic.core.model.entity.ContenedorEntity;
import dgtic.core.model.entity.EstadoContenedorEntity;
import dgtic.core.model.entity.HojaContenedorEntity;
import dgtic.core.model.entity.HojaProduccionEntity;
import dgtic.core.model.entity.UsuarioEntity;
import dgtic.core.repository.ContenedorRepository;
import dgtic.core.repository.EstadoContenedorRepository;
import dgtic.core.repository.HojaContenedorRepository;
import dgtic.core.repository.HojaProduccionRepository;
import dgtic.core.repository.UsuarioBodegaRepository;
import dgtic.core.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HojaContenedorService {
    private static final int ESTADO_DISPONIBLE = 1;
    private static final int ESTADO_EN_USO = 2;

    @Autowired
    private HojaContenedorRepository hojaContenedorRepository;

    @Autowired
    private HojaProduccionRepository hojaProduccionRepository;

    @Autowired
    private ContenedorRepository contenedorRepository;

    @Autowired
    private EstadoContenedorRepository estadoContenedorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioBodegaRepository usuarioBodegaRepository;

    // Obtener todas las asignaciones de contenedores para una hoja de producción específica
    @Transactional(readOnly = true)
    public List<HojaContenedorDTO> getByHoja(Integer idHoja, Integer idBodega) {
        // Validar que los IDs proporcionados sean válidos
        validarId(idHoja, "La hoja de producción es obligatoria.");
        validarId(idBodega, "La bodega es obligatoria.");
        // Validar que la hoja de producción pertenezca a la bodega especificada
        validarHojaEnBodega(idHoja, idBodega);
        // Obtener las asignaciones de contenedores asociadas a la hoja de producción y mapearlas a DTOs
        return hojaContenedorRepository.findByHojaProduccion_IdHoja(idHoja)
                .stream()
                .map(Mapper::toHojaContenedorDTO)
                .toList();
    }
    // Obtener una asignación de contenedor por su ID, asegurando que pertenezca a la bodega especificada
    @Transactional(readOnly = true)
    public HojaContenedorDTO getById(Integer idHojaContenedor, Integer idBodega) {
        // Validar que los IDs proporcionados sean válidos
        validarId(idHojaContenedor, "La asignación HojaContenedor es obligatoria.");
        validarId(idBodega, "La bodega es obligatoria.");
        // Buscar la asignación de contenedor por su ID y verificar que pertenezca a la bodega especificada
        return hojaContenedorRepository.findById(idHojaContenedor)
                .filter(asignacion -> perteneceBodega(asignacion.getHojaProduccion(), idBodega))
                .map(Mapper::toHojaContenedorDTO)
                .orElse(null);
    }
    // Asignar un contenedor a una hoja de producción
    @Transactional
    public HojaContenedorDTO asignar(HojaContenedorAsignacionDTO dto, Integer idUsuario, Integer idBodega) {
        // Validar que los datos de la asignación no sean nulos
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la asignación son obligatorios.");
        }
        // Validar que los IDs de hoja y contenedor sean válidos
        validarId(dto.getIdHoja(), "La hoja de producción es obligatoria.");
        validarId(dto.getIdContenedor(), "El contenedor es obligatorio.");
        // Obtener el usuario autorizado para la bodega especificada
        UsuarioEntity usuario = obtenerUsuarioAutorizado(idUsuario, idBodega);
        // Obtener la hoja de producción y validar que pertenezca a la bodega especificada
        HojaProduccionEntity hoja = hojaProduccionRepository.findByIdForUpdate(dto.getIdHoja())
                .orElseThrow(() -> new IllegalArgumentException("La hoja de producción no existe."));
        // Validar que la hoja de producción pertenezca a la bodega especificada
        validarHojaEnBodega(hoja, idBodega);

        // Obtener el contenedor y validar que esté disponible para asignación
        ContenedorEntity contenedor = contenedorRepository.findByIdForUpdate(dto.getIdContenedor())
                .orElseThrow(() -> new IllegalArgumentException("El contenedor no existe."));
        // Validar que el contenedor esté en estado disponible
        validarEstadoContenedor(contenedor, ESTADO_DISPONIBLE,
                "El contenedor no está disponible para asignación.");
        // Verificar si el contenedor ya tiene una asignación activa
        if (hojaContenedorRepository.findActiveByContenedorForUpdate(dto.getIdContenedor()).isPresent()) {
            throw new IllegalArgumentException("El contenedor ya tiene una asignación activa.");
        }
        // Crear la entidad HojaContenedorEntity a partir del DTO, hoja, contenedor y usuario
        HojaContenedorEntity asignacion = Mapper.toHojaContenedorEntity(
                dto, hoja, contenedor, usuario, LocalDateTime.now());
        // Cambiar el estado del contenedor a "en uso" y guardar la asignación
        contenedor.setEstadoContenedor(obtenerEstadoContenedor(ESTADO_EN_USO));
        // Guardar el contenedor actualizado y la asignación en la base de datos
        contenedorRepository.save(contenedor);
        // Guardar la asignación y devolver el DTO correspondiente
        return Mapper.toHojaContenedorDTO(hojaContenedorRepository.save(asignacion));
    }
    // Cerrar la carga de un contenedor asignado a una hoja de producción
    @Transactional
    public HojaContenedorDTO cerrarCarga(Integer idHojaContenedor, Integer idUsuario, Integer idBodega) {
        // Obtener el usuario autorizado para la bodega especificada
        UsuarioEntity usuario = obtenerUsuarioAutorizado(idUsuario, idBodega);
        // Obtener la asignación de contenedor bloqueada para actualización
        HojaContenedorEntity asignacion = obtenerBloqueada(idHojaContenedor, idBodega);
        // Validar que la asignación no haya sido liberada y que la carga no esté cerrada
        if (asignacion.getFechaLiberacion() != null) {
            throw new IllegalArgumentException("La asignación ya fue liberada.");
        }
        if (asignacion.getFechaCierreCarga() != null) {
            throw new IllegalArgumentException("La carga ya está cerrada.");
        }
        // Establecer la fecha de cierre de carga y el usuario que realizó el cierre
        asignacion.setFechaCierreCarga(LocalDateTime.now());
        asignacion.setUsuarioCierre(usuario);
        // Guardar la asignación actualizada y devolver el DTO correspondiente
        return Mapper.toHojaContenedorDTO(hojaContenedorRepository.save(asignacion));
    }
    // Liberar un contenedor asignado a una hoja de producción
    @Transactional
    public HojaContenedorDTO liberar(Integer idHojaContenedor, Integer idUsuario, Integer idBodega) {
        // Obtener el usuario autorizado para la bodega especificada
        UsuarioEntity usuario = obtenerUsuarioAutorizado(idUsuario, idBodega);
        // Obtener la asignación de contenedor bloqueada para actualización
        HojaContenedorEntity asignacion = obtenerBloqueada(idHojaContenedor, idBodega);
        // Validar que la asignación no haya sido liberada y que la carga esté cerrada
        if (asignacion.getFechaLiberacion() != null) {
            throw new IllegalArgumentException("La asignación ya fue liberada.");
        }
        if (asignacion.getFechaCierreCarga() == null) {
            throw new IllegalArgumentException("La carga debe cerrarse antes de liberar el contenedor.");
        }
        // Obtener el contenedor asociado a la asignación y validar que esté en uso
        ContenedorEntity contenedor = contenedorRepository.findByIdForUpdate(
                        asignacion.getContenedor().getIdContenedor())
                .orElseThrow(() -> new IllegalArgumentException("El contenedor no existe."));
        // Validar que el contenedor esté en estado "en uso" antes de liberarlo
        validarEstadoContenedor(contenedor, ESTADO_EN_USO,
                "El contenedor no está en uso.");
        // Establecer la fecha de liberación y el usuario que realizó la liberación
        asignacion.setFechaLiberacion(LocalDateTime.now());
        asignacion.setUsuarioLiberacion(usuario);
        // Cambiar el estado del contenedor a "disponible" y guardar la asignación
        contenedor.setEstadoContenedor(obtenerEstadoContenedor(ESTADO_DISPONIBLE));
        contenedorRepository.save(contenedor);
        // Guardar la asignación actualizada y devolver el DTO correspondiente
        return Mapper.toHojaContenedorDTO(hojaContenedorRepository.save(asignacion));
    }

    // Método para obtener una asignación de contenedor bloqueada para actualización
    private HojaContenedorEntity obtenerBloqueada(Integer idHojaContenedor, Integer idBodega) {
        // Validar que los IDs proporcionados sean válidos
        validarId(idHojaContenedor, "La asignación HojaContenedor es obligatoria.");
        validarId(idBodega, "La bodega es obligatoria.");
        // Buscar la asignación de contenedor por su ID y bloquearla para actualización
        HojaContenedorEntity asignacion = hojaContenedorRepository.findByIdForUpdate(idHojaContenedor)
                .orElseThrow(() -> new IllegalArgumentException("La asignación HojaContenedor no existe."));
        // Validar que la asignación pertenezca a la bodega especificada
        if (!perteneceBodega(asignacion.getHojaProduccion(), idBodega)) {
            throw new IllegalArgumentException("La asignación no pertenece a la bodega activa.");
        }
        return asignacion;
    }
    // Método para obtener un usuario autorizado para una bodega específica
    private UsuarioEntity obtenerUsuarioAutorizado(Integer idUsuario, Integer idBodega) {
        // Validar que los IDs proporcionados sean válidos
        validarId(idUsuario, "El usuario es obligatorio.");
        validarId(idBodega, "La bodega es obligatoria.");
        // Buscar el usuario por su ID y verificar que esté activo y autorizado para la bodega
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
        // Validar que el usuario esté activo
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new IllegalArgumentException("El usuario está inactivo.");
        }
        // Validar que el usuario esté autorizado para la bodega especificada
        if (!usuarioBodegaRepository.existsByUsuario_IdUsuarioAndBodega_IdBodega(idUsuario, idBodega)) {
            throw new IllegalArgumentException("El usuario no está autorizado para la bodega.");
        }
        return usuario;
    }

    // Método para validar que una hoja de producción pertenezca a una bodega específica
    private void validarHojaEnBodega(Integer idHoja, Integer idBodega) {
        // Obtenemos la hoja de producción por su ID y lanzamos una excepción si no existe
        HojaProduccionEntity hoja = hojaProduccionRepository.findById(idHoja)
                .orElseThrow(() -> new IllegalArgumentException("La hoja de producción no existe."));
        // Validamos que la hoja de producción pertenezca a la bodega especificada
        validarHojaEnBodega(hoja, idBodega);
    }
    // Método para validar que una hoja de producción pertenezca a una bodega específica
    private void validarHojaEnBodega(HojaProduccionEntity hoja, Integer idBodega) {
        // Si no pertenece a la bodega, lanzamos una excepción
        if (!perteneceBodega(hoja, idBodega)) {
            throw new IllegalArgumentException("La hoja no pertenece a la bodega activa.");
        }
    }
    // Método para verificar si una hoja de producción pertenece a una bodega específica
    private boolean perteneceBodega(HojaProduccionEntity hoja, Integer idBodega) {
        // Verificamos que la hoja y su bodega no sean nulas y que el ID de la bodega coincida
        return hoja != null && hoja.getBodega() != null
                && idBodega.equals(hoja.getBodega().getIdBodega());
    }
    // Método para validar el estado de un contenedor
    private void validarEstadoContenedor(ContenedorEntity contenedor, int estadoEsperado, String mensaje) {
        // Validamos que el estado del contenedor no sea nulo y que coincida con el estado esperado
        if (contenedor.getEstadoContenedor() == null
                || !Integer.valueOf(estadoEsperado).equals(
                contenedor.getEstadoContenedor().getIdEstadoContenedor())) {
            throw new IllegalArgumentException(mensaje);
        }
    }
    // Método para obtener el estado de un contenedor por su ID
    private EstadoContenedorEntity obtenerEstadoContenedor(int idEstado) {
        return estadoContenedorRepository.findById(idEstado)
                .orElseThrow(() -> new IllegalStateException("El estado del contenedor no existe."));
    }
    // Método para validar que un ID sea válido (no nulo y mayor que cero)
    private void validarId(Integer id, String mensaje) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}
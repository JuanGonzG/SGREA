package dgtic.core.service;

import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.BodegaDTO;
import dgtic.core.model.dto.BodegaAsociacionDTO;
import dgtic.core.model.dto.UsuarioDTO;
import dgtic.core.model.dto.UsuarioAltaDTO;
import dgtic.core.model.dto.UsuarioEdicionDTO;
import dgtic.core.model.entity.BodegaEntity;
import dgtic.core.model.entity.RolCatalogo;
import dgtic.core.model.entity.RolEntity;
import dgtic.core.model.entity.UsuarioBodega.UsuarioBodegaEntity;
import dgtic.core.model.entity.UsuarioBodega.UsuarioBodegaId;
import dgtic.core.model.entity.UsuarioEntity;
import dgtic.core.repository.BodegaRepository;
import dgtic.core.repository.RolRepository;
import dgtic.core.repository.SesionRepository;
import dgtic.core.repository.UsuarioBodegaRepository;
import dgtic.core.repository.UsuarioRepository;
import dgtic.core.repository.HojaContenedorRepository;
import dgtic.core.repository.MovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioBodegaRepository usuarioBodegaRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private HojaContenedorRepository hojaContenedorRepository;

    @Autowired
    private BodegaRepository bodegaRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private SesionRepository sesionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Obtener todos los usuarios con sus bodegas asociadas
    public List<UsuarioDTO> getUsuarios() {
        return usuarioRepository.findAll().stream().map(this::toUsuarioConBodegas).toList();
    }

    // Obtener un usuario por su ID con sus bodegas asociadas
    public Optional<UsuarioDTO> getUsuarioById(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario).map(this::toUsuarioConBodegas);
    }
    // Obtener todas las bodegas disponibles
    public List<BodegaDTO> getBodegas() {
        return bodegaRepository.findAll().stream().map(Mapper::toBodegaDTO).toList();
    }

    // Obtener todas las bodegas con el estado de asociación para un usuario específico
    public List<BodegaAsociacionDTO> getBodegasConEstado(Integer idUsuario) {
        // Validar que el usuario exista antes de obtener las bodegas
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new IllegalArgumentException("El usuario no existe.");
        }
        // Obtener las bodegas asociadas al usuario y almacenarlas en un conjunto para una búsqueda rápida
        Set<Integer> asociadas = usuarioBodegaRepository.findByUsuario_IdUsuario(idUsuario).stream()
                .map(ub -> ub.getBodega().getIdBodega())
                .collect(Collectors.toSet());
        // Obtener todas las bodegas y mapearlas a BodegaAsociacionDTO, indicando si están asociadas al usuario
        return bodegaRepository.findAll().stream()
                .map(bodega -> BodegaAsociacionDTO.builder()
                        .idBodega(bodega.getIdBodega())
                        .nombre(bodega.getNombre())
                        .descripcion(bodega.getDescripcion())
                        .asociado(asociadas.contains(bodega.getIdBodega()))
                        .build())
                .toList();
    }

    // Actualizar las bodegas asociadas a un usuario
    @Transactional
    public void actualizarBodegas(Integer idUsuario, List<Integer> idsBodegas) {
        // Validar que el usuario exista antes de actualizar las bodegas
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new IllegalArgumentException("El usuario no existe.");
        }
        // Validar que la lista de IDs de bodegas no sea nula ni vacía
        if (idsBodegas == null || idsBodegas.isEmpty()) {
            throw new IllegalStateException("El usuario debe conservar al menos una bodega.");
        }
        // Obtener las entidades de bodegas correspondientes a los IDs proporcionados
        List<BodegaEntity> bodegas = obtenerBodegas(idsBodegas);
        // Crear un conjunto de IDs de bodegas nuevas para facilitar la comparación
        Set<Integer> nuevas = bodegas.stream().map(BodegaEntity::getIdBodega).collect(Collectors.toSet());
        // Obtener las asociaciones actuales entre el usuario y las bodegas
        List<UsuarioBodegaEntity> actuales = usuarioBodegaRepository.findByUsuario_IdUsuario(idUsuario);
        // Eliminar las asociaciones que ya no están presentes en la lista de nuevas bodegas
        actuales.stream()
                .map(ub -> ub.getBodega().getIdBodega())
                .filter(idBodega -> !nuevas.contains(idBodega))
                .forEach(idBodega -> usuarioBodegaRepository
                        .deleteByUsuario_IdUsuarioAndBodega_IdBodega(idUsuario, idBodega));
        // Crear un conjunto de IDs de bodegas actuales para facilitar la comparación
        Set<Integer> actualesIds = actuales.stream()
                .map(ub -> ub.getBodega().getIdBodega())
                .collect(Collectors.toSet());
        // Guardar las nuevas asociaciones entre el usuario y las bodegas que no estaban presentes anteriormente
        bodegas.stream()
                .filter(bodega -> !actualesIds.contains(bodega.getIdBodega()))
                .forEach(bodega -> usuarioBodegaRepository.save(UsuarioBodegaEntity.builder()
                        .id(new UsuarioBodegaId(idUsuario, bodega.getIdBodega()))
                        .usuario(usuarioRepository.getReferenceById(idUsuario))
                        .bodega(bodega)
                        .build()));
    }

    // Crear un nuevo usuario con validaciones y asociaciones a bodegas
    @Transactional
    public UsuarioDTO crear(UsuarioAltaDTO dto) {
        // Validar el username del nuevo usuario
        validarUsernameNuevo(dto.getUsername());
        // Validar el rol y obtener la entidad correspondiente
        RolEntity rol = obtenerRolValido(dto.getRol().getIdRol());
        // Validar las bodegas y obtener las entidades correspondientes
        List<BodegaEntity> bodegas = obtenerBodegas(dto.getIdsBodegas());
        // Crear la entidad de usuario y guardarla en la base de datos
        UsuarioEntity usuario = UsuarioEntity.builder()
                .nombre(dto.getNombre().trim())
                .username(dto.getUsername().trim())
                .password(passwordEncoder.encode(dto.getPassword()))
                .activo(true)
                .rol(rol)
                .build();
        usuario = usuarioRepository.save(usuario);
        // Guardar las asociaciones entre el usuario y las bodegas
        guardarAsociaciones(usuario, bodegas);
        return toUsuarioConBodegas(usuario);
    }

    // Actualizar un usuario existente con validaciones y asociaciones a bodegas
    @Transactional
    public UsuarioDTO actualizar(UsuarioEdicionDTO dto) {
        // Validar que el usuario exista
        UsuarioEntity usuario = usuarioRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
        if (usuarioRepository.existsByUsernameIgnoreCaseAndIdUsuarioNot(dto.getUsername().trim(), dto.getIdUsuario())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese username.");
        }
        // Asignar los nuevos valores al usuario y guardar los cambios
        usuario.setNombre(dto.getNombre().trim());
        usuario.setUsername(dto.getUsername().trim());
        usuario.setActivo(dto.getActivo());
        usuario.setRol(obtenerRolValido(dto.getRol().getIdRol()));
        // Validar si la contraseña es diferente de null y no está en blanco antes de actualizarla
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return toUsuarioConBodegas(usuarioRepository.save(usuario));
    }

    // Eliminar un usuario si no tiene sesiones históricas asociadas
    @Transactional
    public void eliminar(Integer idUsuario) {
        // Validar que el usuario exista antes de eliminarlo
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
        // Verificar si el usuario tiene sesiones históricas antes de eliminarlo
        if (sesionRepository.existsByUsuario_IdUsuario(idUsuario)) {
            throw new IllegalStateException("No se puede eliminar el usuario porque tiene sesiones históricas.");
        }
        // Verificar si el usuario tiene movimientos o referencias operativas antes de eliminarlo
        if (movimientoRepository.existsByUsuario_IdUsuario(idUsuario)
                || hojaContenedorRepository.existsByUsuarioAsignacion_IdUsuario(idUsuario)
                || hojaContenedorRepository.existsByUsuarioCierre_IdUsuario(idUsuario)
                || hojaContenedorRepository.existsByUsuarioLiberacion_IdUsuario(idUsuario)) {
            throw new IllegalStateException(
                    "No se puede eliminar el usuario porque tiene historial o referencias operativas.");
        }
        // Eliminar las asociaciones entre el usuario y las bodegas, y luego eliminar el usuario
        usuarioBodegaRepository.deleteByUsuario_IdUsuario(idUsuario);
        usuarioRepository.delete(usuario);
    }

    // Obtener las bodegas asociadas a un usuario
    public List<BodegaDTO> getBodegasByUsuarioID(Integer usuarioId){
        return usuarioBodegaRepository.findByUsuario_IdUsuario(usuarioId)
                .stream()
                .map(ub -> Mapper.toBodegaDTO(ub.getBodega()))
                .toList();
    }

    // Obtener usuario por username
    public UsuarioDTO getUsuarioByUsername(String username){
        return Mapper.toUsuarioDTO(usuarioRepository.findByUsernameIgnoreCase(username));
    }

    // Validar el username de un nuevo usuario antes de crearlo
    private void validarUsernameNuevo(String username) {
        // Que no sea nulo, que no esté en blanco
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El username es obligatorio.");
        }
        // Que no supere los 255 caracteres
        if (username.trim().length() > 255) {
            throw new IllegalArgumentException("El username no puede superar 255 caracteres.");
        }
        // Que no exista otro usuario con el mismo username (ignorando mayúsculas y minúsculas)
        if (usuarioRepository.existsByUsernameIgnoreCase(username.trim())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese username.");
        }
    }

    // Obtener un rol válido por su ID
    private RolEntity obtenerRolValido(Integer idRol) {
        // Validar que el ID del rol no sea nulo y que exista en el catálogo de roles
        if (idRol == null || Arrays.stream(RolCatalogo.values())
                                .noneMatch(rol -> rol.getId().equals(idRol))) {
            throw new IllegalArgumentException("El rol seleccionado no es válido.");
        }
        // Obtener la entidad del rol desde el repositorio, lanzando una excepción si no existe
        return rolRepository.findById(idRol)
                .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no existe."));
    }

    // Obtener las bodegas válidas por sus IDs, asegurando que no haya duplicados y que todas existan
    private List<BodegaEntity> obtenerBodegas(List<Integer> idsBodegas) {
        // Validar que la lista de IDs de bodegas no sea nula, no esté vacía y que no contenga elementos nulos
        if (idsBodegas == null || idsBodegas.isEmpty() || idsBodegas.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("El usuario debe tener al menos una bodega.");
        }
        // Validar que no haya IDs de bodegas duplicados
        Set<Integer> idsUnicos = Set.copyOf(idsBodegas);
        if (idsUnicos.size() != idsBodegas.size()) {
            throw new IllegalArgumentException("No se pueden repetir bodegas.");
        }
        // Obtener las entidades de bodegas desde el repositorio y validar que todas existan
        List<BodegaEntity> bodegas = bodegaRepository.findAllById(idsUnicos);
        // Validar que todas las bodegas solicitadas existan en la base de datos
        if (bodegas.size() != idsUnicos.size()) {
            throw new IllegalArgumentException("Una o más bodegas no existen.");
        }
        return bodegas;
    }

    // Guardar las asociaciones entre un usuario y sus bodegas
    private void guardarAsociaciones(UsuarioEntity usuario, List<BodegaEntity> bodegas) {
        // Guardar cada asociación entre el usuario y las bodegas en la tabla de relación
        bodegas.forEach(bodega -> usuarioBodegaRepository.save(UsuarioBodegaEntity.builder()
                .id(new UsuarioBodegaId(usuario.getIdUsuario(), bodega.getIdBodega()))
                .usuario(usuario)
                .bodega(bodega)
                .build()));
    }

    // Convertir un UsuarioEntity a UsuarioDTO incluyendo sus bodegas asociadas
    private UsuarioDTO toUsuarioConBodegas(UsuarioEntity usuario) {
        UsuarioDTO dto = Mapper.toUsuarioDTO(usuario);
        dto.setBodegas(getBodegasByUsuarioID(usuario.getIdUsuario()));
        return dto;
    }
}

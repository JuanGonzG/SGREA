package dgtic.core.service;

import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.ContenedorAltaDTO;
import dgtic.core.model.dto.ContenedorDTO;
import dgtic.core.model.dto.ContenedorEdicionDTO;
import dgtic.core.model.dto.EstadoContenedorDTO;
import dgtic.core.model.entity.ContenedorEntity;
import dgtic.core.model.entity.EstadoContenedorEntity;
import dgtic.core.repository.ContenedorRepository;
import dgtic.core.repository.EstadoContenedorRepository;
import dgtic.core.repository.HojaContenedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ContenedorService {
    // Definición de constantes para los estados de contenedor y formato de código
    private static final int ESTADO_DISPONIBLE = 1;
    private static final int ESTADO_EN_USO = 2;
    private static final int ESTADO_MANTENIMIENTO = 3;
    private static final int ESTADO_FUERA_DE_SERVICIO = 4;
    private static final String PREFIJO_CODIGO = "CONT-";
    private static final int LONGITUD_CONSECUTIVO = 6;
    private static final int MAXIMO_CONSECUTIVO = 999999;

    @Autowired
    private ContenedorRepository contenedorRepository;

    @Autowired
    private EstadoContenedorRepository estadoContenedorRepository;

    @Autowired
    private HojaContenedorRepository hojaContenedorRepository;

    // Obtener todos los contenedores ordenados por código ascendente
    @Transactional(readOnly = true)
    public List<ContenedorDTO> getContenedores() {
        return contenedorRepository.findAllByOrderByCodigoAsc()
                .stream()
                .map(Mapper::toContenedorDTO)
                .toList();
    }
    // Obtener un contenedor por su ID
    @Transactional(readOnly = true)
    public Optional<ContenedorDTO> getById(Integer idContenedor) {
        validarIdContenedor(idContenedor);
        return contenedorRepository.findById(idContenedor)
                .map(Mapper::toContenedorDTO);
    }
    // Obtener todos los estados de contenedor
    @Transactional(readOnly = true)
    public List<EstadoContenedorDTO> getEstados() {
        return estadoContenedorRepository.findAll()
                .stream()
                .map(Mapper::toEstadoContenedorDTO)
                .toList();
    }
    // Agregar un nuevo contenedor
    @Transactional
    public ContenedorDTO addContenedor(ContenedorAltaDTO contenedorAltaDTO) {
        // Validar que los datos del contenedor no sean nulos
        if (contenedorAltaDTO == null) {
            throw new IllegalArgumentException("Los datos del contenedor son obligatorios.");
        }
        // Validar que la capacidad del contenedor sea mayor que cero
        validarCapacidad(contenedorAltaDTO.getCapacidad());
        // Normalizar las observaciones del contenedor
        String observaciones = normalizarObservaciones(contenedorAltaDTO.getObservaciones());
        // Obtener el estado inicial del contenedor (Disponible)
        EstadoContenedorEntity estadoDisponible = obtenerEstadoInicial();
        // Generar un código único para el contenedor
        String codigo = generarCodigo();
        //
        ContenedorAltaDTO datos = ContenedorAltaDTO.builder()
                .capacidad(contenedorAltaDTO.getCapacidad())
                .observaciones(observaciones)
                .build();
        // Mapear los datos del contenedor a una entidad y guardarla en la base de datos
        ContenedorEntity contenedor = Mapper.toContenedorEntity(
                datos,
                codigo,
                LocalDateTime.now(),
                estadoDisponible);
        return Mapper.toContenedorDTO(contenedorRepository.save(contenedor));
    }
    // Actualizar un contenedor existente
    @Transactional
    public ContenedorDTO updateContenedor(Integer idContenedor, ContenedorEdicionDTO contenedorEdicionDTO) {
        // Validar que el ID del contenedor sea válido
        validarIdContenedor(idContenedor);
        // Validar que los datos del contenedor no sean nulos
        if (contenedorEdicionDTO == null) {
            throw new IllegalArgumentException("Los datos del contenedor son obligatorios.");
        }
        // Validar que la capacidad del contenedor sea mayor que cero
        validarCapacidad(contenedorEdicionDTO.getCapacidad());
        //
        ContenedorEntity contenedor = contenedorRepository.findById(idContenedor)
                .orElseThrow(() -> new IllegalArgumentException("El contenedor no existe."));
        // Validar el estado actual del contenedor y obtener su ID
        EstadoContenedorEntity estadoActual = validarEstadoActual(contenedor);
        // Verificar si el contenedor está en uso y lanzar una excepción si es así
        int idEstadoActual = estadoActual.getIdEstadoContenedor();
        if (idEstadoActual == ESTADO_EN_USO) {
            throw new IllegalArgumentException("El contenedor en uso no puede modificarse desde el CRUD.");
        }
        // Determinar el nuevo estado del contenedor, si no se proporciona, se mantiene el estado actual
        int idEstadoNuevo = contenedorEdicionDTO.getIdEstadoContenedor() == null
                ? idEstadoActual
                : contenedorEdicionDTO.getIdEstadoContenedor();
        // Validar la transición de estado del contenedor
        validarTransicion(idEstadoActual, idEstadoNuevo);
        // Obtener la entidad del nuevo estado del contenedor
        EstadoContenedorEntity estadoNuevo = obtenerEstadoParaEdicion(idEstadoNuevo);
        // Normalizar las observaciones del contenedor
        String observaciones = normalizarObservaciones(contenedorEdicionDTO.getObservaciones());
        // Crear un DTO con los datos actualizados del contenedor
        ContenedorEdicionDTO datos = ContenedorEdicionDTO.builder()
                .capacidad(contenedorEdicionDTO.getCapacidad())
                .idEstadoContenedor(idEstadoNuevo)
                .observaciones(observaciones)
                .build();

        // Actualizar el contenedor con los datos proporcionados
        Mapper.actualizarContenedor(contenedor, datos, estadoNuevo);
        return Mapper.toContenedorDTO(contenedorRepository.save(contenedor));
    }
    // Eliminar un contenedor existente
    @Transactional
    public void deleteContenedor(Integer idContenedor) {
        // Validar que el ID del contenedor sea válido
        validarIdContenedor(idContenedor);
        // Obtener el contenedor por su ID, lanzar una excepción si no existe
        ContenedorEntity contenedor = contenedorRepository.findById(idContenedor)
                .orElseThrow(() -> new IllegalArgumentException("El contenedor no existe."));
        // Verificar si el contenedor tiene historial o asignaciones, lanzar una excepción si es así
        if (hojaContenedorRepository.existsByContenedor_IdContenedor(idContenedor)) {
            throw new IllegalStateException(
                    "No se puede eliminar el contenedor porque tiene historial o asignaciones.");
        }
        contenedorRepository.delete(contenedor);
    }
    // Método para obtener el estado inicial del contenedor (Disponible) desde el repositorio
    private EstadoContenedorEntity obtenerEstadoInicial() {
        return estadoContenedorRepository.findById(ESTADO_DISPONIBLE)
                .orElseThrow(() -> new IllegalStateException(
                        "El estado inicial Disponible no existe en el catálogo."));
    }
    // Método para obtener el estado del contenedor para edición desde el repositorio
    private EstadoContenedorEntity obtenerEstadoParaEdicion(int idEstado) {
        return estadoContenedorRepository.findById(idEstado)
                .orElseThrow(() -> new IllegalArgumentException("El estado del contenedor no existe."));
    }
    // Método para validar el estado actual del contenedor y lanzar una excepción si no es válido
    private EstadoContenedorEntity validarEstadoActual(ContenedorEntity contenedor) {
        if (contenedor.getEstadoContenedor() == null
                || contenedor.getEstadoContenedor().getIdEstadoContenedor() == null) {
            throw new IllegalStateException("El contenedor no tiene un estado válido.");
        }
        return contenedor.getEstadoContenedor();
    }
    // Método para validar la transición de estado del contenedor y lanzar una excepción si no es válida
    private void validarTransicion(int estadoActual, int estadoNuevo) {
        // Si el estado actual es igual al nuevo estado, no se realiza ninguna acción
        if (estadoActual == estadoNuevo) {
            return;
        }
        // Si el estado actual es En Uso o Fuera de Servicio, no se permite la transición desde el CRUD
        if (estadoActual == ESTADO_EN_USO || estadoActual == ESTADO_FUERA_DE_SERVICIO) {
            throw new IllegalArgumentException("El estado actual no permite esa transición desde el CRUD.");
        }
        // Verificar si la transición de estado está permitida
        boolean permitida = (estadoActual == ESTADO_DISPONIBLE
                && (estadoNuevo == ESTADO_MANTENIMIENTO || estadoNuevo == ESTADO_FUERA_DE_SERVICIO))
                || (estadoActual == ESTADO_MANTENIMIENTO
                && (estadoNuevo == ESTADO_DISPONIBLE || estadoNuevo == ESTADO_FUERA_DE_SERVICIO));

        if (!permitida) {
            throw new IllegalArgumentException("La transición de estado no está permitida desde el CRUD.");
        }
    }

    // Método para generar un código único para un contenedor
    private String generarCodigo() {
        // Obtener el último código de contenedor registrado y extraer su consecutivo
        int siguiente = contenedorRepository.findTopByOrderByCodigoDesc()
                .map(ContenedorEntity::getCodigo)
                .map(this::extraerConsecutivo)
                .orElse(0) + 1;
        // Generar un nuevo código de contenedor asegurando que sea único
        String codigo;
        while (true) {
            if (siguiente > MAXIMO_CONSECUTIVO) {
                throw new IllegalStateException("Se agotó el consecutivo disponible para códigos de contenedor.");
            }
            codigo = PREFIJO_CODIGO + String.format("%0" + LONGITUD_CONSECUTIVO + "d", siguiente++);
            if (!contenedorRepository.existsByCodigo(codigo)) {
                return codigo;
            }
        }
    }

    // Método para extraer el consecutivo de un código de contenedor
    private int extraerConsecutivo(String codigo) {
        // Validar que el código de contenedor tenga el formato correcto
        if (codigo == null || !codigo.matches("^CONT-[0-9]{6}$")) {
            throw new IllegalStateException("Existe un código de contenedor con formato inválido.");
        }
        // Extraer el consecutivo del código y convertirlo a un número entero
        try {
            return Integer.parseInt(codigo.substring(PREFIJO_CODIGO.length()));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("El código de contenedor excede el rango permitido.", exception);
        }
    }
    // Método para validar que la capacidad del contenedor sea mayor que cero
    private void validarCapacidad(Integer capacidad) {
        if (capacidad == null || capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor que cero.");
        }
    }
    // Método para normalizar las observaciones del contenedor, eliminando espacios y validando longitud
    private String normalizarObservaciones(String observaciones) {
        if (observaciones == null) {
            return null;
        }

        String valor = observaciones.trim();
        if (valor.length() > 255) {
            throw new IllegalArgumentException("Las observaciones no pueden superar 255 caracteres.");
        }
        return valor.isEmpty() ? null : valor;
    }
    // Método para validar que el ID del contenedor sea válido (no nulo y mayor que cero)
    private void validarIdContenedor(Integer idContenedor) {
        if (idContenedor == null || idContenedor <= 0) {
            throw new IllegalArgumentException("El identificador del contenedor es obligatorio.");
        }
    }
}

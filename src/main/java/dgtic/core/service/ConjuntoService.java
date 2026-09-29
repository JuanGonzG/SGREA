package dgtic.core.service;

import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.ConjuntoAltaDTO;
import dgtic.core.model.dto.ConjuntoDTO;
import dgtic.core.model.dto.ConjuntoEdicionDTO;
import dgtic.core.model.dto.EstadoConjuntoDTO;
import dgtic.core.model.entity.ConjuntoEntity;
import dgtic.core.model.entity.EstadoConjuntoEntity;
import dgtic.core.model.entity.ProductoEntity;
import dgtic.core.repository.ConjuntoRepository;
import dgtic.core.repository.EstadoConjuntoRepository;
import dgtic.core.repository.MovimientoRepository;
import dgtic.core.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ConjuntoService {
    // Definición de constantes para los estados de conjunto
    private static final int ESTADO_DISPONIBLE = 1;
    private static final int ESTADO_SURTIDO = 2;
    private static final int ESTADO_MANTENIMIENTO = 3;
    private static final int ESTADO_FUERA_DE_SERVICIO = 4;
    // Prefijo constante para los identificadores de conjunto
    private static final String PREFIJO_ID = "CON-";

    @Autowired
    private ConjuntoRepository conjuntoRepository;

    @Autowired
    private EstadoConjuntoRepository estadoConjuntoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    // Obtener todos los conjuntos de una bodega específica
    @Transactional(readOnly = true)
    public List<ConjuntoDTO> getConjuntosByBodegaId(Integer idBodega) {
        // Validar que la bodega activa sea proporcionada
        validarIdBodega(idBodega);
        return conjuntoRepository.findByProducto_Bodega_IdBodega(idBodega)
                .stream()
                .map(Mapper::toConjuntoDTO)
                .toList();
    }

    // Obtener un conjunto por su ID
    @Transactional(readOnly = true)
    public Optional<ConjuntoDTO> getById(String idConjunto, Integer idBodega) {
        // Validar que la bodega activa y el ID del conjunto sean proporcionados
        validarIdConjunto(idConjunto);
        validarIdBodega(idBodega);
        // Buscar el conjunto por su ID y filtrar por la bodega activa
        return conjuntoRepository.findById(idConjunto)
                .filter(conjunto -> conjunto.getProducto() != null
                        && conjunto.getProducto().getBodega() != null
                        && idBodega.equals(conjunto.getProducto().getBodega().getIdBodega()))
                .map(Mapper::toConjuntoDTO);
    }

    // Obtener todos los estados de conjunto
    @Transactional(readOnly = true)
    public List<EstadoConjuntoDTO> getEstados() {
        return estadoConjuntoRepository.findAll()
                .stream()
                .map(Mapper::toEstadoConjuntoDTO)
                .toList();
    }

    // Agregar un nuevo conjunto
    @Transactional
    public ConjuntoDTO addConjunto(ConjuntoAltaDTO conjuntoAltaDTO, Integer idBodega) {
        // Validar los datos de entrada
        if (conjuntoAltaDTO == null || conjuntoAltaDTO.getIdProducto() == null) {
            throw new IllegalArgumentException("El producto es obligatorio.");
        }
        // Validar que la bodega activa sea proporcionada
        validarIdBodega(idBodega);
        // Validar que el producto exista
        ProductoEntity producto = productoRepository.findById(conjuntoAltaDTO.getIdProducto())
                .orElseThrow(() -> new IllegalArgumentException("El producto no existe."));
        // Validar que el producto pertenezca a la bodega activa
        validarProductoAutorizado(producto, idBodega);
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new IllegalArgumentException("Solo se pueden crear conjuntos para productos activos.");
        }
        // Obtener el estado "Disponible" para el nuevo conjunto
        EstadoConjuntoEntity estadoDisponible = obtenerEstado(ESTADO_DISPONIBLE);
        // Generar un nuevo ID de conjunto
        String idConjunto = generarIdConjunto();
        // Normalizar las observaciones
        String observaciones = normalizarObservaciones(conjuntoAltaDTO.getObservaciones());
        // Crear un DTO con los datos necesarios para la creación del conjunto
        ConjuntoAltaDTO datos = ConjuntoAltaDTO.builder()
                .idProducto(conjuntoAltaDTO.getIdProducto())
                .observaciones(observaciones)
                .build();
        // Mapear los datos a una entidad de conjunto y guardarla en la base de datos
        ConjuntoEntity conjunto = Mapper.toConjuntoEntity(
                datos,
                idConjunto,
                LocalDateTime.now(),
                producto,
                estadoDisponible);

        return Mapper.toConjuntoDTO(conjuntoRepository.save(conjunto));
    }

    // Actualizar un conjunto existente
    @Transactional
    public ConjuntoDTO updateConjunto(ConjuntoEdicionDTO conjuntoEdicionDTO, Integer idBodega) {
        // Validar los datos de entrada
        if (conjuntoEdicionDTO == null) {
            throw new IllegalArgumentException("Los datos del conjunto son obligatorios.");
        }
        // Validar que la bodega activa sea proporcionada
        validarIdBodega(idBodega);
        // Validar que el ID del conjunto sea proporcionado
        validarIdConjunto(conjuntoEdicionDTO.getIdConjunto());
        // Obtener el conjunto existente de la base de datos
        ConjuntoEntity conjunto = conjuntoRepository.findById(conjuntoEdicionDTO.getIdConjunto())
                .orElseThrow(() -> new IllegalArgumentException("El conjunto no existe."));

        // Validar que el producto pertenezca a la bodega activa
        validarProductoAutorizado(conjunto.getProducto(), idBodega);
        // Normalizar las observaciones
        String observaciones = normalizarObservaciones(conjuntoEdicionDTO.getObservaciones());
        // Obtener el estado actual del conjunto y determinar el nuevo estado
        EstadoConjuntoEntity estadoActual = conjunto.getEstadoConjunto();
        int idEstadoActual = estadoActual.getIdEstadoConjunto();
        int idEstadoNuevo = conjuntoEdicionDTO.getIdEstadoConjunto() == null
                ? idEstadoActual
                : conjuntoEdicionDTO.getIdEstadoConjunto();
        // Validar la transición de estado
        validarTransicion(idEstadoActual, idEstadoNuevo);
        EstadoConjuntoEntity estadoNuevo = obtenerEstado(idEstadoNuevo);
        // Crear un DTO con los datos necesarios para la actualización del conjunto
        ConjuntoEdicionDTO datos = ConjuntoEdicionDTO.builder()
                .idConjunto(conjunto.getIdConjunto())
                .idEstadoConjunto(idEstadoNuevo)
                .observaciones(observaciones)
                .build();
        // Actualizar el conjunto con los datos proporcionados
        Mapper.actualizarConjunto(conjunto, datos, estadoNuevo);

        return Mapper.toConjuntoDTO(conjuntoRepository.save(conjunto));
    }
    // Eliminar un conjunto existente
    @Transactional
    public void deleteConjunto(String idConjunto, Integer idBodega) {
        // Validar que la bodega activa y el ID del conjunto sean proporcionados
        validarIdBodega(idBodega);
        validarIdConjunto(idConjunto);
        // Obtener el conjunto existente de la base de datos
        ConjuntoEntity conjunto = conjuntoRepository.findById(idConjunto)
                .orElseThrow(() -> new IllegalArgumentException("El conjunto no existe."));
        // Validar que el producto pertenezca a la bodega activa
        validarProductoAutorizado(conjunto.getProducto(), idBodega);
        // Validar que el conjunto no tenga historial de movimientos antes de eliminarlo
        if (movimientoRepository.existsByConjunto_IdConjunto(idConjunto)) {
            throw new IllegalStateException(
                    "No se puede eliminar el conjunto porque tiene historial de movimientos.");
        }
        conjuntoRepository.delete(conjunto);
    }
    // Obtener un estado de conjunto por su ID
    private EstadoConjuntoEntity obtenerEstado(int idEstado) {
        return estadoConjuntoRepository.findById(idEstado)
                .orElseThrow(() -> new IllegalStateException("El estado de conjunto no existe."));
    }
    // Validar la transición de estado de un conjunto
    private void validarTransicion(int estadoActual, int estadoNuevo) {
        // Validar que la transición de estado sea diferente
        if (estadoActual == estadoNuevo) {
            return;
        }
        // Validar que el estado actual no sea "Surtido" o "Fuera de Servicio"
        if (estadoActual == ESTADO_SURTIDO || estadoActual == ESTADO_FUERA_DE_SERVICIO) {
            throw new IllegalArgumentException("El estado actual del conjunto no permite esa transición.");
        }
        // Validar que la transición de estado sea permitida según las reglas definidas
        boolean permitida = (estadoActual == ESTADO_DISPONIBLE
                && (estadoNuevo == ESTADO_MANTENIMIENTO || estadoNuevo == ESTADO_FUERA_DE_SERVICIO))
                || (estadoActual == ESTADO_MANTENIMIENTO
                && (estadoNuevo == ESTADO_DISPONIBLE || estadoNuevo == ESTADO_FUERA_DE_SERVICIO));
        if (!permitida) {
            throw new IllegalArgumentException("La transición de estado no está permitida desde el CRUD.");
        }
    }

    // Validar que el producto pertenezca a la bodega activa
    private void validarProductoAutorizado(ProductoEntity producto, Integer idBodega) {
        if (producto == null || producto.getBodega() == null
                || !idBodega.equals(producto.getBodega().getIdBodega())) {
            throw new IllegalArgumentException("El producto no pertenece a la bodega activa.");
        }
    }
    // Normalizar las observaciones del conjunto
    private String normalizarObservaciones(String observaciones) {
        // Observaciones no pueden ser nulas
        if (observaciones == null) {
            return null;
        }
        // Eliminar espacios en blanco al inicio y al final
        String valor = observaciones.trim();
        // Validar que las observaciones no superen los 255 caracteres
        if (valor.length() > 255) {
            throw new IllegalArgumentException("Las observaciones no pueden superar 255 caracteres.");
        }
        // Retornar null si las observaciones están vacías después de la normalización
        return valor.isEmpty() ? null : valor;
    }
    // Validar que la bodega activa sea proporcionada
    private void validarIdBodega(Integer idBodega) {
        if (idBodega == null) {
            throw new IllegalArgumentException("La bodega activa es obligatoria.");
        }
    }
    // Validar que el ID del conjunto sea proporcionado
    private void validarIdConjunto(String idConjunto) {
        if (idConjunto == null || idConjunto.isBlank()) {
            throw new IllegalArgumentException("El identificador del conjunto es obligatorio.");
        }
    }
    // Generar un nuevo ID de conjunto de manera sincronizada para evitar colisiones
    private synchronized String generarIdConjunto() {
        // Obtener el último ID de conjunto y extraer el número para generar el siguiente
        int siguiente = conjuntoRepository.findTopByOrderByIdConjuntoDesc()
                .map(ConjuntoEntity::getIdConjunto)
                .map(this::extraerNumero)
                .orElse(0) + 1;

        // Generar el ID de conjunto
        String idConjunto;
        do {
            idConjunto = PREFIJO_ID + String.format("%06d", siguiente++);
        } while (conjuntoRepository.existsById(idConjunto));
        return idConjunto;
    }

    // Extraer el número del ID de conjunto, asegurando que tenga el formato correcto
    private int extraerNumero(String idConjunto) {
        // Validar que el ID de conjunto tenga el formato correcto
        if (idConjunto == null || !idConjunto.matches("^CON-[0-9]+$")) {
            throw new IllegalStateException("Existe un identificador de conjunto con formato inválido.");
        }
        // Extraer el número del ID de conjunto y manejar posibles excepciones
        try {
            return Integer.parseInt(idConjunto.substring(PREFIJO_ID.length()));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("El identificador de conjunto excede el rango permitido.", exception);
        }
    }
}

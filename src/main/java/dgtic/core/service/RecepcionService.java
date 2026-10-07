package dgtic.core.service;

import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.HojaContenedorDTO;
import dgtic.core.model.dto.MovimientoDTO;
import dgtic.core.model.dto.MovimientoEntradaDTO;
import dgtic.core.model.dto.RecepcionContenedorDTO;
import dgtic.core.model.dto.RecepcionDetalleDTO;
import dgtic.core.model.dto.RecepcionEntradaResponse;
import dgtic.core.model.dto.RecepcionEstadoDTO;
import dgtic.core.model.dto.RecepcionLiberarContenedorResponse;
import dgtic.core.model.dto.RecepcionMovimientoDTO;
import dgtic.core.model.dto.RecepcionResumenDTO;
import dgtic.core.model.dto.RecepcionSeleccionarContenedorResponse;
import dgtic.core.model.entity.ConjuntoEntity;
import dgtic.core.model.entity.DetalleHojaEntity;
import dgtic.core.model.entity.EstadoHojaEntity;
import dgtic.core.model.entity.HojaContenedorEntity;
import dgtic.core.model.entity.HojaProduccionEntity;
import dgtic.core.model.entity.MovimientoEntity;
import dgtic.core.repository.ConjuntoRepository;
import dgtic.core.repository.ContenedorRepository;
import dgtic.core.repository.DetalleHojaRepository;
import dgtic.core.repository.EstadoHojaRepository;
import dgtic.core.repository.HojaContenedorRepository;
import dgtic.core.repository.HojaProduccionRepository;
import dgtic.core.repository.MovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class RecepcionService {
    // Estados de hoja relevantes para la recepción
    private static final int ESTADO_EN_LLAMADO = 4;
    private static final int ESTADO_RECIBIENDO = 5;
    private static final int ESTADO_COMPLETADO = 6;

    @Autowired
    private HojaProduccionRepository hojaProduccionRepository;

    @Autowired
    private DetalleHojaRepository detalleHojaRepository;

    @Autowired
    private HojaContenedorRepository hojaContenedorRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private ConjuntoRepository conjuntoRepository;

    @Autowired
    private ContenedorRepository contenedorRepository;

    @Autowired
    private EstadoHojaRepository estadoHojaRepository;

    @Autowired
    private MovimientoService movimientoService;

    @Autowired
    private HojaContenedorService hojaContenedorService;

    // Obtiene el estado de la hoja de producción para la recepción
    @Transactional(readOnly = true)
    public RecepcionEstadoDTO obtenerEstado(Integer idHoja, Integer idBodega) {
        // Validar y obtener la hoja de producción
        HojaProduccionEntity hoja = obtenerHojaParaConsulta(idHoja, idBodega);
        // Obtener detalles y contenedores asociados a la hoja
        List<DetalleHojaEntity> detalles = detalleHojaRepository
                .findByHojaProduccionEntity_IdHoja(idHoja);
        List<HojaContenedorEntity> contenedores = hojaContenedorRepository
                .findByHojaProduccion_IdHoja(idHoja);
        // Calcular totales de surtido y devuelto
        int totalSurtido = detalles.stream()
                .mapToInt(detalle -> valor(detalle.getCantidadSurtida()))
                .sum();
        int totalDevuelto = detalles.stream()
                .mapToInt(detalle -> valor(detalle.getCantidadDevuelta()))
                .sum();
        // Verificar si hay contenedores pendientes de liberación
        boolean sinContenedoresPendientes = !hojaContenedorRepository
                .existsByHojaProduccion_IdHojaAndFechaLiberacionIsNull(idHoja);
        // Construir resumen de la recepción
        RecepcionResumenDTO resumen = construirResumen(
                totalSurtido, totalDevuelto, sinContenedoresPendientes);
        // Obtener el contenedor actual (el más reciente sin fecha de liberación)
        RecepcionContenedorDTO contenedorActual = contenedores.stream()
                .filter(contenedor -> contenedor.getFechaLiberacion() == null)
                .max(Comparator.comparing(HojaContenedorEntity::getFechaAsignacion))
                .map(this::mapearContenedor)
                .orElse(null);
        // Obtener los contenedores pendientes (sin fecha de liberación)
        List<RecepcionContenedorDTO> pendientes = contenedores.stream()
                .filter(contenedor -> contenedor.getFechaLiberacion() == null)
                .map(this::mapearContenedor)
                .toList();
        // Obtener los últimos movimientos de la hoja de producción
        List<RecepcionMovimientoDTO> movimientos = movimientoRepository
                .findTop5ByHojaProduccion_IdHojaOrderByFechaDescIdMovimientoDesc(idHoja)
                .stream()
                .map(this::mapearMovimiento)
                .toList();
        // Construir y retornar el DTO de estado de recepción
        return RecepcionEstadoDTO.builder()
                .idHoja(hoja.getIdHoja())
                .nombreProyecto(hoja.getNombreProyecto())
                .cliente(hoja.getCliente())
                .fechaEstimadaRegreso(hoja.getFechaEstimadaRegreso())
                .totalSurtido(resumen.getTotalSurtido())
                .totalDevuelto(resumen.getTotalDevuelto())
                .totalPendiente(resumen.getTotalPendiente())
                .porcentaje(resumen.getPorcentaje())
                .completo(resumen.getCompleto())
                .contenedorActual(contenedorActual)
                .detalles(detalles.stream().map(this::mapearDetalle).toList())
                .contenedoresPendientes(pendientes)
                .ultimosMovimientos(movimientos)
                .build();
    }
    // Selecciona un contenedor para la recepción de la hoja de producción
    @Transactional
    public RecepcionSeleccionarContenedorResponse seleccionarContenedor(Integer idHoja, String codigo, Integer idUsuario, Integer idBodega) {
        // Obtenemos la hoja de producción para la recepción
        obtenerHojaParaRecepcion(idHoja, idBodega);
        // Normalizamos el código del contenedor y validamos su existencia
        String codigoNormalizado = normalizarCodigo(
                codigo, "El código del contenedor es obligatorio.");
        Integer idContenedor = contenedorRepository.findByCodigoIgnoreCase(codigoNormalizado)
                .map(contenedor -> contenedor.getIdContenedor())
                .orElseThrow(() -> new IllegalArgumentException("El contenedor no existe."));
        // Obtenemos o asignamos el contenedor para la recepción de la hoja de producción
        HojaContenedorDTO asignacion = hojaContenedorService.obtenerOAsignarParaRecepcion(
                idHoja, idContenedor, idUsuario, idBodega);
        // Retornamos la respuesta indicando que el contenedor fue seleccionado correctamente
        return RecepcionSeleccionarContenedorResponse.builder()
                .mensaje("Contenedor de retorno seleccionado correctamente.")
                .contenedor(mapearContenedor(asignacion))
                .build();
    }
    // Registra la entrada de un conjunto en la hoja de producción
    @Transactional
    public RecepcionEntradaResponse registrarEntrada(
            Integer idHoja, String codigoConjunto, Integer idHojaContenedor,
            Integer idUsuario, Integer idBodega) {
        // Obtenemos la hoja de producción para la recepción
        HojaProduccionEntity hoja = obtenerHojaParaRecepcion(idHoja, idBodega);
        // Normalizamos el código del conjunto y validamos su existencia
        String codigoNormalizado = normalizarCodigo(
                codigoConjunto, "El código del conjunto es obligatorio.");
        ConjuntoEntity conjunto = conjuntoRepository.findById(codigoNormalizado)
                .orElseThrow(() -> new IllegalArgumentException("El conjunto no existe."));
        // Validamos que el conjunto tenga un producto válido
        if (conjunto.getProducto() == null || conjunto.getProducto().getIdProducto() == null) {
            throw new IllegalArgumentException("El conjunto no tiene un producto válido.");
        }
        // Validamos que el contenedor (si se proporciona) pertenezca a la hoja de producción y no haya sido liberado
        if (idHojaContenedor != null) {
            HojaContenedorEntity asignacion = hojaContenedorRepository
                    .findByIdHojaContenedorAndHojaProduccion_IdHoja(idHojaContenedor, idHoja)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La HojaContenedor no pertenece a la hoja indicada."));
            // Validamos que la HojaContenedor no haya sido liberada
            if (asignacion.getFechaLiberacion() != null) {
                throw new IllegalArgumentException(
                        "La HojaContenedor ya fue liberada y no puede recibir entradas.");
            }
        }
        // Registramos la entrada del conjunto en la hoja de producción
        MovimientoEntradaDTO entrada = MovimientoEntradaDTO.builder()
                .idHoja(idHoja)
                .idConjunto(codigoNormalizado)
                .idHojaContenedor(idHojaContenedor)
                .build();
        // Registramos la entrada utilizando el servicio de movimientos
        MovimientoDTO movimiento = movimientoService.registrarEntrada(
                entrada, idUsuario, idBodega);
        // Si la hoja estaba en estado "En llamado", la cambiamos a "Recibiendo"
        if (Integer.valueOf(ESTADO_EN_LLAMADO).equals(hoja.getEstadoHoja().getIdEstadoHoja())) {
            cambiarEstadoHoja(hoja, ESTADO_RECIBIENDO);
        }
        // Evaluamos si la recepción de la hoja de producción está completa
        boolean recepcionCompleta = evaluarFinalizacion(hoja.getIdHoja());
        DetalleHojaEntity detalle = detalleHojaRepository
                .findByHojaProduccionEntity_IdHojaAndProducto_IdProducto(
                        idHoja, conjunto.getProducto().getIdProducto())
                .orElseThrow(() -> new IllegalStateException(
                        "No fue posible recuperar el detalle actualizado."));

        // Retornamos la respuesta indicando que la entrada fue registrada correctamente
        return RecepcionEntradaResponse.builder()
                .mensaje("Entrada registrada correctamente.")
                .movimiento(mapearMovimiento(movimiento))
                .detalle(mapearDetalle(detalle))
                .contenedor(idHojaContenedor == null ? null : obtenerContenedor(idHojaContenedor))
                .hoja(construirResumenDeHoja(idHoja))
                .estadoHoja(hoja.getEstadoHoja().getNombre())
                .recepcionCompleta(recepcionCompleta)
                .build();
    }
    // Libera un contenedor de la hoja de producción
    @Transactional
    public RecepcionLiberarContenedorResponse liberarContenedor(
            Integer idHoja, Integer idHojaContenedor, Integer idUsuario, Integer idBodega) {
        // Obtenemos la hoja de producción para la recepción
        HojaProduccionEntity hoja = obtenerHojaParaRecepcion(idHoja, idBodega);
        // Validamos que la HojaContenedor pertenezca a la hoja de producción
        hojaContenedorRepository.findByIdHojaContenedorAndHojaProduccion_IdHoja(idHojaContenedor, idHoja)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La HojaContenedor no pertenece a la hoja indicada."));
        // Liberamos el contenedor utilizando el servicio de hoja contenedor
        HojaContenedorDTO liberado = hojaContenedorService.liberar(
                idHojaContenedor, idUsuario, idBodega);
        // Evaluamos si la recepción de la hoja de producción está completa
        boolean recepcionCompleta = evaluarFinalizacion(hoja.getIdHoja());
        // Obtenemos la lista de contenedores pendientes de liberación
        List<RecepcionContenedorDTO> pendientes = hojaContenedorRepository
                .findByHojaProduccion_IdHojaAndFechaLiberacionIsNull(idHoja)
                .stream()
                .map(this::mapearContenedor)
                .toList();
        // Retornamos la respuesta indicando que el contenedor fue liberado correctamente
        return RecepcionLiberarContenedorResponse.builder()
                .mensaje(recepcionCompleta
                        ? "Contenedor liberado. La recepción fue completada."
                        : "Contenedor liberado correctamente.")
                .contenedor(mapearContenedor(liberado))
                .contenedoresPendientes(pendientes)
                .recepcionCompleta(recepcionCompleta)
                .build();
    }
    // Evalúa si la recepción de la hoja de producción está completa y cambia el estado si es necesario
    @Transactional
    public boolean evaluarFinalizacion(Integer idHoja) {
        // Obtenemos la hoja de producción para actualización y validamos su existencia
        HojaProduccionEntity hoja = hojaProduccionRepository.findByIdForUpdate(idHoja)
                .orElseThrow(() -> new IllegalArgumentException("La hoja de producción no existe."));
        // Obtenemos los detalles de la hoja de producción y verificamos si todos los productos han sido devueltos
        List<DetalleHojaEntity> detalles = detalleHojaRepository
                .findByHojaProduccionEntity_IdHoja(idHoja);
        // Verificamos si todos los detalles tienen la cantidad devuelta igual a la cantidad surtida
        boolean todosDevueltos = !detalles.isEmpty() && detalles.stream()
                .allMatch(detalle -> valor(detalle.getCantidadDevuelta())
                        == valor(detalle.getCantidadSurtida()));
        // Verificamos si no hay contenedores pendientes de liberación
        boolean sinContenedoresPendientes = !hojaContenedorRepository
                .existsByHojaProduccion_IdHojaAndFechaLiberacionIsNull(idHoja);
        // Si todos los productos han sido devueltos, no hay contenedores pendientes y la hoja está en estado "Recibiendo", cambiamos el estado a "Completado"
        if (todosDevueltos && sinContenedoresPendientes
                && Integer.valueOf(ESTADO_RECIBIENDO).equals(hoja.getEstadoHoja().getIdEstadoHoja())) {
            cambiarEstadoHoja(hoja, ESTADO_COMPLETADO);
            return true;
        }
        // Retornamos true si la hoja ya estaba en estado "Completado"
        return Integer.valueOf(ESTADO_COMPLETADO).equals(hoja.getEstadoHoja().getIdEstadoHoja());
    }
    // Obtiene la hoja de producción para la recepción, validando que esté en un estado adecuado
    private HojaProduccionEntity obtenerHojaParaRecepcion(Integer idHoja, Integer idBodega) {
        // Obtenemos la hoja de producción para consulta y validamos su existencia y pertenencia a la bodega
        HojaProduccionEntity hoja = obtenerHojaParaConsulta(idHoja, idBodega);
        // Validamos que la hoja esté en estado "En llamado" o "Recibiendo" para permitir la recepción
        Integer idEstado = hoja.getEstadoHoja() == null
                ? null : hoja.getEstadoHoja().getIdEstadoHoja();
        // Si la hoja no está en un estado adecuado, lanzamos una excepción
        if (!Integer.valueOf(ESTADO_EN_LLAMADO).equals(idEstado)
                && !Integer.valueOf(ESTADO_RECIBIENDO).equals(idEstado)) {
            throw new IllegalArgumentException(
                    "La hoja no está disponible para recepción. Debe estar En llamado o Recibiendo.");
        }
        return hoja;
    }
    // Obtiene la hoja de producción para consulta
    private HojaProduccionEntity obtenerHojaParaConsulta(Integer idHoja, Integer idBodega) {
        // Validamos que los IDs de hoja y bodega sean válidos
        validarId(idHoja, "La hoja de producción es obligatoria.");
        validarId(idBodega, "La bodega es obligatoria.");
        // Obtenemos la hoja de producción y validamos su existencia y pertenencia a la bodega
        HojaProduccionEntity hoja = hojaProduccionRepository.findById(idHoja)
                .orElseThrow(() -> new IllegalArgumentException("La hoja de producción no existe."));
        if (hoja.getBodega() == null || !idBodega.equals(hoja.getBodega().getIdBodega())) {
            throw new IllegalArgumentException("La hoja no pertenece a la bodega activa.");
        }
        // Validamos que la hoja esté en un estado adecuado para consulta
        Integer idEstado = hoja.getEstadoHoja() == null
                ? null : hoja.getEstadoHoja().getIdEstadoHoja();
        // Si la hoja no está en un estado adecuado, lanzamos una excepción
        if (!Integer.valueOf(ESTADO_EN_LLAMADO).equals(idEstado)
                && !Integer.valueOf(ESTADO_RECIBIENDO).equals(idEstado)
                && !Integer.valueOf(ESTADO_COMPLETADO).equals(idEstado)) {
            throw new IllegalArgumentException(
                    "La hoja no está disponible para consulta de recepción.");
        }
        return hoja;
    }
    // Cambia el estado de la hoja de producción a un nuevo estado
    private void cambiarEstadoHoja(HojaProduccionEntity hoja, int idEstado) {
        // Obtenemos el estado de hoja correspondiente al ID proporcionado y validamos su existencia
        EstadoHojaEntity estado = estadoHojaRepository.findById(idEstado)
                .orElseThrow(() -> new IllegalStateException("No existe el estado de hoja requerido."));
        // Cambiamos el estado de la hoja y guardamos los cambios en el repositorio
        hoja.setEstadoHoja(estado);
        hojaProduccionRepository.save(hoja);
    }
    // Construye un resumen de la hoja de producción para la recepción
    private RecepcionResumenDTO construirResumenDeHoja(Integer idHoja) {
        // Obtenemos los detalles de la hoja de producción y calculamos los totales de surtido y devuelto
        List<DetalleHojaEntity> detalles = detalleHojaRepository
                .findByHojaProduccionEntity_IdHoja(idHoja);
        int totalSurtido = detalles.stream()
                .mapToInt(detalle -> valor(detalle.getCantidadSurtida())).sum();
        int totalDevuelto = detalles.stream()
                .mapToInt(detalle -> valor(detalle.getCantidadDevuelta())).sum();
        // Verificamos si hay contenedores pendientes de liberación
        boolean sinContenedoresPendientes = !hojaContenedorRepository
                .existsByHojaProduccion_IdHojaAndFechaLiberacionIsNull(idHoja);
        // Construimos y retornamos el resumen de la hoja de producción
        return construirResumen(totalSurtido, totalDevuelto, sinContenedoresPendientes);
    }
    // Construye un resumen de la recepción con los totales de surtido, devuelto y pendientes
    private RecepcionResumenDTO construirResumen(
            int totalSurtido, int totalDevuelto, boolean sinContenedoresPendientes) {
        // Calculamos la cantidad pendiente de recepción
        int pendiente = Math.max(totalSurtido - totalDevuelto, 0);
        return RecepcionResumenDTO.builder()
                .totalSurtido(totalSurtido)
                .totalDevuelto(totalDevuelto)
                .totalPendiente(pendiente)
                .porcentaje(calcularPorcentaje(totalDevuelto, totalSurtido))
                .completo(totalSurtido > 0 && pendiente == 0 && sinContenedoresPendientes)
                .build();
    }
    // Mapea un detalle de hoja de producción a un DTO de detalle de recepción
    private RecepcionDetalleDTO mapearDetalle(DetalleHojaEntity detalle) {
        int surtido = valor(detalle.getCantidadSurtida());
        int devuelto = valor(detalle.getCantidadDevuelta());
        int pendiente = Math.max(surtido - devuelto, 0);
        return Mapper.toRecepcionDetalleDTO(
                detalle,
                surtido,
                devuelto,
                pendiente,
                calcularPorcentaje(devuelto, surtido),
                pendiente == 0);
    }
    // Mapea un contenedor de hoja de producción a un DTO de contenedor de recepción
    private RecepcionContenedorDTO mapearContenedor(HojaContenedorEntity entity) {
        // Determinamos si el contenedor ha sido liberado (si tiene fecha de liberación)
        boolean liberado = entity.getFechaLiberacion() != null;
        return Mapper.toRecepcionContenedorDTO(entity, liberado, !liberado);
    }
    // Mapea un DTO de hoja contenedor a un DTO de contenedor de recepción
    private RecepcionContenedorDTO mapearContenedor(HojaContenedorDTO dto) {
        return Mapper.toRecepcionContenedorDTO(dto);
    }
    // Obtiene un contenedor de recepción a partir del ID de la hoja contenedor
    private RecepcionContenedorDTO obtenerContenedor(Integer idHojaContenedor) {
        return hojaContenedorRepository.findById(idHojaContenedor)
                .map(this::mapearContenedor)
                .orElse(null);
    }
    // Mapea un movimiento de hoja de producción a un DTO de movimiento de recepción
    private RecepcionMovimientoDTO mapearMovimiento(MovimientoEntity movimiento) {
        return Mapper.toRecepcionMovimientoDTO(movimiento);
    }
    // Mapea un DTO de movimiento a un DTO de movimiento de recepción
    private RecepcionMovimientoDTO mapearMovimiento(MovimientoDTO movimiento) {
        return Mapper.toRecepcionMovimientoDTO(movimiento);
    }
    // Normaliza un código (conjunto o contenedor) y valida que no sea nulo o vacío
    private String normalizarCodigo(String codigo, String mensaje) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return codigo.trim().toUpperCase(Locale.ROOT);
    }
    // Calcula el porcentaje de un valor respecto a un total, asegurando que no exceda 100%
    private int calcularPorcentaje(int valor, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.min((valor * 100) / total, 100);
    }
    // Retorna el valor de un Integer, devolviendo 0 si es nulo
    private int valor(Integer valor) {
        return valor == null ? 0 : valor;
    }
    // Valida que un ID sea mayor a cero, lanzando una excepción si no lo es
    private void validarId(Integer id, String mensaje) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}

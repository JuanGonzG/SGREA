package dgtic.core.service;

import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.HojaContenedorAsignacionDTO;
import dgtic.core.model.dto.MovimientoDTO;
import dgtic.core.model.dto.MovimientoSalidaDTO;
import dgtic.core.model.dto.SurtidoAsignarContenedorResponse;
import dgtic.core.model.dto.SurtidoCerrarCargaResponse;
import dgtic.core.model.dto.SurtidoContenedorDTO;
import dgtic.core.model.dto.SurtidoDetalleDTO;
import dgtic.core.model.dto.SurtidoEstadoDTO;
import dgtic.core.model.dto.SurtidoMovimientoDTO;
import dgtic.core.model.dto.SurtidoResumenDTO;
import dgtic.core.model.dto.SurtidoSalidaResponse;
import dgtic.core.model.entity.ConjuntoEntity;
import dgtic.core.model.entity.DetalleHojaEntity;
import dgtic.core.model.entity.HojaContenedorEntity;
import dgtic.core.model.entity.HojaProduccionEntity;
import dgtic.core.model.entity.MovimientoEntity;
import dgtic.core.model.entity.EstadoHojaEntity;
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

import java.util.List;
import java.util.Locale;

@Service
public class SurtidoService {
    private static final int TIPO_SALIDA = 1;
    private static final int ESTADO_CONTENEDOR_EN_USO = 2;
    private static final int ESTADO_HOJA_POR_SURTIR = 2;
    private static final int ESTADO_HOJA_SURTIENDO = 3;
    private static final int ESTADO_HOJA_EN_LLAMADO = 4;

    @Autowired
    private HojaProduccionRepository hojaProduccionRepository;

    @Autowired
    private DetalleHojaRepository detalleHojaRepository;

    @Autowired
    private ConjuntoRepository conjuntoRepository;

    @Autowired
    private ContenedorRepository contenedorRepository;

    @Autowired
    private HojaContenedorRepository hojaContenedorRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private EstadoHojaRepository estadoHojaRepository;

    @Autowired
    private HojaContenedorService hojaContenedorService;

    @Autowired
    private MovimientoService movimientoService;

    // Obtener el estado de surtido de una hoja de producción en una bodega específica
    @Transactional(readOnly = true)
    public SurtidoEstadoDTO obtenerEstado(Integer idHoja, Integer idBodega) {
        // Validar y obtener la hoja de producción en la bodega especificada
        HojaProduccionEntity hoja = obtenerHojaEnBodega(idHoja, idBodega);
        validarHojaParaSurtido(hoja);
        // Obtener los detalles de la hoja de producción
        List<DetalleHojaEntity> detalles = detalleHojaRepository
                .findByHojaProduccionEntity_IdHoja(hoja.getIdHoja());
        // Calcular los totales de cantidad solicitada y surtida
        int totalSolicitado = detalles.stream()
                .mapToInt(detalle -> valor(detalle.getCantidadSolicitada()))
                .sum();
        int totalSurtido = detalles.stream()
                .mapToInt(detalle -> valor(detalle.getCantidadSurtida()))
                .sum();
        // Construir el resumen del surtido
        SurtidoResumenDTO resumen = construirResumen(totalSolicitado, totalSurtido);
        // Mapear los detalles a DTOs
        List<SurtidoDetalleDTO> detallesDTO = detalles.stream()
                .map(this::mapearDetalle)
                .toList();
        // Obtener el contenedor activo de la hoja de producción
        SurtidoContenedorDTO contenedorActivo = hojaContenedorRepository
                .findByHojaProduccion_IdHojaAndFechaCierreCargaIsNullAndFechaLiberacionIsNull(
                        hoja.getIdHoja())
                .map(this::mapearContenedor)
                .orElse(null);
        // Obtener los últimos movimientos de la hoja de producción
        List<SurtidoMovimientoDTO> ultimosMovimientos = movimientoRepository
                .findTop5ByHojaProduccion_IdHojaOrderByFechaDescIdMovimientoDesc(hoja.getIdHoja())
                .stream()
                .map(Mapper::toSurtidoMovimientoDTO)
                .toList();
        // Construir y retornar el DTO de estado de surtido
        return SurtidoEstadoDTO.builder()
                .idHoja(hoja.getIdHoja())
                .nombreProyecto(hoja.getNombreProyecto())
                .cliente(hoja.getCliente())
                .fechaSalida(hoja.getFechaSalida())
                .totalSolicitado(resumen.getTotalSolicitado())
                .totalSurtido(resumen.getTotalSurtido())
                .totalPendiente(resumen.getTotalPendiente())
                .porcentaje(resumen.getPorcentaje())
                .completo(resumen.getCompleto())
                .contenedorActivo(contenedorActivo)
                .detalles(detallesDTO)
                .ultimosMovimientos(ultimosMovimientos)
                .build();
    }
    // Asignar un contenedor a una hoja de producción en una bodega específica
    @Transactional
    public SurtidoAsignarContenedorResponse asignarContenedor(Integer idHoja, String codigo, Integer idUsuario, Integer idBodega) {
        // Validar y obtener la hoja de producción en la bodega especificada
        HojaProduccionEntity hoja = obtenerHojaEnBodega(idHoja, idBodega);
        validarHojaParaSurtido(hoja);
        // Verificar si la hoja ya tiene un contenedor activo
        if (hojaContenedorRepository
                .findByHojaProduccion_IdHojaAndFechaCierreCargaIsNullAndFechaLiberacionIsNull(
                        hoja.getIdHoja())
                .isPresent()) {
            throw new IllegalArgumentException("La hoja ya tiene un contenedor activo.");
        }
        // Normalizar el código del contenedor
        String codigoNormalizado = normalizarCodigo(codigo, "El código del contenedor es obligatorio.");
        // Buscar el contenedor por su código y obtener su ID
        Integer idContenedor = contenedorRepository.findByCodigoIgnoreCase(codigoNormalizado)
                .map(contenedor -> contenedor.getIdContenedor())
                .orElseThrow(() -> new IllegalArgumentException("El contenedor no existe."));
        // Crear un DTO de asignación de contenedor y asignarlo a la hoja
        HojaContenedorAsignacionDTO asignacion = HojaContenedorAsignacionDTO.builder()
                .idHoja(hoja.getIdHoja())
                .idContenedor(idContenedor)
                .build();
        hojaContenedorService.asignar(asignacion, idUsuario, idBodega);
        // Obtener el contenedor activo de la hoja de producción después de la asignación
        HojaContenedorEntity contenedorActivo = hojaContenedorRepository
                .findByHojaProduccion_IdHojaAndFechaCierreCargaIsNullAndFechaLiberacionIsNull(
                        hoja.getIdHoja())
                .orElseThrow(() -> new IllegalStateException(
                        "No fue posible recuperar el contenedor asignado."));
        // Construir y retornar la respuesta de asignación de contenedor
        return SurtidoAsignarContenedorResponse.builder()
                .mensaje("Contenedor asignado correctamente.")
                .contenedor(mapearContenedor(contenedorActivo))
                .build();
    }

    // Registrar la salida de un conjunto de una hoja de producción en una bodega específica
    @Transactional
    public SurtidoSalidaResponse registrarSalida(Integer idHoja, Integer idHojaContenedor, String codigoConjunto, Integer idUsuario, Integer idBodega) {
        // Validar y obtener la hoja de producción en la bodega especificada
        HojaProduccionEntity hoja = obtenerHojaEnBodega(idHoja, idBodega);
        validarHojaParaSurtido(hoja);
        // Validar que la hoja tenga un contenedor activo
        String codigoNormalizado = normalizarCodigo(
                codigoConjunto, "El código del conjunto es obligatorio.");
        // Validar que la hoja tenga un contenedor activo
        ConjuntoEntity conjunto = conjuntoRepository.findById(codigoNormalizado)
                .orElseThrow(() -> new IllegalArgumentException("El conjunto no existe."));
        if (conjunto.getProducto() == null || conjunto.getProducto().getIdProducto() == null) {
            throw new IllegalArgumentException("El conjunto no tiene un producto válido.");
        }
        // Validar que exista un detalle de hoja para el producto del conjunto
        DetalleHojaEntity detalle = detalleHojaRepository
                .findByHojaProduccionEntity_IdHojaAndProducto_IdProducto(
                        hoja.getIdHoja(), conjunto.getProducto().getIdProducto())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe detalle para el producto del conjunto en la hoja."));

        // Crear un DTO de salida de movimiento
        MovimientoSalidaDTO salida = MovimientoSalidaDTO.builder()
                .idHoja(hoja.getIdHoja())
                .idDetalle(detalle.getIdDetalle())
                .idConjunto(codigoNormalizado)
                .idHojaContenedor(idHojaContenedor)
                .build();
        // Registrar la salida del conjunto y obtener el movimiento registrado
        MovimientoDTO movimiento = movimientoService.registrarSalida(salida, idUsuario, idBodega);
        // Cambiar el estado de la hoja a "Surtiendo" si estaba en "Por surtir"
        if (Integer.valueOf(ESTADO_HOJA_POR_SURTIR).equals(hoja.getEstadoHoja().getIdEstadoHoja())) {
            cambiarEstadoHoja(hoja, ESTADO_HOJA_SURTIENDO,
                    "No existe el estado de hoja Surtiendo.");
        }

        // Recuperar el movimiento registrado y el detalle actualizado
        MovimientoEntity movimientoEntity = movimientoRepository.findById(movimiento.getIdMovimiento())
                .orElseThrow(() -> new IllegalStateException(
                        "No fue posible recuperar el movimiento registrado."));
        // Recuperar el detalle actualizado después de la salida
        DetalleHojaEntity detalleActualizado = detalleHojaRepository.findById(detalle.getIdDetalle())
                .orElseThrow(() -> new IllegalStateException(
                        "No fue posible recuperar el detalle actualizado."));
        // Recuperar el contenedor actualizado después de la salida
        HojaContenedorEntity contenedorActualizado = obtenerHojaContenedorDeHoja(
                idHojaContenedor, hoja);
        // Mapear el detalle actualizado a DTO y construir el resumen de la hoja
        SurtidoDetalleDTO detalleDTO = mapearDetalle(detalleActualizado);
        SurtidoResumenDTO resumen = construirResumenDeHoja(hoja.getIdHoja());
        // Construir y retornar la respuesta de salida registrada
        return SurtidoSalidaResponse.builder()
                .mensaje("Salida registrada correctamente.")
                .movimiento(Mapper.toSurtidoMovimientoDTO(movimientoEntity))
                .detalle(detalleDTO)
                .contenedor(mapearContenedor(contenedorActualizado))
                .hoja(resumen)
                .build();
    }
    // Cerrar la carga de un contenedor de una hoja de producción en una bodega específica
    @Transactional
    public SurtidoCerrarCargaResponse cerrarCarga(Integer idHoja, Integer idHojaContenedor, Integer idUsuario, Integer idBodega) {
        // Validar y obtener la hoja de producción en la bodega especificada
        HojaProduccionEntity hoja = obtenerHojaEnBodega(idHoja, idBodega);
        validarHojaParaSurtido(hoja);
        // Validar que la hoja tenga un contenedor activo
        HojaContenedorEntity asignacion = obtenerHojaContenedorDeHoja(idHojaContenedor, hoja);
        // Validar que la hoja-contenedor tenga movimientos de salida antes de cerrar la carga
        long salidas = movimientoRepository
                .countByHojaContenedor_IdHojaContenedorAndTipoMovimiento_IdTipoMovimiento(
                        asignacion.getIdHojaContenedor(), TIPO_SALIDA);
        // Si no hay salidas registradas, lanzar una excepción
        if (salidas == 0) {
            throw new IllegalArgumentException("No se puede cerrar una carga vacía.");
        }
        // Cerrar la carga del contenedor de la hoja de producción
        hojaContenedorService.cerrarCarga(idHojaContenedor, idUsuario, idBodega);
        // Recuperar la hoja-contenedor cerrada y construir el resumen de la hoja
        HojaContenedorEntity cerrada = hojaContenedorRepository.findById(idHojaContenedor)
                .orElseThrow(() -> new IllegalStateException(
                        "No fue posible recuperar la carga cerrada."));
        // Construir el resumen de la hoja y verificar si el surtido está completo
        SurtidoResumenDTO resumen = construirResumenDeHoja(hoja.getIdHoja());
        boolean surtidoCompleto = Boolean.TRUE.equals(resumen.getCompleto());
        // Cambiar el estado de la hoja a "En llamado" si el surtido está completo
        if (surtidoCompleto) {
            cambiarEstadoHoja(hoja, ESTADO_HOJA_EN_LLAMADO,
                    "No existe el estado de hoja En llamado.");
        }

        // Construir y retornar la respuesta de cierre de carga
        return SurtidoCerrarCargaResponse.builder()
                .mensaje(surtidoCompleto
                        ? "Surtido completado. La hoja cambió al estado En llamado."
                        : "Carga cerrada correctamente.")
                .contenedor(mapearContenedor(cerrada))
                .requiereNuevoContenedor(!surtidoCompleto)
                .surtidoCompleto(surtidoCompleto)
                .build();
    }

    // Mapear un detalle de hoja a un DTO de surtido
    private SurtidoDetalleDTO mapearDetalle(DetalleHojaEntity detalle) {
        // Calcular la cantidad solicitada, surtida y pendiente
        int solicitada = valor(detalle.getCantidadSolicitada());
        int surtida = valor(detalle.getCantidadSurtida());
        int pendiente = Math.max(solicitada - surtida, 0);
        return Mapper.toSurtidoDetalleDTO(
                detalle,
                surtida,
                pendiente,
                calcularPorcentaje(surtida, solicitada),
                pendiente == 0);
    }

    // Mapear un contenedor de hoja a un DTO de surtido
    private SurtidoContenedorDTO mapearContenedor(HojaContenedorEntity hojaContenedor) {
        // Calcular la capacidad y ocupación del contenedor
        int capacidad = valor(hojaContenedor.getContenedor().getCapacidad());
        int ocupacion = (int) movimientoRepository
                .countByHojaContenedor_IdHojaContenedorAndTipoMovimiento_IdTipoMovimiento(
                        hojaContenedor.getIdHojaContenedor(), TIPO_SALIDA);
        // Determinar si la carga está abierta y si el contenedor está disponible para surtido
        boolean cargaAbierta = hojaContenedor.getFechaCierreCarga() == null
                && hojaContenedor.getFechaLiberacion() == null;
        Integer idEstadoContenedor = hojaContenedor.getContenedor().getEstadoContenedor() == null
                ? null
                : hojaContenedor.getContenedor().getEstadoContenedor().getIdEstadoContenedor();
        // Verificar si el contenedor está disponible para surtido
        boolean disponible = cargaAbierta
                && Integer.valueOf(ESTADO_CONTENEDOR_EN_USO).equals(idEstadoContenedor)
                && ocupacion < capacidad;
        // Construir y retornar el DTO de contenedor de surtido
        return Mapper.toSurtidoContenedorDTO(
                hojaContenedor,
                ocupacion,
                calcularPorcentaje(ocupacion, capacidad),
                disponible,
                capacidad > 0 && ocupacion >= capacidad);
    }
    // Construir un resumen de surtido para una hoja de producción específica
    private SurtidoResumenDTO construirResumenDeHoja(Integer idHoja) {
        // Obtener los detalles de la hoja de producción
        List<DetalleHojaEntity> detalles = detalleHojaRepository
                .findByHojaProduccionEntity_IdHoja(idHoja);
        // Calcular los totales de cantidad solicitada y surtida
        int totalSolicitado = detalles.stream()
                .mapToInt(detalle -> valor(detalle.getCantidadSolicitada()))
                .sum();
        // Calcular el total de cantidad surtida
        int totalSurtido = detalles.stream()
                .mapToInt(detalle -> valor(detalle.getCantidadSurtida()))
                .sum();
        // Construir y retornar el resumen del surtido
        return construirResumen(totalSolicitado, totalSurtido);
    }
    // Construir un resumen de surtido a partir de los totales de cantidad solicitada y surtida
    private SurtidoResumenDTO construirResumen(int totalSolicitado, int totalSurtido) {
        // Calcular el total pendiente de surtido
        int totalPendiente = Math.max(totalSolicitado - totalSurtido, 0);
        // Construir y retornar el DTO de resumen de surtido
        return SurtidoResumenDTO.builder()
                .totalSolicitado(totalSolicitado)
                .totalSurtido(totalSurtido)
                .totalPendiente(totalPendiente)
                .porcentaje(calcularPorcentaje(totalSurtido, totalSolicitado))
                .completo(totalPendiente == 0)
                .build();
    }
    // Validar y obtener una hoja de producción en una bodega específica
    private HojaProduccionEntity obtenerHojaEnBodega(Integer idHoja, Integer idBodega) {
        // Validar que los IDs de hoja y bodega sean válidos
        validarId(idHoja, "La hoja de producción es obligatoria.");
        validarId(idBodega, "La bodega es obligatoria.");
        // Obtener la hoja de producción por su ID y lanzar una excepción si no existe
        HojaProduccionEntity hoja = hojaProduccionRepository.findById(idHoja)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La hoja de producción no existe."));
        // Validar que la hoja pertenezca a la bodega activa
        if (hoja.getBodega() == null || !idBodega.equals(hoja.getBodega().getIdBodega())) {
            throw new IllegalArgumentException("La hoja no pertenece a la bodega activa.");
        }
        return hoja;
    }
    // Validar que la hoja de producción esté en un estado válido para surtido
    private void validarHojaParaSurtido(HojaProduccionEntity hoja) {
        // Validar que la hoja de producción tenga un estado válido para surtido
        Integer idEstado = hoja.getEstadoHoja() == null
                ? null
                : hoja.getEstadoHoja().getIdEstadoHoja();
        // Verificar si el estado de la hoja es "Por surtir" o "Surtiendo"
        if (!Integer.valueOf(ESTADO_HOJA_POR_SURTIR).equals(idEstado)
                && !Integer.valueOf(ESTADO_HOJA_SURTIENDO).equals(idEstado)) {
            throw new IllegalArgumentException(
                    "La hoja no está disponible para surtido. Debe estar en estado Por surtir o Surtiendo.");
        }
    }
    // Cambiar el estado de una hoja de producción a un nuevo estado
    private void cambiarEstadoHoja(HojaProduccionEntity hoja, int idEstado, String mensajeError) {
        // Obtener el nuevo estado de hoja por su ID y lanzar una excepción si no existe
        EstadoHojaEntity estado = estadoHojaRepository.findById(idEstado)
                .orElseThrow(() -> new IllegalStateException(mensajeError));
        // Cambiar el estado de la hoja de producción y guardar los cambios en el repositorio
        hoja.setEstadoHoja(estado);
        hojaProduccionRepository.save(hoja);
    }
    // Validar y obtener una hoja-contenedor asociada a una hoja de producción específica
    private HojaContenedorEntity obtenerHojaContenedorDeHoja(Integer idHojaContenedor, HojaProduccionEntity hoja) {
        // Validar que el ID de hoja-contenedor sea válido
        validarId(idHojaContenedor, "La asignación HojaContenedor es obligatoria.");
        // Obtener la asignación de hoja-contenedor por su ID y lanzar una excepción si no existe
        HojaContenedorEntity asignacion = hojaContenedorRepository.findById(idHojaContenedor)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La asignación HojaContenedor no existe."));
        // Validar que la hoja-contenedor pertenezca a la hoja de producción indicada
        if (asignacion.getHojaProduccion() == null
                || !hoja.getIdHoja().equals(asignacion.getHojaProduccion().getIdHoja())) {
            throw new IllegalArgumentException(
                    "La HojaContenedor no pertenece a la hoja indicada.");
        }
        return asignacion;
    }
    // Normalizar un código de conjunto o contenedor, asegurando que no sea nulo ni vacío
    private String normalizarCodigo(String codigo, String mensaje) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return codigo.trim().toUpperCase(Locale.ROOT);
    }
    // Calcular el porcentaje de un valor respecto a un total, asegurando que no exceda el 100%
    private int calcularPorcentaje(int valor, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.min((valor * 100) / total, 100);
    }
    // Obtener el valor de un Integer, devolviendo 0 si es nulo
    private int valor(Integer valor) {
        return valor == null ? 0 : valor;
    }
    // Validar que un ID sea mayor que cero, lanzando una excepción si no lo es
    private void validarId(Integer id, String mensaje) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}

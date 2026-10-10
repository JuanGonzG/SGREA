package dgtic.core.service;

import dgtic.core.exception.MonitoreoNoAutorizadoException;
import dgtic.core.exception.MonitoreoRecursoNoEncontradoException;
import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.monitoreo.MonitoreoContenedorDTO;
import dgtic.core.model.dto.monitoreo.MonitoreoDetalleDTO;
import dgtic.core.model.dto.monitoreo.MonitoreoEstadoDTO;
import dgtic.core.model.dto.monitoreo.MonitoreoHojaDTO;
import dgtic.core.model.dto.monitoreo.MonitoreoMovimientoDTO;
import dgtic.core.model.entity.DetalleHojaEntity;
import dgtic.core.model.entity.HojaContenedorEntity;
import dgtic.core.model.entity.HojaProduccionEntity;
import dgtic.core.model.entity.MovimientoEntity;
import dgtic.core.model.entity.UsuarioEntity;
import dgtic.core.repository.DetalleHojaRepository;
import dgtic.core.repository.HojaContenedorRepository;
import dgtic.core.repository.HojaProduccionRepository;
import dgtic.core.repository.MovimientoRepository;
import dgtic.core.repository.UsuarioBodegaRepository;
import dgtic.core.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MonitoreoService {
    // Constantes para los roles y tipos de movimiento
    private static final int ROL_ADMINISTRADOR = 1;
    private static final int ROL_SUPERVISOR = 2;
    private static final int TIPO_SALIDA = 1;
    private static final int TIPO_ENTRADA = 2;
    // Constantes para los estados de la hoja de producción
    private static final int ESTADO_POR_SURTIR = 2;
    private static final int ESTADO_SURTIENDO = 3;
    private static final int ESTADO_EN_LLAMADO = 4;
    private static final int ESTADO_RECIBIENDO = 5;
    private static final int ESTADO_COMPLETADO = 6;
    // Repositorios para acceder a los datos
    private final DetalleHojaRepository detalleHojaRepository;
    private final HojaContenedorRepository hojaContenedorRepository;
    private final HojaProduccionRepository hojaProduccionRepository;
    private final MovimientoRepository movimientoRepository;
    private final UsuarioBodegaRepository usuarioBodegaRepository;
    private final UsuarioRepository usuarioRepository;

    public MonitoreoService(
            DetalleHojaRepository detalleHojaRepository,
            HojaContenedorRepository hojaContenedorRepository,
            HojaProduccionRepository hojaProduccionRepository,
            MovimientoRepository movimientoRepository,
            UsuarioBodegaRepository usuarioBodegaRepository,
            UsuarioRepository usuarioRepository) {
        this.detalleHojaRepository = detalleHojaRepository;
        this.hojaContenedorRepository = hojaContenedorRepository;
        this.hojaProduccionRepository = hojaProduccionRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioBodegaRepository = usuarioBodegaRepository;
        this.usuarioRepository = usuarioRepository;
    }
    // Método para validar el acceso al monitoreo
    public void validarAccesoMonitoreo(Integer idUsuario, Integer idBodega) {
        validarAccesoMonitoreoInterno(idUsuario, idBodega);
    }
    // Métodos para obtener los surtidos y recepciones activas
    public List<MonitoreoHojaDTO> obtenerSurtidosActivos(Integer idUsuario, Integer idBodega) {
        // Validar el acceso del usuario a la bodega
        validarAccesoMonitoreoInterno(idUsuario, idBodega);
        // Obtener las hojas de producción activas para surtido
        return hojaProduccionRepository
                .findByBodega_IdBodegaAndEstadoHoja_IdEstadoHojaInOrderByFechaSalidaAscIdHojaAsc(
                        idBodega, List.of(ESTADO_POR_SURTIR, ESTADO_SURTIENDO))
                .stream()
                .map(hoja -> construirResumen(hoja, true))
                .toList();
    }
    // Método para obtener las recepciones activas
    public List<MonitoreoHojaDTO> obtenerRecepcionesActivas(Integer idUsuario, Integer idBodega) {
        // Validar el acceso del usuario a la bodega
        validarAccesoMonitoreoInterno(idUsuario, idBodega);
        // Obtener las hojas de producción activas para recepción
        return hojaProduccionRepository
                .findByBodega_IdBodegaAndEstadoHoja_IdEstadoHojaInOrderByFechaSalidaAscIdHojaAsc(
                        idBodega, List.of(ESTADO_EN_LLAMADO, ESTADO_RECIBIENDO))
                .stream()
                .map(hoja -> construirResumen(hoja, false))
                .toList();
    }
    // Métodos para obtener el estado de surtido y recepción de una hoja específica
    public MonitoreoEstadoDTO obtenerSurtido(Integer idUsuario, Integer idBodega, Integer idHoja) {
        // Validar el acceso del usuario a la bodega
        validarAccesoMonitoreoInterno(idUsuario, idBodega);
        // Obtener la hoja de producción autorizada
        HojaProduccionEntity hoja = obtenerHojaAutorizada(idHoja, idBodega);
        // Validar que la hoja esté en un estado válido para surtido
        validarEstado(hoja, List.of(ESTADO_POR_SURTIR, ESTADO_SURTIENDO, ESTADO_EN_LLAMADO),
                "La hoja no está disponible para consulta de surtido.");
        // Construir y retornar el estado de surtido
        return construirEstado(hoja, TIPO_SALIDA);
    }
    // Método para obtener el estado de recepción de una hoja específica
    public MonitoreoEstadoDTO obtenerRecepcion(Integer idUsuario, Integer idBodega, Integer idHoja) {
        // Validar el acceso del usuario a la bodega
        validarAccesoMonitoreoInterno(idUsuario, idBodega);
        // Obtener la hoja de producción autorizada
        HojaProduccionEntity hoja = obtenerHojaAutorizada(idHoja, idBodega);
        // Validar que la hoja esté en un estado válido para recepción
        validarEstado(hoja, List.of(ESTADO_EN_LLAMADO, ESTADO_RECIBIENDO, ESTADO_COMPLETADO),
                "La hoja no está disponible para consulta de recepción.");
        // Construir y retornar el estado de recepción
        return construirEstado(hoja, TIPO_ENTRADA);
    }
    // Métodos privados para construir los DTOs de resumen y estado
    private MonitoreoHojaDTO construirResumen(HojaProduccionEntity hoja, boolean surtido) {
        // Obtener los detalles de la hoja de producción
        List<DetalleHojaEntity> detalles = obtenerDetalles(hoja);
        // Calcular los totales de solicitado, surtido y devuelto
        int totalSolicitado = detalles.stream()
                .mapToInt(detalle -> valor(detalle.getCantidadSolicitada()))
                .sum();
        int totalSurtido = detalles.stream()
                .mapToInt(detalle -> valor(detalle.getCantidadSurtida()))
                .sum();
        int totalDevuelto = detalles.stream()
                .mapToInt(detalle -> valor(detalle.getCantidadDevuelta()))
                .sum();
        // Determinar el total y procesado según si es surtido o recepción
        int total = surtido ? totalSolicitado : totalSurtido;
        int procesado = surtido ? totalSurtido : totalDevuelto;
        // Construir y retornar el DTO de resumen de la hoja
        return Mapper.toMonitoreoHojaDTO(
                hoja,
                total,
                procesado,
                pendiente(total, procesado),
                calcularPorcentaje(procesado, total),
                obtenerInicio(hoja, surtido ? TIPO_SALIDA : TIPO_ENTRADA),
                obtenerUltimaActividad(hoja),
                obtenerUltimoOperador(hoja),
                // Contar los contenedores activos (sin fecha de liberación) para la hoja
                Math.toIntExact(hojaContenedorRepository
                        .countByHojaProduccion_IdHojaAndFechaLiberacionIsNull(hoja.getIdHoja())));
    }
    // Método privado para construir el estado de una hoja de producción
    private MonitoreoEstadoDTO construirEstado(HojaProduccionEntity hoja, int tipoMovimiento) {
        // Obtener los detalles de la hoja de producción
        List<DetalleHojaEntity> detalles = obtenerDetalles(hoja);
        // Calcular los totales de solicitado, surtido y devuelto
        int totalSolicitado = detalles.stream()
                .mapToInt(detalle -> valor(detalle.getCantidadSolicitada()))
                .sum();
        int totalSurtido = detalles.stream()
                .mapToInt(detalle -> valor(detalle.getCantidadSurtida()))
                .sum();
        int totalDevuelto = detalles.stream()
                .mapToInt(detalle -> valor(detalle.getCantidadDevuelta()))
                .sum();
        // Mapear los detalles a DTOs
        List<MonitoreoDetalleDTO> detallesDTO = detalles.stream()
                .map(this::mapearDetalle)
                .toList();
        // Mapear los contenedores a DTOs
        List<MonitoreoContenedorDTO> contenedores = hojaContenedorRepository
                .findByHojaProduccion_IdHojaOrderByFechaAsignacionAscIdHojaContenedorAsc(hoja.getIdHoja())
                .stream()
                .map(this::mapearContenedor)
                .toList();
        // Mapear los movimientos a DTOs
        List<MonitoreoMovimientoDTO> movimientos = movimientoRepository
                .findByHojaProduccion_IdHojaAndTipoMovimiento_IdTipoMovimientoOrderByFechaDescIdMovimientoDesc(
                        hoja.getIdHoja(), tipoMovimiento)
                .stream()
                .map(Mapper::toMonitoreoMovimientoDTO)
                .toList();
        // Construir y retornar el DTO de estado de la hoja
        return Mapper.toMonitoreoEstadoDTO(
                hoja,
                totalSolicitado,
                totalSurtido,
                totalDevuelto,
                pendiente(totalSolicitado, totalSurtido),
                pendiente(totalSurtido, totalDevuelto),
                calcularPorcentaje(totalSurtido, totalSolicitado),
                calcularPorcentaje(totalDevuelto, totalSurtido),
                obtenerInicio(hoja, TIPO_SALIDA),
                obtenerInicio(hoja, TIPO_ENTRADA),
                obtenerUltimaActividad(hoja),
                obtenerUltimoOperador(hoja),
                detallesDTO,
                contenedores,
                movimientos);
    }
    // Métodos privados para mapear entidades a DTOs
    private MonitoreoDetalleDTO mapearDetalle(DetalleHojaEntity detalle) {
        // Calcular los valores de solicitado, surtido y devuelto
        int solicitada = valor(detalle.getCantidadSolicitada());
        int surtida = valor(detalle.getCantidadSurtida());
        int devuelta = valor(detalle.getCantidadDevuelta());
        // Construir y retornar el DTO de detalle de la hoja
        return Mapper.toMonitoreoDetalleDTO(
                detalle,
                surtida,
                devuelta,
                pendiente(solicitada, surtida),
                pendiente(surtida, devuelta),
                calcularPorcentaje(surtida, solicitada),
                calcularPorcentaje(devuelta, surtida));
    }
    // Método privado para mapear un contenedor a DTO
    private MonitoreoContenedorDTO mapearContenedor(HojaContenedorEntity hojaContenedor) {
        // Contar los movimientos de salida asociados al contenedor
        int ocupacion = Math.toIntExact(movimientoRepository
                .countByHojaContenedor_IdHojaContenedorAndTipoMovimiento_IdTipoMovimiento(
                        hojaContenedor.getIdHojaContenedor(), TIPO_SALIDA));
        // Construir y retornar el DTO de contenedor
        return Mapper.toMonitoreoContenedorDTO(hojaContenedor, ocupacion);
    }
    // Métodos privados para obtener detalles, inicio, última actividad y último operador
    private List<DetalleHojaEntity> obtenerDetalles(HojaProduccionEntity hoja) {
        return detalleHojaRepository.findByHojaProduccionEntity_IdHoja(hoja.getIdHoja());
    }
    // Método privado para obtener la fecha de inicio de un tipo de movimiento específico
    private LocalDateTime obtenerInicio(HojaProduccionEntity hoja, int tipoMovimiento) {
        return movimientoRepository
                .findFirstByHojaProduccion_IdHojaAndTipoMovimiento_IdTipoMovimientoOrderByFechaAscIdMovimientoAsc(
                        hoja.getIdHoja(), tipoMovimiento)
                .map(MovimientoEntity::getFecha)
                .orElse(null);
    }
    // Método privado para obtener la fecha de la última actividad de la hoja
    private LocalDateTime obtenerUltimaActividad(HojaProduccionEntity hoja) {
        return movimientoRepository
                .findFirstByHojaProduccion_IdHojaOrderByFechaDescIdMovimientoDesc(hoja.getIdHoja())
                .map(MovimientoEntity::getFecha)
                .orElse(null);
    }

    // Método privado para obtener el nombre del último operador de la hoja
    private String obtenerUltimoOperador(HojaProduccionEntity hoja) {
        return movimientoRepository
                .findFirstByHojaProduccion_IdHojaOrderByFechaDescIdMovimientoDesc(hoja.getIdHoja())
                .map(MovimientoEntity::getUsuario)
                .map(UsuarioEntity::getNombre)
                .orElse(null);
    }
    // Método privado para obtener una hoja de producción autorizada por ID y bodega
    private HojaProduccionEntity obtenerHojaAutorizada(Integer idHoja, Integer idBodega) {
        // Validar que el ID de la hoja sea válido
        validarId(idHoja, "La hoja de producción es obligatoria.");
        // Obtener la hoja de producción por ID
        HojaProduccionEntity hoja = hojaProduccionRepository.findById(idHoja)
                .orElseThrow(() -> new MonitoreoRecursoNoEncontradoException(
                        "La hoja de producción no existe."));
        // Verificar que la hoja pertenezca a la bodega activa
        if (hoja.getBodega() == null || !idBodega.equals(hoja.getBodega().getIdBodega())) {
            throw new MonitoreoRecursoNoEncontradoException(
                    "La hoja no pertenece a la bodega activa.");
        }
        return hoja;
    }
    // Método privado para validar el acceso del usuario al monitoreo interno
    private void validarAccesoMonitoreoInterno(Integer idUsuario, Integer idBodega) {
        // Validar que los IDs de usuario y bodega sean válidos
        validarId(idUsuario, "El usuario es obligatorio.");
        validarId(idBodega, "La bodega es obligatoria.");
        // Obtener el usuario por ID y verificar su autorización
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new MonitoreoNoAutorizadoException("El usuario no está autorizado."));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new MonitoreoNoAutorizadoException("El usuario está inactivo.");
        }
        // Verificar que el usuario tenga un rol de administrador o supervisor
        Integer idRol = usuario.getRol() == null ? null : usuario.getRol().getIdRol();
        if (!Integer.valueOf(ROL_ADMINISTRADOR).equals(idRol)
                && !Integer.valueOf(ROL_SUPERVISOR).equals(idRol)) {
            throw new MonitoreoNoAutorizadoException(
                    "El usuario no tiene permisos de administrador o supervisor.");
        }
        // Verificar que el usuario esté autorizado para la bodega
        if (!usuarioBodegaRepository.existsByUsuario_IdUsuarioAndBodega_IdBodega(idUsuario, idBodega)) {
            throw new MonitoreoNoAutorizadoException(
                    "El usuario no está autorizado para la bodega.");
        }
    }
    // Método privado para validar el estado de la hoja de producción
    private void validarEstado(HojaProduccionEntity hoja, List<Integer> estados, String mensaje) {
        // Obtener el ID del estado de la hoja, manejando el caso de estado nulo
        Integer idEstado = hoja.getEstadoHoja() == null
                ? null
                : hoja.getEstadoHoja().getIdEstadoHoja();
        // Verificar que el estado de la hoja esté en la lista de estados permitidos
        if (!estados.contains(idEstado)) {
            throw new IllegalArgumentException(mensaje);
        }
    }
    // Método privado para calcular el porcentaje de valor respecto al total
    private int calcularPorcentaje(int valor, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.min((valor * 100) / total, 100);
    }
    // Método privado para calcular la cantidad pendiente de procesar
    private int pendiente(int total, int procesado) {
        return Math.max(total - procesado, 0);
    }
    // Método privado para manejar valores nulos y retornar 0 en caso de ser nulo
    private int valor(Integer valor) {
        return valor == null ? 0 : valor;
    }
    // Método privado para validar que un ID sea positivo y no nulo
    private void validarId(Integer id, String mensaje) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}

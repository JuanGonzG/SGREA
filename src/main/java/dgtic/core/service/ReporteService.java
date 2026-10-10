package dgtic.core.service;

import dgtic.core.exception.ReporteNoAutorizadoException;
import dgtic.core.exception.ReporteRecursoNoEncontradoException;
import dgtic.core.model.dto.reporte.ReporteConjuntoDTO;
import dgtic.core.model.dto.reporte.ReporteConjuntoFiltroDTO;
import dgtic.core.model.dto.reporte.ReporteContenedorDTO;
import dgtic.core.model.dto.reporte.ReporteContenedorFiltroDTO;
import dgtic.core.model.dto.reporte.ReporteHojaDTO;
import dgtic.core.model.dto.reporte.ReporteHojaDetalleDTO;
import dgtic.core.model.dto.reporte.ReporteHojaFiltroDTO;
import dgtic.core.model.dto.reporte.ReporteMovimientoDTO;
import dgtic.core.model.dto.reporte.ReporteMovimientoFiltroDTO;
import dgtic.core.model.dto.reporte.ReporteOperacionDetalleDTO;
import dgtic.core.model.dto.reporte.ReporteOperacionHojaDTO;
import dgtic.core.model.entity.ConjuntoEntity;
import dgtic.core.model.entity.DetalleHojaEntity;
import dgtic.core.model.entity.HojaContenedorEntity;
import dgtic.core.model.entity.HojaProduccionEntity;
import dgtic.core.model.entity.MovimientoEntity;
import dgtic.core.model.entity.UsuarioEntity;
import dgtic.core.repository.ConjuntoRepository;
import dgtic.core.repository.DetalleHojaRepository;
import dgtic.core.repository.HojaContenedorRepository;
import dgtic.core.repository.HojaProduccionRepository;
import dgtic.core.repository.MovimientoRepository;
import dgtic.core.repository.UsuarioBodegaRepository;
import dgtic.core.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReporteService {
    // Definición de constantes para roles y tipos de movimiento
    private static final int ROL_ADMINISTRADOR = 1;
    private static final int ROL_SUPERVISOR = 2;
    private static final int TIPO_SALIDA = 1;
    private static final int TIPO_ENTRADA = 2;

    private final ConjuntoRepository conjuntoRepository;
    private final DetalleHojaRepository detalleHojaRepository;
    private final HojaContenedorRepository hojaContenedorRepository;
    private final HojaProduccionRepository hojaProduccionRepository;
    private final MovimientoRepository movimientoRepository;
    private final UsuarioBodegaRepository usuarioBodegaRepository;
    private final UsuarioRepository usuarioRepository;

    public ReporteService(
            ConjuntoRepository conjuntoRepository,
            DetalleHojaRepository detalleHojaRepository,
            HojaContenedorRepository hojaContenedorRepository,
            HojaProduccionRepository hojaProduccionRepository,
            MovimientoRepository movimientoRepository,
            UsuarioBodegaRepository usuarioBodegaRepository,
            UsuarioRepository usuarioRepository) {
        this.conjuntoRepository = conjuntoRepository;
        this.detalleHojaRepository = detalleHojaRepository;
        this.hojaContenedorRepository = hojaContenedorRepository;
        this.hojaProduccionRepository = hojaProduccionRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioBodegaRepository = usuarioBodegaRepository;
        this.usuarioRepository = usuarioRepository;
    }
    // Método para validar el acceso a los reportes según el usuario y la bodega
    public void validarAccesoReportes(Integer idUsuario, Integer idBodega) {
        // Validar que los parámetros no sean nulos o inválidos
        validarId(idUsuario, "El usuario es obligatorio.");
        validarId(idBodega, "La bodega es obligatoria.");
        // Validar que el usuario exista y esté activo
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ReporteNoAutorizadoException("El usuario no está autorizado."));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ReporteNoAutorizadoException("El usuario está inactivo.");
        }
        // Validar que el usuario tenga el rol de administrador o supervisor
        Integer idRol = usuario.getRol() == null ? null : usuario.getRol().getIdRol();
        if (!Integer.valueOf(ROL_ADMINISTRADOR).equals(idRol)
                && !Integer.valueOf(ROL_SUPERVISOR).equals(idRol)) {
            throw new ReporteNoAutorizadoException(
                    "El usuario no tiene permisos de administrador o supervisor.");
        }
        // Validar que el usuario esté autorizado para la bodega
        if (!usuarioBodegaRepository.existsByUsuario_IdUsuarioAndBodega_IdBodega(idUsuario, idBodega)) {
            throw new ReporteNoAutorizadoException(
                    "El usuario no está autorizado para la bodega.");
        }
    }
    // Método para buscar hojas de producción según los filtros proporcionados
    public List<ReporteHojaDTO> buscarHojas(Integer idUsuario, Integer idBodega, ReporteHojaFiltroDTO filtro) {
        // Validar el acceso a los reportes para el usuario y la bodega
        validarAccesoReportes(idUsuario, idBodega);
        // Si el filtro es nulo, se crea un filtro seguro con valores predeterminados
        ReporteHojaFiltroDTO filtroSeguro = filtro == null ? new ReporteHojaFiltroDTO() : filtro;
        // Validar el rango de fechas proporcionado en el filtro
        validarRango(filtroSeguro.getFechaDesde(), filtroSeguro.getFechaHasta());
        // Validar el ID del estado si se proporciona
        validarIdOpcional(filtroSeguro.getIdEstado(), "El estado");
        // Normalizar el texto de cliente y proyecto para evitar problemas de búsqueda
        return hojaProduccionRepository.buscarParaReporte(
                        idBodega,
                        inicioDelDia(filtroSeguro.getFechaDesde()),
                        finDelDia(filtroSeguro.getFechaHasta()),
                        filtroSeguro.getIdEstado(),
                        normalizarTexto(filtroSeguro.getCliente()),
                        normalizarTexto(filtroSeguro.getProyecto()))
                .stream()
                .map(this::mapearHoja)
                .toList();
    }
    // Método para buscar movimientos según los filtros proporcionados
    public List<ReporteMovimientoDTO> buscarMovimientos(Integer idUsuario, Integer idBodega, ReporteMovimientoFiltroDTO filtro) {
        // Validar el acceso a los reportes para el usuario y la bodega
        validarAccesoReportes(idUsuario, idBodega);
        // Si el filtro es nulo, se crea un filtro seguro con valores predeterminados
        ReporteMovimientoFiltroDTO filtroSeguro = filtro == null
                ? new ReporteMovimientoFiltroDTO()
                : filtro;
        // Validar el rango de fechas proporcionado en el filtro
        validarRango(filtroSeguro.getFechaDesde(), filtroSeguro.getFechaHasta());
        // Validar los IDs opcionales proporcionados en el filtro
        validarIdOpcional(filtroSeguro.getIdTipoMovimiento(), "El tipo de movimiento");
        validarIdOpcional(filtroSeguro.getIdHoja(), "La hoja");
        validarIdOpcional(filtroSeguro.getIdUsuario(), "El usuario");
        // Obtener los movimientos desde el repositorio según los filtros y mapearlos a DTOs
        return movimientoRepository.buscarParaReporte(
                        idBodega,
                        inicioDelDia(filtroSeguro.getFechaDesde()),
                        finDelDia(filtroSeguro.getFechaHasta()),
                        filtroSeguro.getIdTipoMovimiento(),
                        filtroSeguro.getIdHoja(),
                        normalizarTexto(filtroSeguro.getCodigoConjunto()),
                        filtroSeguro.getIdUsuario())
                .stream()
                .map(this::mapearMovimiento)
                .toList();
    }
    // Método para buscar contenedores según los filtros proporcionados
    public List<ReporteContenedorDTO> buscarContenedores(Integer idUsuario, Integer idBodega, ReporteContenedorFiltroDTO filtro) {
        // Validar el acceso a los reportes para el usuario y la bodega
        validarAccesoReportes(idUsuario, idBodega);
        // Si el filtro es nulo, se crea un filtro seguro con valores predeterminados
        ReporteContenedorFiltroDTO filtroSeguro = filtro == null
                ? new ReporteContenedorFiltroDTO()
                : filtro;
        // Validar el rango de fechas proporcionado en el filtro
        validarRango(filtroSeguro.getFechaDesde(), filtroSeguro.getFechaHasta());
        // Validar los IDs opcionales proporcionados en el filtro
        validarIdOpcional(filtroSeguro.getIdHoja(), "La hoja");
        // Obtener los contenedores desde el repositorio según los filtros y mapearlos a DTOs
        return hojaContenedorRepository.buscarParaReporte(
                        idBodega,
                        inicioDelDia(filtroSeguro.getFechaDesde()),
                        finDelDia(filtroSeguro.getFechaHasta()),
                        filtroSeguro.getIdHoja(),
                        normalizarTexto(filtroSeguro.getCodigoContenedor()))
                .stream()
                .map(this::mapearContenedor)
                .toList();
    }
    // Método para buscar inventario de conjuntos según los filtros proporcionados
    public List<ReporteConjuntoDTO> buscarInventario(Integer idUsuario, Integer idBodega, ReporteConjuntoFiltroDTO filtro) {
        // Validar el acceso a los reportes para el usuario y la bodega
        validarAccesoReportes(idUsuario, idBodega);
        // Si el filtro es nulo, se crea un filtro seguro con valores predeterminados
        ReporteConjuntoFiltroDTO filtroSeguro = filtro == null
                ? new ReporteConjuntoFiltroDTO()
                : filtro;
        // Validar los IDs opcionales proporcionados en el filtro
        validarIdOpcional(filtroSeguro.getIdProducto(), "El producto");
        validarIdOpcional(filtroSeguro.getIdEstadoConjunto(), "El estado del conjunto");
        // Obtener los conjuntos desde el repositorio según los filtros y mapearlos a DTOs
        return conjuntoRepository.buscarParaReporte(
                        idBodega,
                        filtroSeguro.getIdProducto(),
                        filtroSeguro.getIdEstadoConjunto(),
                        normalizarTexto(filtroSeguro.getCodigoConjunto()))
                .stream()
                .map(this::mapearConjunto)
                .toList();
    }
    // Método para obtener el reporte de operación de una hoja de producción
    public ReporteOperacionHojaDTO obtenerSurtido(Integer idUsuario, Integer idBodega, Integer idHoja) {
        return obtenerOperacion(idUsuario, idBodega, idHoja, TIPO_SALIDA);
    }
    // Método para obtener el reporte de recepción de una hoja de producción
    public ReporteOperacionHojaDTO obtenerRecepcion(Integer idUsuario, Integer idBodega, Integer idHoja) {
        return obtenerOperacion(idUsuario, idBodega, idHoja, TIPO_ENTRADA);
    }
    // Método para obtener el detalle de una hoja de producción
    public ReporteHojaDetalleDTO obtenerHoja(Integer idUsuario, Integer idBodega, Integer idHoja) {
        // Validar el acceso a los reportes para el usuario y la bodega
        validarAccesoReportes(idUsuario, idBodega);
        // Obtener la hoja de producción autorizada y sus detalles
        HojaProduccionEntity hoja = obtenerHojaAutorizada(idHoja, idBodega);
        List<DetalleHojaEntity> detalles = detalleHojaRepository
                .findByHojaProduccionEntity_IdHoja(idHoja);
        // Calcular los totales de solicitado, surtido y devuelto
        int solicitado = totalSolicitado(detalles);
        int surtido = totalSurtido(detalles);
        int devuelto = totalDevuelto(detalles);
        // Construir y retornar el DTO del reporte de detalle de la hoja de producción
        return ReporteHojaDetalleDTO.builder()
                .idHoja(hoja.getIdHoja())
                .nombreProyecto(hoja.getNombreProyecto())
                .cliente(hoja.getCliente())
                .estado(hoja.getEstadoHoja() == null ? null : hoja.getEstadoHoja().getNombre())
                .fechaSalida(hoja.getFechaSalida())
                .fechaEstimadaRegreso(hoja.getFechaEstimadaRegreso())
                .totalSolicitado(solicitado)
                .totalSurtido(surtido)
                .totalDevuelto(devuelto)
                .porcentajeSurtido(porcentaje(surtido, solicitado))
                .porcentajeRecepcion(porcentaje(devuelto, surtido))
                .detalles(detalles.stream().map(this::mapearDetalleHoja).toList())
                .build();
    }
    // Método privado para obtener el reporte de operación de una hoja de producción según el tipo de movimiento
    private ReporteOperacionHojaDTO obtenerOperacion(Integer idUsuario, Integer idBodega, Integer idHoja, int tipoMovimiento) {
        // Validar el acceso a los reportes para el usuario y la bodega
        validarAccesoReportes(idUsuario, idBodega);
        // Obtener la hoja de producción autorizada y sus detalles, movimientos y contenedores
        HojaProduccionEntity hoja = obtenerHojaAutorizada(idHoja, idBodega);
        List<DetalleHojaEntity> detalles = detalleHojaRepository
                .findByHojaProduccionEntity_IdHoja(idHoja);
        List<MovimientoEntity> movimientos = movimientoRepository
                .findByHojaProduccion_IdHojaAndTipoMovimiento_IdTipoMovimientoOrderByFechaDescIdMovimientoDesc(
                        idHoja, tipoMovimiento);
        List<HojaContenedorEntity> contenedores = hojaContenedorRepository
                .findByHojaProduccion_IdHojaOrderByFechaAsignacionAscIdHojaContenedorAsc(idHoja);
        // Obtener las fechas de inicio y fin de la operación según el tipo de movimiento
        LocalDateTime inicio = movimientoRepository
                .findFirstByHojaProduccion_IdHojaAndTipoMovimiento_IdTipoMovimientoOrderByFechaAscIdMovimientoAsc(
                        idHoja, tipoMovimiento)
                .map(MovimientoEntity::getFecha)
                .orElse(null);
        LocalDateTime fin = movimientoRepository
                .findFirstByHojaProduccion_IdHojaAndTipoMovimiento_IdTipoMovimientoOrderByFechaDescIdMovimientoDesc(
                        idHoja, tipoMovimiento)
                .map(MovimientoEntity::getFecha)
                .orElse(null);
        // Construir y retornar el DTO del reporte de operación de la hoja de producción
        return ReporteOperacionHojaDTO.builder()
                .idHoja(hoja.getIdHoja())
                .nombreProyecto(hoja.getNombreProyecto())
                .cliente(hoja.getCliente())
                .estado(hoja.getEstadoHoja() == null ? null : hoja.getEstadoHoja().getNombre())
                .fechaSalida(hoja.getFechaSalida())
                .fechaEstimadaRegreso(hoja.getFechaEstimadaRegreso())
                .inicioOperacion(inicio)
                .finOperacion(fin)
                .totalSolicitado(totalSolicitado(detalles))
                .totalSurtido(totalSurtido(detalles))
                .totalDevuelto(totalDevuelto(detalles))
                .detalles(detalles.stream().map(this::mapearDetalle).toList())
                .movimientos(movimientos.stream().map(this::mapearMovimiento).toList())
                .contenedores(contenedores.stream().map(this::mapearContenedor).toList())
                .build();
    }
    // Método privado para obtener una hoja de producción autorizada según su ID y la bodega
    private HojaProduccionEntity obtenerHojaAutorizada(Integer idHoja, Integer idBodega) {
        // Validar que el ID de la hoja no sea nulo o inválido
        validarId(idHoja, "La hoja es obligatoria.");
        // Obtener la hoja de producción desde el repositorio y lanzar una excepción si no existe
        HojaProduccionEntity hoja = hojaProduccionRepository.findById(idHoja)
                .orElseThrow(() -> new ReporteRecursoNoEncontradoException(
                        "La hoja de producción no existe."));
        if (hoja.getBodega() == null || !idBodega.equals(hoja.getBodega().getIdBodega())) {
            throw new ReporteRecursoNoEncontradoException(
                    "La hoja no pertenece a la bodega activa.");
        }
        return hoja;
    }
    // Método privado para mapear una entidad de hoja de producción a un DTO de reporte
    private ReporteHojaDTO mapearHoja(HojaProduccionEntity hoja) {
        // Obtener los detalles de la hoja de producción y calcular los totales de solicitado, surtido y devuelto
        List<DetalleHojaEntity> detalles = detalleHojaRepository
                .findByHojaProduccionEntity_IdHoja(hoja.getIdHoja());
        int solicitado = totalSolicitado(detalles);
        int surtido = totalSurtido(detalles);
        int devuelto = totalDevuelto(detalles);
        // Construir y retornar el DTO del reporte de la hoja de producción
        return ReporteHojaDTO.builder()
                .idHoja(hoja.getIdHoja())
                .nombreProyecto(hoja.getNombreProyecto())
                .cliente(hoja.getCliente())
                .idEstado(hoja.getEstadoHoja() == null ? null : hoja.getEstadoHoja().getIdEstadoHoja())
                .estado(hoja.getEstadoHoja() == null ? null : hoja.getEstadoHoja().getNombre())
                .fechaSalida(hoja.getFechaSalida())
                .fechaEstimadaRegreso(hoja.getFechaEstimadaRegreso())
                .totalSolicitado(solicitado)
                .totalSurtido(surtido)
                .totalDevuelto(devuelto)
                .porcentajeSurtido(porcentaje(surtido, solicitado))
                .porcentajeRecepcion(porcentaje(devuelto, surtido))
                .build();
    }
    // Método privado para mapear una entidad de movimiento a un DTO de reporte
    private ReporteMovimientoDTO mapearMovimiento(MovimientoEntity movimiento) {
        // Obtener el conjunto asociado al movimiento
        ConjuntoEntity conjunto = movimiento.getConjunto();
        // Construir y retornar el DTO del reporte de movimiento
        return ReporteMovimientoDTO.builder()
                .idMovimiento(movimiento.getIdMovimiento())
                .fecha(movimiento.getFecha())
                .idTipoMovimiento(movimiento.getTipoMovimiento().getIdTipoMovimiento())
                .tipoMovimiento(movimiento.getTipoMovimiento().getNombre())
                .idHoja(movimiento.getHojaProduccion().getIdHoja())
                .proyecto(movimiento.getHojaProduccion().getNombreProyecto())
                .codigoConjunto(conjunto.getIdConjunto())
                .idProducto(conjunto.getProducto().getIdProducto())
                .producto(conjunto.getProducto().getNombre())
                .codigoContenedor(movimiento.getHojaContenedor() == null
                        ? null
                        : movimiento.getHojaContenedor().getContenedor().getCodigo())
                .idUsuario(movimiento.getUsuario().getIdUsuario())
                .usuario(movimiento.getUsuario().getNombre())
                .observaciones(movimiento.getObservaciones())
                .build();
    }
    // Método privado para mapear una entidad de hoja de contenedor a un DTO de reporte
    private ReporteContenedorDTO mapearContenedor(HojaContenedorEntity hojaContenedor) {
        // Obtener la capacidad del contenedor y la cantidad de salidas asociadas a la hoja de contenedor
        int capacidad = valor(hojaContenedor.getContenedor().getCapacidad());
        long cantidadSalidas = movimientoRepository
                .countByHojaContenedor_IdHojaContenedorAndTipoMovimiento_IdTipoMovimiento(
                        hojaContenedor.getIdHojaContenedor(), TIPO_SALIDA);
        // Construir y retornar el DTO del reporte de contenedor
        return ReporteContenedorDTO.builder()
                .idHojaContenedor(hojaContenedor.getIdHojaContenedor())
                .idContenedor(hojaContenedor.getContenedor().getIdContenedor())
                .codigoContenedor(hojaContenedor.getContenedor().getCodigo())
                .capacidad(capacidad)
                .idHoja(hojaContenedor.getHojaProduccion().getIdHoja())
                .proyecto(hojaContenedor.getHojaProduccion().getNombreProyecto())
                .fechaAsignacion(hojaContenedor.getFechaAsignacion())
                .usuarioAsignacion(nombre(hojaContenedor.getUsuarioAsignacion()))
                .fechaCierreCarga(hojaContenedor.getFechaCierreCarga())
                .usuarioCierre(nombre(hojaContenedor.getUsuarioCierre()))
                .fechaLiberacion(hojaContenedor.getFechaLiberacion())
                .usuarioLiberacion(nombre(hojaContenedor.getUsuarioLiberacion()))
                .cantidadSalidas(Math.toIntExact(cantidadSalidas))
                .porcentajeUtilizacion(porcentajeUtilizacion(cantidadSalidas, capacidad))
                .build();
    }
    // Método privado para mapear una entidad de conjunto a un DTO de reporte
    private ReporteConjuntoDTO mapearConjunto(ConjuntoEntity conjunto) {
        return ReporteConjuntoDTO.builder()
                .codigoConjunto(conjunto.getIdConjunto())
                .idProducto(conjunto.getProducto().getIdProducto())
                .producto(conjunto.getProducto().getNombre())
                .idEstado(conjunto.getEstadoConjunto().getIdEstadoConjunto())
                .estado(conjunto.getEstadoConjunto().getNombre())
                .fechaAlta(conjunto.getFechaAlta())
                .observaciones(conjunto.getObservaciones())
                .build();
    }
    // Método privado para mapear una entidad de detalle de hoja a un DTO de reporte
    private ReporteOperacionDetalleDTO mapearDetalle(DetalleHojaEntity detalle) {
        return ReporteOperacionDetalleDTO.builder()
                .idDetalle(detalle.getIdDetalle())
                .idProducto(detalle.getProducto().getIdProducto())
                .producto(detalle.getProducto().getNombre())
                .cantidadSolicitada(valor(detalle.getCantidadSolicitada()))
                .cantidadSurtida(valor(detalle.getCantidadSurtida()))
                .cantidadDevuelta(valor(detalle.getCantidadDevuelta()))
                .build();
    }
    // Método privado para mapear una entidad de detalle de hoja a un DTO de reporte de detalle de hoja
    private ReporteHojaDetalleDTO.Detalle mapearDetalleHoja(DetalleHojaEntity detalle) {
        // Calcular las cantidades solicitadas, surtidas y devueltas, asegurando que sean valores enteros válidos
        int solicitada = valor(detalle.getCantidadSolicitada());
        int surtida = valor(detalle.getCantidadSurtida());
        int devuelta = valor(detalle.getCantidadDevuelta());
        // Construir y retornar el DTO del detalle de la hoja de producción
        return ReporteHojaDetalleDTO.Detalle.builder()
                .idProducto(detalle.getProducto().getIdProducto())
                .producto(detalle.getProducto().getNombre())
                .cantidadSolicitada(solicitada)
                .cantidadSurtida(surtida)
                .cantidadDevuelta(devuelta)
                .pendienteSurtir(Math.max(solicitada - surtida, 0))
                .pendienteDevolver(Math.max(surtida - devuelta, 0))
                .build();
    }
    // Métodos privados para calcular totales y porcentajes
    private int totalSolicitado(List<DetalleHojaEntity> detalles) {
        return detalles.stream().mapToInt(detalle -> valor(detalle.getCantidadSolicitada())).sum();
    }
    // Método privado para calcular el total surtido
    private int totalSurtido(List<DetalleHojaEntity> detalles) {
        return detalles.stream().mapToInt(detalle -> valor(detalle.getCantidadSurtida())).sum();
    }

    // Método privado para calcular el total devuelto
    private int totalDevuelto(List<DetalleHojaEntity> detalles) {
        return detalles.stream().mapToInt(detalle -> valor(detalle.getCantidadDevuelta())).sum();
    }

    // Método privado para calcular el porcentaje
    private int porcentaje(int valor, int total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min(((long) valor * 100) / total, 100);
    }

    // Método privado para calcular el porcentaje de utilización
    private int porcentajeUtilizacion(long cantidadSalidas, int capacidad) {
        if (capacidad <= 0) {
            return 0;
        }
        return Math.toIntExact((cantidadSalidas * 100) / capacidad);
    }

    // Método privado para obtener el nombre de un usuario
    private String nombre(UsuarioEntity usuario) {
        return usuario == null ? null : usuario.getNombre();
    }

    // Método privado para obtener el valor de un entero, devolviendo 0 si es nulo
    private int valor(Integer valor) {
        return valor == null ? 0 : valor;
    }

    // Método privado para normalizar un texto, devolviendo null si está vacío o es nulo
    private String normalizarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String normalizado = texto.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }

    // Método privado para validar el rango de fechas
    private void validarRango(java.time.LocalDate fechaDesde, java.time.LocalDate fechaHasta) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fechaDesde no puede ser posterior a fechaHasta.");
        }
    }

    // Método privado para obtener el inicio del día de una fecha
    private LocalDateTime inicioDelDia(java.time.LocalDate fecha) {
        return fecha == null ? null : fecha.atStartOfDay();
    }

    // Método privado para obtener el fin del día de una fecha
    private LocalDateTime finDelDia(java.time.LocalDate fecha) {
        return fecha == null ? null : fecha.atTime(LocalTime.MAX);
    }

    // Método privado para validar un ID
    private void validarId(Integer id, String mensaje) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    // Método privado para validar un ID opcional
    private void validarIdOpcional(Integer id, String nombre) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException(nombre + " debe ser positivo.");
        }
    }
}

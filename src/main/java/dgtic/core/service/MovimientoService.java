package dgtic.core.service;

import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.MovimientoDTO;
import dgtic.core.model.dto.MovimientoEntradaDTO;
import dgtic.core.model.dto.MovimientoSalidaDTO;
import dgtic.core.model.entity.ConjuntoEntity;
import dgtic.core.model.entity.DetalleHojaEntity;
import dgtic.core.model.entity.EstadoConjuntoEntity;
import dgtic.core.model.entity.HojaContenedorEntity;
import dgtic.core.model.entity.HojaProduccionEntity;
import dgtic.core.model.entity.MovimientoEntity;
import dgtic.core.model.entity.TipoMovimientoEntity;
import dgtic.core.model.entity.UsuarioEntity;
import dgtic.core.repository.ConjuntoRepository;
import dgtic.core.repository.DetalleHojaRepository;
import dgtic.core.repository.EstadoConjuntoRepository;
import dgtic.core.repository.HojaContenedorRepository;
import dgtic.core.repository.HojaProduccionRepository;
import dgtic.core.repository.MovimientoRepository;
import dgtic.core.repository.TipoMovimientoRepository;
import dgtic.core.repository.UsuarioBodegaRepository;
import dgtic.core.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimientoService {
    private static final int TIPO_SALIDA = 1;
    private static final int TIPO_ENTRADA = 2;
    private static final int ESTADO_DISPONIBLE = 1;
    private static final int ESTADO_SURTIDO = 2;
    private static final int ESTADO_HOJA_POR_SURTIR = 2;
    private static final int ESTADO_HOJA_SURTIENDO = 3;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private TipoMovimientoRepository tipoMovimientoRepository;

    @Autowired
    private HojaProduccionRepository hojaProduccionRepository;

    @Autowired
    private DetalleHojaRepository detalleHojaRepository;

    @Autowired
    private EstadoConjuntoRepository estadoConjuntoRepository;

    @Autowired
    private ConjuntoRepository conjuntoRepository;

    @Autowired
    private HojaContenedorRepository hojaContenedorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioBodegaRepository usuarioBodegaRepository;

    // Registrar una salida de conjunto
    @Transactional
    public MovimientoDTO registrarSalida(MovimientoSalidaDTO dto, Integer idUsuario, Integer idBodega) {
        // Validar que el DTO no sea nulo
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la salida son obligatorios.");
        }
        // Obtener el usuario autorizado para la bodega
        UsuarioEntity usuario = obtenerUsuarioAutorizado(idUsuario, idBodega);
        // Obtener la hoja de producción bloqueada para actualización
        HojaProduccionEntity hoja = obtenerHojaBloqueada(dto.getIdHoja(), idBodega);
        // Validar que la hoja esté en estado POR_SURTIR o SURTIENDO
        Integer idEstadoHoja = hoja.getEstadoHoja() == null
                ? null
                : hoja.getEstadoHoja().getIdEstadoHoja();
        if (!Integer.valueOf(ESTADO_HOJA_POR_SURTIR).equals(idEstadoHoja)
                && !Integer.valueOf(ESTADO_HOJA_SURTIENDO).equals(idEstadoHoja)) {
            throw new IllegalArgumentException(
                    "La hoja no está disponible para registrar salidas.");
        }
        // Obtener el detalle de hoja bloqueado para actualización
        DetalleHojaEntity detalle = detalleHojaRepository.findByIdForUpdate(dto.getIdDetalle())
                .orElseThrow(() -> new IllegalArgumentException("El detalle de hoja no existe."));
        // Validar que el detalle pertenezca a la hoja
        validarDetalleDeHoja(detalle, hoja);
        // Obtener el conjunto bloqueado para actualización
        ConjuntoEntity conjunto = conjuntoRepository.findByIdForUpdate(dto.getIdConjunto())
                .orElseThrow(() -> new IllegalArgumentException("El conjunto no existe."));
        // Validar que el conjunto pertenezca a la hoja y al detalle
        validarConjuntoDeHoja(conjunto, hoja, detalle);
        // Validar que el conjunto esté disponible para salida
        validarEstadoConjunto(conjunto, ESTADO_DISPONIBLE,
                "El conjunto no está disponible para salida.");
        // Obtener la asignación HojaContenedor bloqueada para actualización
        HojaContenedorEntity hojaContenedor = hojaContenedorRepository.findByIdForUpdate(dto.getIdHojaContenedor())
                .orElseThrow(() -> new IllegalArgumentException("La asignación HojaContenedor no existe."));
        // Validar que la hojaContenedor esté abierta
        validarHojaContenedorAbierta(hojaContenedor, hoja);

        // Validar que la capacidad del contenedor no haya sido alcanzada
        long ocupacion = movimientoRepository.countByHojaContenedor_IdHojaContenedorAndTipoMovimiento_IdTipoMovimiento(
                hojaContenedor.getIdHojaContenedor(), TIPO_SALIDA);
        if (ocupacion >= hojaContenedor.getContenedor().getCapacidad()) {
            throw new IllegalArgumentException("La capacidad del contenedor ya fue alcanzada.");
        }
        // Validar que el conjunto no tenga una salida pendiente para esta hoja
        if (movimientoRepository.existsSalidaPendiente(
                hoja.getIdHoja(), conjunto.getIdConjunto(), TIPO_SALIDA, TIPO_ENTRADA)) {
            throw new IllegalArgumentException("El conjunto ya tiene una salida pendiente para esta hoja.");
        }
        // Validar que la cantidad solicitada no haya sido surtida
        int cantidadSurtida = detalle.getCantidadSurtida() == null ? 0 : detalle.getCantidadSurtida();
        if (cantidadSurtida >= detalle.getCantidadSolicitada()) {
            throw new IllegalArgumentException("La cantidad solicitada ya fue surtida.");
        }

        // Crear el movimiento de salida y actualizar el detalle y el conjunto
        TipoMovimientoEntity tipo = obtenerTipo(TIPO_SALIDA);
        MovimientoEntity movimiento = Mapper.toMovimientoSalidaEntity(
                dto, tipo, conjunto, hoja, detalle, hojaContenedor, usuario,
                LocalDateTime.now());
        // Actualizar la cantidad surtida en el detalle y el estado del conjunto
        detalle.setCantidadSurtida(cantidadSurtida + 1);
        // Cambiar el estado del conjunto a surtido
        conjunto.setEstadoConjunto(obtenerEstadoConjunto(ESTADO_SURTIDO));
        // Guardar los cambios en el detalle y el conjunto
        detalleHojaRepository.save(detalle);
        conjuntoRepository.save(conjunto);
        return Mapper.toMovimientoDTO(movimientoRepository.save(movimiento));
    }
    // Registrar una entrada de conjunto
    @Transactional
    public MovimientoDTO registrarEntrada(MovimientoEntradaDTO dto, Integer idUsuario, Integer idBodega) {
        // Validar que el DTO no sea nulo
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la entrada son obligatorios.");
        }
        // Obtener el usuario autorizado para la bodega
        UsuarioEntity usuario = obtenerUsuarioAutorizado(idUsuario, idBodega);
        // Obtener la hoja de producción bloqueada para actualización
        HojaProduccionEntity hoja = obtenerHojaBloqueada(dto.getIdHoja(), idBodega);
        // Obtener el conjunto bloqueado para actualización
        ConjuntoEntity conjunto = conjuntoRepository.findByIdForUpdate(dto.getIdConjunto())
                .orElseThrow(() -> new IllegalArgumentException("El conjunto no existe."));
        // Validar que el conjunto pertenezca a la bodega activa
        if (!perteneceBodega(conjunto, idBodega)) {
            throw new IllegalArgumentException("El conjunto no pertenece a la bodega activa.");
        }
        // Validar que el conjunto esté surtido para entrada
        validarEstadoConjunto(conjunto, ESTADO_SURTIDO,
                "El conjunto no está surtido para entrada.");
        // Validar que el conjunto tenga una salida pendiente para esta hoja
        if (!movimientoRepository.existsSalidaPendiente(
                hoja.getIdHoja(), conjunto.getIdConjunto(), TIPO_SALIDA, TIPO_ENTRADA)) {
            throw new IllegalArgumentException("El conjunto no tiene una salida pendiente para esta hoja.");
        }
        // Obtener el detalle de hoja correspondiente al producto del conjunto
        DetalleHojaEntity detalle = detalleHojaRepository
                .findByHojaProduccionEntity_IdHojaAndProducto_IdProducto(
                        hoja.getIdHoja(), conjunto.getProducto().getIdProducto())
                .orElseThrow(() -> new IllegalArgumentException("No existe detalle para el producto del conjunto."));
        // Validar cantidad surtida y devuelta
        int cantidadSurtida = detalle.getCantidadSurtida() == null ? 0 : detalle.getCantidadSurtida();
        int cantidadDevuelta = detalle.getCantidadDevuelta() == null ? 0 : detalle.getCantidadDevuelta();
        if (cantidadDevuelta >= cantidadSurtida) {
            throw new IllegalArgumentException("La cantidad surtida ya fue devuelta.");
        }

        // Obtener la asignación HojaContenedor si se proporciona
        HojaContenedorEntity hojaContenedor = null;
        if (dto.getIdHojaContenedor() != null) {
            hojaContenedor = hojaContenedorRepository.findByIdForUpdate(dto.getIdHojaContenedor())
                    .orElseThrow(() -> new IllegalArgumentException("La asignación HojaContenedor no existe."));
            if (!perteneceHoja(hojaContenedor, hoja)) {
                throw new IllegalArgumentException("La HojaContenedor no pertenece a la hoja indicada.");
            }
        }
        // Crear el movimiento de entrada y actualizar el detalle y el conjunto
        MovimientoEntity movimiento = Mapper.toMovimientoEntradaEntity(
                dto, obtenerTipo(TIPO_ENTRADA), conjunto, hoja, detalle, hojaContenedor, usuario,
                LocalDateTime.now());
        // Actualizar la cantidad devuelta en el detalle y el estado del conjunto
        detalle.setCantidadDevuelta(cantidadDevuelta + 1);
        conjunto.setEstadoConjunto(obtenerEstadoConjunto(ESTADO_DISPONIBLE));
        // Guardar los cambios en el detalle y el conjunto
        detalleHojaRepository.save(detalle);
        conjuntoRepository.save(conjunto);
        return Mapper.toMovimientoDTO(movimientoRepository.save(movimiento));
    }
    // Obtener todos los movimientos de una hoja de producción
    @Transactional(readOnly = true)
    public List<MovimientoDTO> getByHoja(Integer idHoja, Integer idBodega) {
        // Validar que la hoja de producción exista
        HojaProduccionEntity hoja = hojaProduccionRepository.findById(idHoja)
                .orElseThrow(() -> new IllegalArgumentException("La hoja de producción no existe."));
        // Validar que la hoja pertenezca a la bodega activa
        validarBodega(hoja, idBodega);
        // Obtener los movimientos de la hoja de producción y mapearlos a DTOs
        return movimientoRepository.findByHojaProduccion_IdHojaOrderByFechaAscIdMovimientoAsc(idHoja)
                .stream().map(Mapper::toMovimientoDTO).toList();
    }
    // Obtener un movimiento por su ID
    private HojaProduccionEntity obtenerHojaBloqueada(Integer idHoja, Integer idBodega) {
        // Validar que la hoja no sea nula o menor o igual a cero
        if (idHoja == null || idHoja <= 0) {
            throw new IllegalArgumentException("La hoja de producción es obligatoria.");
        }
        // Obtener la hoja de producción bloqueada para actualización
        HojaProduccionEntity hoja = hojaProduccionRepository.findByIdForUpdate(idHoja)
                .orElseThrow(() -> new IllegalArgumentException("La hoja de producción no existe."));
        // Validar que la hoja pertenezca a la bodega activa
        validarBodega(hoja, idBodega);
        return hoja;
    }
    // Obtener el tipo de movimiento por su ID
    private TipoMovimientoEntity obtenerTipo(int idTipo) {
        return tipoMovimientoRepository.findById(idTipo)
                .orElseThrow(() -> new IllegalStateException("El tipo de movimiento no existe."));
    }

    // Obtener el usuario autorizado para la bodega
    private UsuarioEntity obtenerUsuarioAutorizado(Integer idUsuario, Integer idBodega) {
        // Validar que el usuario y la bodega no sean nulos
        if (idUsuario == null || idBodega == null) {
            throw new IllegalArgumentException("El usuario y la bodega son obligatorios.");
        }
        // Obtener el usuario por su ID
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
        // Validar que el usuario esté activo
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new IllegalArgumentException("El usuario está inactivo.");
        }
        // Validar que el usuario esté autorizado para la bodega
        if (!usuarioBodegaRepository.existsByUsuario_IdUsuarioAndBodega_IdBodega(idUsuario, idBodega)) {
            throw new IllegalArgumentException("El usuario no está autorizado para la bodega.");
        }
        return usuario;
    }
    // Validar que el detalle de hoja pertenezca a la hoja de producción
    private void validarDetalleDeHoja(DetalleHojaEntity detalle, HojaProduccionEntity hoja) {
        // Validar que el detalle de hoja pertenezca a la hoja de producción
        if (detalle.getHojaProduccionEntity() == null
                || !hoja.getIdHoja().equals(detalle.getHojaProduccionEntity().getIdHoja())) {
            throw new IllegalArgumentException("El detalle no pertenece a la hoja indicada.");
        }
    }
    // Validar que el conjunto pertenezca a la hoja de producción y al detalle de hoja
    private void validarConjuntoDeHoja(
            ConjuntoEntity conjunto,
            HojaProduccionEntity hoja,
            DetalleHojaEntity detalle) {
        // Validar que el conjunto pertenezca a la bodega de la hoja
        if (!perteneceBodega(conjunto, hoja.getBodega().getIdBodega())) {
            throw new IllegalArgumentException("El conjunto no pertenece a la bodega de la hoja.");
        }
        // Validar que el producto del conjunto coincida con el producto del detalle
        if (conjunto.getProducto() == null || detalle.getProducto() == null
                || !conjunto.getProducto().getIdProducto().equals(detalle.getProducto().getIdProducto())) {
            throw new IllegalArgumentException("El producto del conjunto no coincide con el detalle.");
        }
    }

    // Validar que la hoja contenedor esté abierta
    private void validarHojaContenedorAbierta(HojaContenedorEntity hojaContenedor, HojaProduccionEntity hoja) {
        // Validar que la hojaContenedor pertenezca a la hoja de producción
        if (!perteneceHoja(hojaContenedor, hoja)) {
            throw new IllegalArgumentException("La HojaContenedor no pertenece a la hoja indicada.");
        }
        // Validar que la hoja contenedor no esté cerrada ni liberada
        if (hojaContenedor.getFechaCierreCarga() != null || hojaContenedor.getFechaLiberacion() != null) {
            throw new IllegalArgumentException("La carga del contenedor no está abierta.");
        }
    }
    // Validar que la hoja contenedor pertenezca a la hoja de producción
    private boolean perteneceHoja(HojaContenedorEntity hojaContenedor, HojaProduccionEntity hoja) {
        // Validar que la hojaContenedor y la hoja de producción no sean nulas y que sus IDs coincidan
        return hojaContenedor != null && hojaContenedor.getHojaProduccion() != null
                && hoja.getIdHoja().equals(hojaContenedor.getHojaProduccion().getIdHoja());
    }
    // Validar que el conjunto pertenezca a la bodega activa
    private boolean perteneceBodega(ConjuntoEntity conjunto, Integer idBodega) {
        // Validar que el conjunto, su producto y la bodega del producto no sean nulos y que el ID de la bodega coincida
        return conjunto != null && conjunto.getProducto() != null
                && conjunto.getProducto().getBodega() != null
                && idBodega.equals(conjunto.getProducto().getBodega().getIdBodega());
    }
    // Validar que la hoja de producción pertenezca a la bodega activa
    private void validarBodega(HojaProduccionEntity hoja, Integer idBodega) {
        // Validar que la hoja de producción y su bodega no sean nulas y que el ID de la bodega coincida
        if (idBodega == null || hoja.getBodega() == null
                || !idBodega.equals(hoja.getBodega().getIdBodega())) {
            throw new IllegalArgumentException("La hoja no pertenece a la bodega activa.");
        }
    }
    // Validar que el conjunto tenga el estado esperado
    private void validarEstadoConjunto(ConjuntoEntity conjunto, int estadoEsperado, String mensaje) {
        // Validar que el conjunto y su estado no sean nulos y que el ID del estado coincida con el estado esperado
        if (conjunto.getEstadoConjunto() == null
                || !Integer.valueOf(estadoEsperado).equals(
                conjunto.getEstadoConjunto().getIdEstadoConjunto())) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    // Obtener el estado del conjunto por su ID
    private EstadoConjuntoEntity obtenerEstadoConjunto(int idEstado) {
        return estadoConjuntoRepository.findById(idEstado)
                .orElseThrow(() -> new IllegalStateException("El estado del conjunto no existe."));
    }
}

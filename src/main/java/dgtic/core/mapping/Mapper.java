package dgtic.core.mapping;

import dgtic.core.model.dto.*;
import dgtic.core.model.entity.*;

import java.time.LocalDateTime;

public class Mapper {
    // UsuarioEntity to UsuarioDTO
    public static UsuarioDTO toUsuarioDTO(UsuarioEntity usuarioEntity){
        return UsuarioDTO.builder()
                .idUsuario(usuarioEntity.getIdUsuario())
                .nombre(usuarioEntity.getNombre())
                .username(usuarioEntity.getUsername())
                .activo(usuarioEntity.getActivo())
                .rol(RolDTO.builder()
                        .idRol(usuarioEntity.getRol().getIdRol())
                        .nombre(usuarioEntity.getRol().getNombre()).build())
                .build();
    }

    // SesionEntity to SesionDTO
    public static SesionDTO toSesionDTO(SesionEntity sesionEntity){
        return SesionDTO.builder()
                .idSesion(sesionEntity.getIdSesion())
                .token(sesionEntity.getToken())
                .fechaInicio(sesionEntity.getFechaInicio())
                .fechaFin(sesionEntity.getFechaFin())
                .activa(sesionEntity.getActiva())
                .usuario(toUsuarioDTO(sesionEntity.getUsuario()))
                .bodega(BodegaDTO.builder()
                        .idBodega(sesionEntity.getBodega().getIdBodega())
                        .nombre(sesionEntity.getBodega().getNombre())
                        .build())
                .build();
    }

    // ProductoEntity to ProductoDTO
    public static ProductoDTO toProductoDTO(ProductoEntity productoEntity){
        return ProductoDTO.builder()
                .idProducto(productoEntity.getIdProducto())
                .nombre(productoEntity.getNombre())
                .descripcion(productoEntity.getDescripcion())
                .activo(productoEntity.getActivo())
                .urlImagen(productoEntity.getUrlImagen())
                .bodega(BodegaDTO.builder()
                        .idBodega(productoEntity.getBodega().getIdBodega())
                        .nombre(productoEntity.getBodega().getNombre())
                        .build())
                .build();
    }

    // ProductoDTO to ProductoEntity
    public static ProductoEntity toProductoEntity(ProductoDTO productoDTO) {
        return ProductoEntity.builder()
                .idProducto(productoDTO.getIdProducto())
                .nombre(productoDTO.getNombre())
                .descripcion(productoDTO.getDescripcion())
                .activo(productoDTO.getActivo())
                .urlImagen(productoDTO.getUrlImagen())
                .bodega(BodegaEntity.builder()
                        .idBodega(productoDTO.getBodega().getIdBodega())
                        .nombre(productoDTO.getBodega().getNombre())
                        .build())
                .build();
    }

    // BodegaEntity to BodegaDTO
    public static BodegaDTO toBodegaDTO(BodegaEntity bodegaEntity) {
        return BodegaDTO.builder()
                .idBodega(bodegaEntity.getIdBodega())
                .nombre(bodegaEntity.getNombre())
                .descripcion(bodegaEntity.getDescripcion())
                .build();
    }

    // BodegaDTO to BodegaEntity
    public static BodegaEntity toBodegaEntity(BodegaDTO bodegaDTO) {
        return BodegaEntity.builder()
                .idBodega(bodegaDTO.getIdBodega())
                .nombre(bodegaDTO.getNombre())
                .descripcion(bodegaDTO.getDescripcion())
                .build();
    }

    // HojaProduccionEntity to HojaProduccionDTO
    public static HojaProduccionDTO toHojaProduccionDTO(HojaProduccionEntity hojaProduccionEntity) {
        return HojaProduccionDTO.builder()
                .idHoja(hojaProduccionEntity.getIdHoja())
                .nombreProyecto(hojaProduccionEntity.getNombreProyecto())
                .cliente(hojaProduccionEntity.getCliente())
                .fechaSalida(hojaProduccionEntity.getFechaSalida())
                .fechaEstimadaRegreso(hojaProduccionEntity.getFechaEstimadaRegreso())
                .estadoHoja(EstadoHojaDTO.builder()
                        .idEstadoHoja(hojaProduccionEntity.getEstadoHoja().getIdEstadoHoja())
                        .nombre(hojaProduccionEntity.getEstadoHoja().getNombre())
                        .build())
                .bodega(BodegaDTO.builder()
                        .idBodega(hojaProduccionEntity.getBodega().getIdBodega())
                        .nombre(hojaProduccionEntity.getBodega().getNombre())
                        .build())
                .build();
    }

    // HojaProduccionDTO to HojaProduccionEntity
    public static HojaProduccionEntity toHojaProduccionEntity(HojaProduccionDTO hojaProduccionDTO) {
        return HojaProduccionEntity.builder()
                .idHoja(hojaProduccionDTO.getIdHoja())
                .nombreProyecto(hojaProduccionDTO.getNombreProyecto())
                .cliente(hojaProduccionDTO.getCliente())
                .fechaSalida(hojaProduccionDTO.getFechaSalida())
                .fechaEstimadaRegreso(hojaProduccionDTO.getFechaEstimadaRegreso())
                .estadoHoja(EstadoHojaEntity.builder()
                        .idEstadoHoja(hojaProduccionDTO.getEstadoHoja().getIdEstadoHoja())
                        .nombre(hojaProduccionDTO.getEstadoHoja().getNombre())
                        .build())
                .bodega(BodegaEntity.builder()
                        .idBodega(hojaProduccionDTO.getBodega().getIdBodega())
                        .nombre(hojaProduccionDTO.getBodega().getNombre())
                        .build())
                .build();

    }

    // DetalleHojaEntity to DetalleHojaDTO
    public static DetalleHojaDTO toDetalleHojaDTO(DetalleHojaEntity detalleHojaEntity) {
        return DetalleHojaDTO.builder()
                .idDetalleHoja(detalleHojaEntity.getIdDetalle())
                .hojaProduccion(toHojaProduccionDTO(detalleHojaEntity.getHojaProduccionEntity()))
                .producto(toProductoDTO(detalleHojaEntity.getProducto()))
                .cantidadSolicitada(detalleHojaEntity.getCantidadSolicitada())
                .cantidadSurtida(detalleHojaEntity.getCantidadSurtida())
                .cantidadDevuelta(detalleHojaEntity.getCantidadDevuelta())
                .build();
    }

    // DetalleHojaDTO to DetalleHojaEntity
    public static DetalleHojaEntity toDetalleHojaEntity(DetalleHojaDTO detalleHojaDTO) {
        return DetalleHojaEntity.builder()
                .idDetalle(detalleHojaDTO.getIdDetalleHoja())
                .hojaProduccionEntity(toHojaProduccionEntity(detalleHojaDTO.getHojaProduccion()))
                .producto(toProductoEntity(detalleHojaDTO.getProducto()))
                .cantidadSolicitada(detalleHojaDTO.getCantidadSolicitada())
                .cantidadSurtida(detalleHojaDTO.getCantidadSurtida())
                .cantidadDevuelta(detalleHojaDTO.getCantidadDevuelta())
                .build();
    }

    // EstadoHojaEntity to EstadoHojaDTO
    public static EstadoHojaDTO toEstadoHojaDTO(EstadoHojaEntity estadoHojaEntity) {
        return EstadoHojaDTO.builder()
                .idEstadoHoja(estadoHojaEntity.getIdEstadoHoja())
                .nombre(estadoHojaEntity.getNombre())
                .build();
    }

    // EstadoConjuntoEntity to EstadoConjuntoDTO
    public static EstadoConjuntoDTO toEstadoConjuntoDTO(EstadoConjuntoEntity estadoConjuntoEntity) {
        return EstadoConjuntoDTO.builder()
                .idEstadoConjunto(estadoConjuntoEntity.getIdEstadoConjunto())
                .nombre(estadoConjuntoEntity.getNombre())
                .build();
    }

    // ConjuntoEntity to ConjuntoDTO
    public static ConjuntoDTO toConjuntoDTO(ConjuntoEntity conjuntoEntity) {
        return ConjuntoDTO.builder()
                .idConjunto(conjuntoEntity.getIdConjunto())
                .producto(toProductoDTO(conjuntoEntity.getProducto()))
                .estadoConjunto(toEstadoConjuntoDTO(conjuntoEntity.getEstadoConjunto()))
                .fechaAlta(conjuntoEntity.getFechaAlta())
                .observaciones(conjuntoEntity.getObservaciones())
                .build();
    }

    // ConjuntoAltaDTO to ConjuntoEntity using server-controlled values
    public static ConjuntoEntity toConjuntoEntity(
            ConjuntoAltaDTO conjuntoAltaDTO,
            String idConjunto,
            LocalDateTime fechaAlta,
            ProductoEntity producto,
            EstadoConjuntoEntity estadoConjunto) {
        return ConjuntoEntity.builder()
                .idConjunto(idConjunto)
                .producto(producto)
                .estadoConjunto(estadoConjunto)
                .fechaAlta(fechaAlta)
                .observaciones(conjuntoAltaDTO.getObservaciones())
                .build();
    }

    // Apply an already validated administrative edit to an existing entity
    public static void actualizarConjunto(
            ConjuntoEntity conjuntoEntity,
            ConjuntoEdicionDTO conjuntoEdicionDTO,
            EstadoConjuntoEntity estadoConjunto) {
        conjuntoEntity.setEstadoConjunto(estadoConjunto);
        conjuntoEntity.setObservaciones(conjuntoEdicionDTO.getObservaciones());
    }

    // EstadoContenedorEntity to EstadoContenedorDTO
    public static EstadoContenedorDTO toEstadoContenedorDTO(EstadoContenedorEntity estadoContenedorEntity) {
        return EstadoContenedorDTO.builder()
                .idEstadoContenedor(estadoContenedorEntity.getIdEstadoContenedor())
                .nombre(estadoContenedorEntity.getNombre())
                .build();
    }

    // ContenedorEntity to ContenedorDTO
    public static ContenedorDTO toContenedorDTO(ContenedorEntity contenedorEntity) {
        return ContenedorDTO.builder()
                .idContenedor(contenedorEntity.getIdContenedor())
                .codigo(contenedorEntity.getCodigo())
                .capacidad(contenedorEntity.getCapacidad())
                .estadoContenedor(toEstadoContenedorDTO(contenedorEntity.getEstadoContenedor()))
                .fechaAlta(contenedorEntity.getFechaAlta())
                .observaciones(contenedorEntity.getObservaciones())
                .build();
    }

    // ContenedorAltaDTO to ContenedorEntity using server-controlled values
    public static ContenedorEntity toContenedorEntity(
            ContenedorAltaDTO contenedorAltaDTO,
            String codigo,
            LocalDateTime fechaAlta,
            EstadoContenedorEntity estadoContenedor) {
        return ContenedorEntity.builder()
                .codigo(codigo)
                .capacidad(contenedorAltaDTO.getCapacidad())
                .estadoContenedor(estadoContenedor)
                .fechaAlta(fechaAlta)
                .observaciones(contenedorAltaDTO.getObservaciones())
                .build();
    }

    // Apply an already validated administrative edit to an existing entity
    public static void actualizarContenedor(
            ContenedorEntity contenedorEntity,
            ContenedorEdicionDTO contenedorEdicionDTO,
            EstadoContenedorEntity estadoContenedor) {
        contenedorEntity.setCapacidad(contenedorEdicionDTO.getCapacidad());
        contenedorEntity.setEstadoContenedor(estadoContenedor);
        contenedorEntity.setObservaciones(contenedorEdicionDTO.getObservaciones());
    }

    // TipoMovimientoEntity to TipoMovimientoDTO
    public static TipoMovimientoDTO toTipoMovimientoDTO(TipoMovimientoEntity tipoMovimientoEntity) {
        return TipoMovimientoDTO.builder()
                .idTipoMovimiento(tipoMovimientoEntity.getIdTipoMovimiento())
                .nombre(tipoMovimientoEntity.getNombre())
                .build();
    }

    // HojaContenedorEntity to HojaContenedorDTO
    public static HojaContenedorDTO toHojaContenedorDTO(HojaContenedorEntity entity) {
        return HojaContenedorDTO.builder()
                .idHojaContenedor(entity.getIdHojaContenedor())
                .hojaProduccion(toHojaProduccionDTO(entity.getHojaProduccion()))
                .contenedor(toContenedorDTO(entity.getContenedor()))
                .fechaAsignacion(entity.getFechaAsignacion())
                .fechaCierreCarga(entity.getFechaCierreCarga())
                .fechaLiberacion(entity.getFechaLiberacion())
                .usuarioAsignacion(toUsuarioDTO(entity.getUsuarioAsignacion()))
                .usuarioCierre(entity.getUsuarioCierre() == null ? null : toUsuarioDTO(entity.getUsuarioCierre()))
                .usuarioLiberacion(entity.getUsuarioLiberacion() == null ? null : toUsuarioDTO(entity.getUsuarioLiberacion()))
                .observaciones(entity.getObservaciones())
                .build();
    }

    // HojaContenedorAsignacionDTO to HojaContenedorEntity using server-controlled values
    public static HojaContenedorEntity toHojaContenedorEntity(
            HojaContenedorAsignacionDTO dto,
            HojaProduccionEntity hojaProduccion,
            ContenedorEntity contenedor,
            UsuarioEntity usuarioAsignacion,
            LocalDateTime fechaAsignacion) {
        return HojaContenedorEntity.builder()
                .hojaProduccion(hojaProduccion)
                .contenedor(contenedor)
                .fechaAsignacion(fechaAsignacion)
                .usuarioAsignacion(usuarioAsignacion)
                .observaciones(dto.getObservaciones())
                .build();
    }

    // MovimientoEntity to MovimientoDTO
    public static MovimientoDTO toMovimientoDTO(MovimientoEntity entity) {
        return MovimientoDTO.builder()
                .idMovimiento(entity.getIdMovimiento())
                .tipoMovimiento(toTipoMovimientoDTO(entity.getTipoMovimiento()))
                .conjunto(toConjuntoDTO(entity.getConjunto()))
                .hojaProduccion(toHojaProduccionDTO(entity.getHojaProduccion()))
                .detalleHoja(entity.getDetalleHoja() == null ? null : toDetalleHojaDTO(entity.getDetalleHoja()))
                .hojaContenedor(entity.getHojaContenedor() == null ? null : toHojaContenedorDTO(entity.getHojaContenedor()))
                .usuario(toUsuarioDTO(entity.getUsuario()))
                .fecha(entity.getFecha())
                .observaciones(entity.getObservaciones())
                .build();
    }

    // DetalleHojaEntity to SurtidoDetalleDTO using service-calculated progress values
    public static SurtidoDetalleDTO toSurtidoDetalleDTO(
            DetalleHojaEntity entity,
            Integer cantidadSurtida,
            Integer cantidadPendiente,
            Integer porcentaje,
            Boolean completo) {
        return SurtidoDetalleDTO.builder()
                .idDetalle(entity.getIdDetalle())
                .idProducto(entity.getProducto().getIdProducto())
                .producto(entity.getProducto().getNombre())
                .cantidadSolicitada(entity.getCantidadSolicitada())
                .cantidadSurtida(cantidadSurtida)
                .cantidadPendiente(cantidadPendiente)
                .porcentaje(porcentaje)
                .completo(completo)
                .build();
    }

    // HojaContenedorEntity to SurtidoContenedorDTO using service-calculated occupancy values
    public static SurtidoContenedorDTO toSurtidoContenedorDTO(
            HojaContenedorEntity entity,
            Integer ocupacion,
            Integer porcentaje,
            Boolean disponible,
            Boolean lleno) {
        return SurtidoContenedorDTO.builder()
                .idHojaContenedor(entity.getIdHojaContenedor())
                .idContenedor(entity.getContenedor().getIdContenedor())
                .codigo(entity.getContenedor().getCodigo())
                .capacidad(entity.getContenedor().getCapacidad())
                .ocupacion(ocupacion)
                .disponible(disponible)
                .porcentaje(porcentaje)
                .cargaCerrada(entity.getFechaCierreCarga() != null)
                .lleno(lleno)
                .build();
    }

    // MovimientoEntity to the compact representation used by HH Surtido
    public static SurtidoMovimientoDTO toSurtidoMovimientoDTO(MovimientoEntity entity) {
        return SurtidoMovimientoDTO.builder()
                .idMovimiento(entity.getIdMovimiento())
                .codigoConjunto(entity.getConjunto().getIdConjunto())
                .producto(entity.getConjunto().getProducto().getNombre())
                .codigoContenedor(entity.getHojaContenedor() == null
                        ? null
                        : entity.getHojaContenedor().getContenedor().getCodigo())
                .fecha(entity.getFecha())
                .usuario(entity.getUsuario().getNombre())
                .build();
    }

    // MovimientoSalidaDTO to MovimientoEntity using server-controlled values
    public static MovimientoEntity toMovimientoSalidaEntity(
            MovimientoSalidaDTO dto,
            TipoMovimientoEntity tipoMovimiento,
            ConjuntoEntity conjunto,
            HojaProduccionEntity hojaProduccion,
            DetalleHojaEntity detalleHoja,
            HojaContenedorEntity hojaContenedor,
            UsuarioEntity usuario,
            LocalDateTime fecha) {
        return MovimientoEntity.builder()
                .tipoMovimiento(tipoMovimiento)
                .conjunto(conjunto)
                .hojaProduccion(hojaProduccion)
                .detalleHoja(detalleHoja)
                .hojaContenedor(hojaContenedor)
                .usuario(usuario)
                .fecha(fecha)
                .observaciones(dto.getObservaciones())
                .build();
    }

    // MovimientoEntradaDTO to MovimientoEntity using server-controlled values
    public static MovimientoEntity toMovimientoEntradaEntity(
            MovimientoEntradaDTO dto,
            TipoMovimientoEntity tipoMovimiento,
            ConjuntoEntity conjunto,
            HojaProduccionEntity hojaProduccion,
            DetalleHojaEntity detalleHoja,
            HojaContenedorEntity hojaContenedor,
            UsuarioEntity usuario,
            LocalDateTime fecha) {
        return MovimientoEntity.builder()
                .tipoMovimiento(tipoMovimiento)
                .conjunto(conjunto)
                .hojaProduccion(hojaProduccion)
                .detalleHoja(detalleHoja)
                .hojaContenedor(hojaContenedor)
                .usuario(usuario)
                .fecha(fecha)
                .observaciones(dto.getObservaciones())
                .build();
    }
}

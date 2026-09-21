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
}

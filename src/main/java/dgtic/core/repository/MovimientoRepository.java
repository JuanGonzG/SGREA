package dgtic.core.repository;

import dgtic.core.model.entity.MovimientoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovimientoRepository extends JpaRepository<MovimientoEntity, Integer> {
    // Buscar movimientos por idHoja ordenados por fecha ascendente y idMovimiento ascendente
    List<MovimientoEntity> findByHojaProduccion_IdHojaOrderByFechaAscIdMovimientoAsc(Integer idHoja);
    // Buscar los últimos cinco movimientos de una hoja para reconstruir la pantalla HH
    List<MovimientoEntity> findTop5ByHojaProduccion_IdHojaOrderByFechaDescIdMovimientoDesc(Integer idHoja);
    // Buscar movimientos por idDetalle ordenados por fecha ascendente y idMovimiento ascendente
    List<MovimientoEntity> findByConjunto_IdConjuntoOrderByFechaAscIdMovimientoAsc(String idConjunto);
    // Buscar movimientos por idHojaContenedor ordenados por fecha ascendente y idMovimiento ascendente
    List<MovimientoEntity> findByHojaContenedor_IdHojaContenedorOrderByFechaAscIdMovimientoAsc(Integer idHojaContenedor);
    // Buscar movimientos por idUsuario ordenados por fecha descendente y idMovimiento descendente
    List<MovimientoEntity> findByUsuario_IdUsuarioOrderByFechaDescIdMovimientoDesc(Integer idUsuario);
    // Validar si existen movimientos por idConjunto
    boolean existsByConjunto_IdConjunto(String idConjunto);
    // Validar si existen movimientos por idHoja
    boolean existsByHojaProduccion_IdHoja(Integer idHoja);
    // Validar si existen movimientos por idDetalle
    boolean existsByDetalleHoja_IdDetalle(Integer idDetalle);
    // Validar si existen movimientos por idHojaContenedor
    boolean existsByHojaContenedor_IdHojaContenedor(Integer idHojaContenedor);
    // Validar si existen movimientos por idUsuario
    boolean existsByUsuario_IdUsuario(Integer idUsuario);

    // Validar si existen movimientos por idHoja, idConjunto e idTipoMovimiento
    boolean existsByHojaProduccion_IdHojaAndConjunto_IdConjuntoAndTipoMovimiento_IdTipoMovimiento(
            Integer idHoja, String idConjunto, Integer idTipoMovimiento);

    // Contar movimientos por idHojaContenedor e idTipoMovimiento
    long countByHojaContenedor_IdHojaContenedorAndTipoMovimiento_IdTipoMovimiento(
            Integer idHojaContenedor, Integer idTipoMovimiento);

    // Validar si existe una salida pendiente por idHoja, idConjunto e idTipoMovimiento
    @Query("select case when count(m) > 0 then true else false end "
            + "from MovimientoEntity m "
            + "where m.hojaProduccion.idHoja = :idHoja "
            + "and m.conjunto.idConjunto = :idConjunto "
            + "and m.tipoMovimiento.idTipoMovimiento = :idTipoSalida "
            + "and not exists ("
            + "select entrada.idMovimiento from MovimientoEntity entrada "
            + "where entrada.hojaProduccion.idHoja = m.hojaProduccion.idHoja "
            + "and entrada.conjunto.idConjunto = m.conjunto.idConjunto "
            + "and entrada.tipoMovimiento.idTipoMovimiento = :idTipoEntrada "
            + "and (entrada.fecha > m.fecha or "
            + "(entrada.fecha = m.fecha and entrada.idMovimiento > m.idMovimiento)))")
    boolean existsSalidaPendiente(
            @Param("idHoja") Integer idHoja,
            @Param("idConjunto") String idConjunto,
            @Param("idTipoSalida") Integer idTipoSalida,
            @Param("idTipoEntrada") Integer idTipoEntrada);
}

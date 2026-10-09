package dgtic.core.repository;

import dgtic.core.model.entity.HojaContenedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface HojaContenedorRepository extends JpaRepository<HojaContenedorEntity, Integer> {
    // Buscar todos los registros de HojaContenedor por idHoja
    List<HojaContenedorEntity> findByHojaProduccion_IdHoja(Integer idHoja);
    // Buscar utilizaciones de una hoja ordenadas por fecha de asignación
    List<HojaContenedorEntity> findByHojaProduccion_IdHojaOrderByFechaAsignacionAscIdHojaContenedorAsc(Integer idHoja);
    // Buscar utilizaciones no liberadas de una hoja
    List<HojaContenedorEntity> findByHojaProduccion_IdHojaAndFechaLiberacionIsNull(Integer idHoja);
    // Buscar una utilización activa de un contenedor dentro de una hoja
    Optional<HojaContenedorEntity> findByHojaProduccion_IdHojaAndContenedor_IdContenedorAndFechaLiberacionIsNull(Integer idHoja, Integer idContenedor);
    // Buscar una utilización por ID verificando que pertenezca a la hoja indicada
    Optional<HojaContenedorEntity> findByIdHojaContenedorAndHojaProduccion_IdHoja(Integer idHojaContenedor, Integer idHoja);
    // Buscar la asignación abierta de una hoja, sin cierre de carga ni liberación
    Optional<HojaContenedorEntity> findByHojaProduccion_IdHojaAndFechaCierreCargaIsNullAndFechaLiberacionIsNull(Integer idHoja);
    // Buscar todos los registros de HojaContenedor por idContenedor
    List<HojaContenedorEntity> findByContenedor_IdContenedor(Integer idContenedor);
    // Buscar el registro de HojaContenedor por idContenedor y fechaLiberacion nula
    Optional<HojaContenedorEntity> findByContenedor_IdContenedorAndFechaLiberacionIsNull(Integer idContenedor);
    // Busca el registro de HojaContenedor por idHojaContenedor con bloqueo pesimista para actualización
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select hc from HojaContenedorEntity hc where hc.idHojaContenedor = :idHojaContenedor")
    Optional<HojaContenedorEntity> findByIdForUpdate(@Param("idHojaContenedor") Integer idHojaContenedor);
    // Busca el registro activo de HojaContenedor por idContenedor con bloqueo pesimista para actualización
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select hc from HojaContenedorEntity hc "
            + "where hc.contenedor.idContenedor = :idContenedor "
            + "and hc.fechaLiberacion is null")
    Optional<HojaContenedorEntity> findActiveByContenedorForUpdate(
            @Param("idContenedor") Integer idContenedor);
    // Validar si existe un registro activo de HojaContenedor por idContenedor y fechaLiberacion nula
    boolean existsByContenedor_IdContenedorAndFechaLiberacionIsNull(Integer idContenedor);
    // Validar si existe un registro de HojaContenedor por idHoja
    boolean existsByHojaProduccion_IdHoja(Integer idHoja);
    // Validar si una hoja conserva utilizaciones pendientes de liberar
    boolean existsByHojaProduccion_IdHojaAndFechaLiberacionIsNull(Integer idHoja);
    // Contar utilizaciones pendientes de liberar para Monitoreo
    long countByHojaProduccion_IdHojaAndFechaLiberacionIsNull(Integer idHoja);
    // Validar si existe un registro de HojaContenedor por idContenedor
    boolean existsByContenedor_IdContenedor(Integer idContenedor);
    // Validar si existe un registro de HojaContenedor por idUsuarioAsignacion
    boolean existsByUsuarioAsignacion_IdUsuario(Integer idUsuario);
    // Validar si existe un registro de HojaContenedor por idUsuarioCierre
    boolean existsByUsuarioCierre_IdUsuario(Integer idUsuario);
    // Validar si existe un registro de HojaContenedor por idUsuarioLiberacion
    boolean existsByUsuarioLiberacion_IdUsuario(Integer idUsuario);
}

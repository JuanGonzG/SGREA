package dgtic.core.repository;

import dgtic.core.model.entity.HojaProduccionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface HojaProduccionRepository extends JpaRepository<HojaProduccionEntity, Integer> {
    // Buscar hojas de producción por idBodega
    List<HojaProduccionEntity> findByBodega_IdBodega(Integer idBodega);
    // Buscar hojas de recepción de una bodega por los estados operativos indicados
    List<HojaProduccionEntity> findByBodega_IdBodegaAndEstadoHoja_IdEstadoHojaIn(Integer idBodega, List<Integer> estados);
    // Buscar hojas activas de una bodega ordenadas por antigüedad operativa
    List<HojaProduccionEntity> findByBodega_IdBodegaAndEstadoHoja_IdEstadoHojaInOrderByFechaSalidaAscIdHojaAsc(Integer idBodega, List<Integer> estados);
    // Buscar hojas históricas para Reportes con filtros opcionales y aislamiento por bodega
    @Query("""
            select h
            from HojaProduccionEntity h
            where h.bodega.idBodega = :idBodega
              and (:fechaDesde is null or h.fechaSalida >= :fechaDesde)
              and (:fechaHasta is null or h.fechaSalida <= :fechaHasta)
              and (:idEstado is null or h.estadoHoja.idEstadoHoja = :idEstado)
              and (:cliente is null or lower(h.cliente) like lower(concat('%', :cliente, '%')))
              and (:proyecto is null or lower(h.nombreProyecto) like lower(concat('%', :proyecto, '%')))
            order by h.fechaSalida desc, h.idHoja desc
            """)
    List<HojaProduccionEntity> buscarParaReporte(
            @Param("idBodega") Integer idBodega,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            @Param("idEstado") Integer idEstado,
            @Param("cliente") String cliente,
            @Param("proyecto") String proyecto);
    // Validar si existen hojas de producción por idBodega
    boolean existsByBodega_IdBodega(Integer idBodega);
    // Buscar hoja de producción por idHoja con bloqueo pesimista para actualización
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HojaProduccionEntity h where h.idHoja = :idHoja")
    Optional<HojaProduccionEntity> findByIdForUpdate(@Param("idHoja") Integer idHoja);
}

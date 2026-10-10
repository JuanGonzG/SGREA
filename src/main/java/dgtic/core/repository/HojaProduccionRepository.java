package dgtic.core.repository;

import dgtic.core.model.entity.HojaProduccionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HojaProduccionRepository extends JpaRepository<HojaProduccionEntity, Integer> {
    // Buscar hojas de producción por idBodega
    List<HojaProduccionEntity> findByBodega_IdBodega(Integer idBodega);
    // Buscar hojas de recepción de una bodega por los estados operativos indicados
    List<HojaProduccionEntity> findByBodega_IdBodegaAndEstadoHoja_IdEstadoHojaIn(Integer idBodega, List<Integer> estados);
    // Buscar hojas activas de una bodega ordenadas por antigüedad operativa
    List<HojaProduccionEntity> findByBodega_IdBodegaAndEstadoHoja_IdEstadoHojaInOrderByFechaSalidaAscIdHojaAsc(Integer idBodega, List<Integer> estados);
    // Validar si existen hojas de producción por idBodega
    boolean existsByBodega_IdBodega(Integer idBodega);
    // Buscar hoja de producción por idHoja con bloqueo pesimista para actualización
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HojaProduccionEntity h where h.idHoja = :idHoja")
    Optional<HojaProduccionEntity> findByIdForUpdate(@Param("idHoja") Integer idHoja);
}

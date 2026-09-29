package dgtic.core.repository;

import dgtic.core.model.entity.DetalleHojaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DetalleHojaRepository extends JpaRepository<DetalleHojaEntity, Integer> {
    // Buscar todos los detalles de hoja por idHoja
    List<DetalleHojaEntity> findByHojaProduccionEntity_IdHoja(Integer idHoja);
    // Eliminar todos los detalles de hoja por idHoja
    void deleteByHojaProduccionEntity_IdHoja(Integer idHoja);
    // Validar si existe un detalle de hoja por idHoja y idProducto
    boolean existsByHojaProduccionEntity_IdHojaAndProducto_IdProducto(Integer idHoja, Integer idProducto);
    // Validar si existe un detalle de hoja por idHoja y idProducto, excluyendo un detalle específico por idDetalle
    boolean existsByHojaProduccionEntity_IdHojaAndProducto_IdProductoAndIdDetalleNot(Integer idHoja, Integer idProducto, Integer idDetalle);
    // Buscar un detalle de hoja por idHoja y idProducto
    Optional<DetalleHojaEntity> findByHojaProduccionEntity_IdHojaAndProducto_IdProducto(Integer idHoja, Integer idProducto);
    // Buscar un detalle de hoja por idDetalle con bloqueo pesimista para actualización
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DetalleHojaEntity d where d.idDetalle = :idDetalle")
    Optional<DetalleHojaEntity> findByIdForUpdate(@Param("idDetalle") Integer idDetalle);
}

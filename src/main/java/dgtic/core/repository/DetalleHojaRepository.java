package dgtic.core.repository;

import dgtic.core.model.entity.DetalleHojaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleHojaRepository extends JpaRepository<DetalleHojaEntity, Integer> {
    List<DetalleHojaEntity> findByHojaProduccionEntity_IdHoja(Integer idHoja);
    void deleteByHojaProduccionEntity_IdHoja(Integer idHoja);
    boolean existsByHojaProduccionEntity_IdHojaAndProducto_IdProducto(Integer idHoja, Integer idProducto);
    boolean existsByHojaProduccionEntity_IdHojaAndProducto_IdProductoAndIdDetalleNot(
            Integer idHoja, Integer idProducto, Integer idDetalle);
}

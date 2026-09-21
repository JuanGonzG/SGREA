package dgtic.core.repository;

import dgtic.core.model.entity.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<ProductoEntity, Integer> {
    List<ProductoEntity> findByBodegaIdBodega(Integer idBodega);
    List<ProductoEntity> findByBodegaIdBodegaAndActivoTrue(Integer idBodega);
    boolean existsByBodega_IdBodega(Integer idBodega);
}

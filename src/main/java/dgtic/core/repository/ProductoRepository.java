package dgtic.core.repository;

import dgtic.core.model.entity.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<ProductoEntity, Integer> {
    // Buscar productos por idBodega
    List<ProductoEntity> findByBodegaIdBodega(Integer idBodega);
    // Buscar productos por idBodega y activos
    List<ProductoEntity> findByBodegaIdBodegaAndActivoTrue(Integer idBodega);
    // Validar si existen productos por idBodega
    boolean existsByBodega_IdBodega(Integer idBodega);
}

package dgtic.core.repository;

import dgtic.core.model.entity.BodegaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BodegaRepository extends JpaRepository<BodegaEntity, Integer> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdBodegaNot(String nombre, Integer idBodega);
}

package dgtic.core.repository;

import dgtic.core.model.entity.BodegaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BodegaRepository extends JpaRepository<BodegaEntity, Integer> {
    // Buscar Bodega por nombre ignorando mayúsculas y minúsculas
    boolean existsByNombreIgnoreCase(String nombre);
    // Buscar Bodega por nombre ignorando mayúsculas y minúsculas, excluyendo una bodega específica por idBodega
    boolean existsByNombreIgnoreCaseAndIdBodegaNot(String nombre, Integer idBodega);
}

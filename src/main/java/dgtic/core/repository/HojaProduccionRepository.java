package dgtic.core.repository;

import dgtic.core.model.entity.HojaProduccionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HojaProduccionRepository extends JpaRepository<HojaProduccionEntity, Integer> {
    // Buscar hojas de producción por idBodega
    List<HojaProduccionEntity> findByBodega_IdBodega(Integer idBodega);
    // Validar si existen hojas de producción por idBodega
    boolean existsByBodega_IdBodega(Integer idBodega);
}

package dgtic.core.repository;

import dgtic.core.model.entity.ConjuntoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConjuntoRepository extends JpaRepository<ConjuntoEntity, String> {
    // Buscar Conjuntos por idBodega
    List<ConjuntoEntity> findByProducto_Bodega_IdBodega(Integer idBodega);
    // Buscar Conjuntos por idProducto
    List<ConjuntoEntity> findByProducto_IdProducto(Integer idProducto);
    // Buscar Conjuntos por idBodega y idEstadoConjunto
    List<ConjuntoEntity> findByProducto_Bodega_IdBodegaAndEstadoConjunto_IdEstadoConjunto(Integer idBodega, Integer idEstadoConjunto);
    // Buscar Conjunto por orden de idConjunto descendente
    Optional<ConjuntoEntity> findTopByOrderByIdConjuntoDesc();
    // Buscar Conjunto por idConjunto con bloqueo pesimista para actualización
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConjuntoEntity c where c.idConjunto = :idConjunto")
    Optional<ConjuntoEntity> findByIdForUpdate(@Param("idConjunto") String idConjunto);
}

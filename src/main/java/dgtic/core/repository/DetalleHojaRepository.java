package dgtic.core.repository;

import dgtic.core.model.entity.DetalleHojaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleHojaRepository extends JpaRepository<DetalleHojaEntity, Integer> {
    // Buscar todos los detalles de hoja por idHoja
    List<DetalleHojaEntity> findByHojaProduccionEntity_IdHoja(Integer idHoja);
    // Eliminar todos los detalles de hoja por idHoja
    void deleteByHojaProduccionEntity_IdHoja(Integer idHoja);
    // Validar si existe un detalle de hoja por idHoja y idProducto
    boolean existsByHojaProduccionEntity_IdHojaAndProducto_IdProducto(Integer idHoja, Integer idProducto);
    // Validar si existe un detalle de hoja por idHoja y idProducto, excluyendo un detalle específico por idDetalle
    boolean existsByHojaProduccionEntity_IdHojaAndProducto_IdProductoAndIdDetalleNot(Integer idHoja, Integer idProducto, Integer idDetalle);
}

package dgtic.core.service;

import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.BodegaDTO;
import dgtic.core.model.dto.ProductoDTO;
import dgtic.core.model.entity.ProductoEntity;
import lombok.extern.slf4j.Slf4j;
import dgtic.core.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProductoService {
    @Autowired
    private ProductoRepository productoRepository;

    // Obtener todos los productos de una bodega específica
    public List<ProductoDTO> getProductosByBodega(Integer idBodega) {
        return productoRepository.findByBodegaIdBodega(idBodega)
                .stream()
                .map(producto -> Mapper.toProductoDTO(producto))
                .toList();
    }
    // Obtener los productos activos de una bodega específica
    public List<ProductoDTO> getProductosActivosByBodega(Integer idBodega) {
        return productoRepository.findByBodegaIdBodegaAndActivoTrue(idBodega)
                .stream()
                .map(producto -> Mapper.toProductoDTO(producto))
                .toList();
    }

    // Obtener un producto por su ID
    public Optional<ProductoDTO> getProductoById(Integer idProducto) {
        return productoRepository.findById(idProducto)
                .map(producto -> Mapper.toProductoDTO(producto));
    }
    // Agregar un nuevo producto
    public ProductoDTO addProducto(ProductoDTO productoDTO) {
        // Verificar si el producto ya existe
        ProductoEntity existente = productoDTO.getIdProducto() == null
                ? null
                : productoRepository.findById(productoDTO.getIdProducto()).orElse(null);
        // Guardar el producto (creando uno nuevo o actualizando uno existente)
        ProductoEntity guardado = productoRepository.save(Mapper.toProductoEntity(productoDTO));
        log.info("Producto {} idProducto={}, bodegaId={}",
                existente == null ? "creado" : "actualizado",
                guardado.getIdProducto(),
                guardado.getBodega() == null ? null : guardado.getBodega().getIdBodega());
        // Registrar cambios en el estado activo del producto
        if (existente != null && !java.util.Objects.equals(existente.getActivo(), guardado.getActivo())) {
            log.info("Producto {} idProducto={}, activo={}",
                    Boolean.TRUE.equals(guardado.getActivo()) ? "activado" : "desactivado",
                    guardado.getIdProducto(), guardado.getActivo());
        }
        return Mapper.toProductoDTO(guardado);
    }
    // Eliminar un producto por su ID
    public Boolean deleteProductoById(Integer idProducto) {
        if (productoRepository.existsById(idProducto)) {
            productoRepository.deleteById(idProducto);
            log.info("Producto eliminado idProducto={}", idProducto);
            return true;
        }
        log.warn("Eliminación de producto rechazada: producto inexistente idProducto={}", idProducto);
        return false;
    }
}

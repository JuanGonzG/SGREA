package dgtic.core.service;

import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.BodegaDTO;
import dgtic.core.model.dto.ProductoDTO;
import dgtic.core.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
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
        return Mapper.toProductoDTO(productoRepository.save(Mapper.toProductoEntity(productoDTO)));
    }
    // Eliminar un producto por su ID
    public Boolean deleteProductoById(Integer idProducto) {
        if (productoRepository.existsById(idProducto)) {
            productoRepository.deleteById(idProducto);
            return true;
        }
        return false;
    }
}

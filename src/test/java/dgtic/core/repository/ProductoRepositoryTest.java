package dgtic.core.repository;

import dgtic.core.model.entity.BodegaEntity;
import dgtic.core.model.entity.ProductoEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductoRepositoryTest {
    @Autowired
    private ProductoRepository productoRepository;

    @Test
    void buscarPorBodega(){
        List<ProductoEntity> productos = productoRepository.findByBodegaIdBodega(2);
        assertFalse(productos.isEmpty(), "No se encontraron productos para la bodega especificada");
        System.out.println("Productos encontrados:");
        productos.forEach(System.out::println);
    }

    @Test
    void buscarPorId(){
        ProductoEntity producto = productoRepository.findById(2).orElse(null);
        assertNotNull(producto);
        System.out.println("Producto encontrado: " + producto);
    }

    @Test
    void guardar(){
        ProductoEntity producto = ProductoEntity.builder()
                .nombre("Shure SM58")
                .descripcion("Micrófono dinámico de mano")
                .activo(true)
                .urlImagen("https://www.shure.com/damfiles/default/product-images/sm58-lc-hero.png")
                .bodega(BodegaEntity.builder().idBodega(1).build())
                .build();

        ProductoEntity nuevoProducto = productoRepository.save(producto);
        assertNotNull(nuevoProducto.getIdProducto());
        System.out.println("Producto guardado: " + nuevoProducto);
    }

}
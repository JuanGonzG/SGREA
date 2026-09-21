package dgtic.core.service;

import dgtic.core.model.dto.ProductoDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductoServiceTest {
    @Autowired
    private ProductoService productoService;

    @Transactional
    @Test
    void getProductosByBodega() {
        List<ProductoDTO> productos = productoService.getProductosByBodega(2);
        assertFalse(productos.isEmpty(), "No se encontraron productos para la bodega especificada");
        System.out.println("Productos encontrados:");
        productos.forEach(System.out::println);
    }

    @Test
    void getProductoById() {

    }
}
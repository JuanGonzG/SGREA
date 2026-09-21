package dgtic.core.controller;

import dgtic.core.model.dto.BodegaDTO;
import dgtic.core.model.dto.ProductoDTO;
import dgtic.core.service.ProductoService;
import dgtic.core.service.SesionService;
import dgtic.core.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping(value = "productos")
public class ProductoController {
    @Autowired
    private SesionService sesionService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private UsuarioService usuarioService;

    @Value("${app.productos.upload-dir}")
    private String productosUploadDir;

    // Validar si la bodega pertenece al usuario logueado
    private boolean bodegaAutorizada(HttpSession session, Integer idBodega) {
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        return idUsuario != null
                && idBodega != null
                && usuarioService.getBodegasByUsuarioID(idUsuario).stream()
                .anyMatch(bodega -> idBodega.equals(bodega.getIdBodega()));
    }

    // Guardar la imagen en el servidor y devolver la ruta relativa
    private String guardarImagen(MultipartFile imagen) throws IOException {
        // Validar el tipo de archivo y obtener la extensión
        String extension = switch (imagen.getContentType()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new IllegalArgumentException("El archivo debe ser una imagen PNG, JPG o WEBP.");
        };

        // Validar el tamaño del archivo (máximo 5 MB)
        if (imagen.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("La imagen no puede superar 5 MB.");
        }
        // Crear el directorio de destino si no existe
        Path directorio = Path.of(productosUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(directorio);
        // Generar un nombre de archivo único y seguro
        String nombreArchivo = UUID.randomUUID() + "." + extension;
        Path destino = directorio.resolve(nombreArchivo).normalize();
        // Validar que la ruta de destino esté dentro del directorio permitido
        if (!destino.startsWith(directorio)) {
            throw new IllegalArgumentException("Nombre de archivo no válido.");
        }
        // Guardar la imagen en el servidor
        Files.copy(imagen.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/productos/" + nombreArchivo;
    }

    // Endpoint para mostrar la página de inicio de productos
    @GetMapping("/")
    public String inicioProducto(HttpSession session, Model model){
        // Verificar si hay una sesión activa en el servidor
        Boolean sesionActiva = sesionService.isSesionActiva(session);
        if (!sesionActiva) {
            return "redirect:/login";
        }

        // Obtener los productos de la bodega logueada
        Integer idBodega = (Integer) session.getAttribute("idBodega");
        if (idBodega == null) {
            return "error/error";
        }
        List<ProductoDTO> productos = productoService.getProductosByBodega(idBodega);

        // Agregar los productos al modelo para que puedan ser accedidos en la vista
        model.addAttribute("productos", productos);
        return "catalogos/productos";
    }

    // Endpoint para obtener las bodegas de un usuario
    @GetMapping("/getBodegas/{idUsuario}")
    @ResponseBody
    public ResponseEntity<List<BodegaDTO>> getBodegas(HttpSession session, @PathVariable Integer idUsuario){
        // Verificar si hay una sesión activa en el servidor
        if (!sesionService.isSesionActiva(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Verificar si el usuario logueado es el mismo que el solicitado
        Integer idUsuarioSesion = (Integer) session.getAttribute("idUsuario");
        if (idUsuarioSesion == null || !idUsuarioSesion.equals(idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(usuarioService.getBodegasByUsuarioID(idUsuario));
    }

    // Endpoint para obtener un producto por su ID
    @GetMapping("/getProducto/{idProducto}")
    @ResponseBody
    public ResponseEntity<ProductoDTO> getProducto(HttpSession session, @PathVariable Integer idProducto){
        // Verificar si hay una sesión activa en el servidor
        if (!sesionService.isSesionActiva(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ProductoDTO producto = productoService.getProductoById(idProducto).orElse(null);
        // Verificar si el producto existe y si pertenece a una bodega autorizada
        if (producto == null || producto.getBodega() == null
                || !bodegaAutorizada(session, producto.getBodega().getIdBodega())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(producto);
    }

    // Endpoint para guardar un producto
    @PostMapping(value = "/guardar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarProducto(
            HttpSession session,
            @RequestPart("producto") ProductoDTO producto,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        Map<String, String> response = new HashMap<>();

        try {
            // Verificar si hay una sesión activa en el servidor
            if (!sesionService.isSesionActiva(session)) {
                response.put("mensaje", "La sesion ha expirado.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            // Verificar si la bodega pertenece al usuario logueado
            if (producto.getBodega() == null || !bodegaAutorizada(session, producto.getBodega().getIdBodega())) {
                throw new IllegalArgumentException("La bodega seleccionada no pertenece al usuario.");
            }
            // Verificar si el producto existe y si pertenece a una bodega autorizada
            if (producto.getIdProducto() != null) {
                ProductoDTO productoActual = productoService.getProductoById(producto.getIdProducto())
                        .orElseThrow(() -> new IllegalArgumentException("El producto no existe."));
                if (productoActual.getBodega() == null
                        || !bodegaAutorizada(session, productoActual.getBodega().getIdBodega())) {
                    throw new IllegalArgumentException("El producto no pertenece a una bodega autorizada.");
                }
            }
            // Guardar la imagen si se proporciona, o mantener la existente si se está editando
            if (imagen != null && !imagen.isEmpty()) {
                producto.setUrlImagen(guardarImagen(imagen));
            } else if (producto.getIdProducto() != null) {
                producto.setUrlImagen(productoService.getProductoById(producto.getIdProducto())
                        .map(ProductoDTO::getUrlImagen)
                        .orElseThrow(() -> new IllegalArgumentException("El producto no existe.")));
            } else {
                throw new IllegalArgumentException("Selecciona una imagen para el producto.");
            }
            // Guardar el producto en la base de datos
            productoService.addProducto(producto);
            response.put("mensaje", "Producto guardado correctamente");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            response.put("mensaje", exception.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException exception) {
            response.put("mensaje", "No fue posible guardar la imagen.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Endpoint para eliminar un producto
    @DeleteMapping("/eliminar/{idProducto}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminarProducto(HttpSession session, @PathVariable Integer idProducto) {
        Map<String, String> response = new HashMap<>();
        try {
            // Verificar si hay una sesión activa en el servidor
            if (!sesionService.isSesionActiva(session)) {
                response.put("mensaje", "La sesion ha expirado.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            ProductoDTO producto = productoService.getProductoById(idProducto)
                    .orElseThrow(() -> new IllegalArgumentException("El producto no existe."));
            // Verificar si el producto pertenece a una bodega autorizada
            if (producto.getBodega() == null
                    || !bodegaAutorizada(session, producto.getBodega().getIdBodega())) {
                throw new IllegalArgumentException("El producto no pertenece a una bodega autorizada.");
            }
            // Eliminar el producto de la base de datos
            if (!productoService.deleteProductoById(idProducto)) {
                throw new IllegalArgumentException("El producto no existe.");
            }
            response.put("mensaje", "Producto eliminado correctamente");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            response.put("mensaje", exception.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

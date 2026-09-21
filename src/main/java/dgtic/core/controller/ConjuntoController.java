package dgtic.core.controller;

import dgtic.core.model.dto.ConjuntoAltaDTO;
import dgtic.core.model.dto.ConjuntoDTO;
import dgtic.core.model.dto.ConjuntoEdicionDTO;
import dgtic.core.service.ConjuntoService;
import dgtic.core.service.ProductoService;
import dgtic.core.service.SesionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/conjunto")
public class ConjuntoController {
    @Autowired
    private ConjuntoService conjuntoService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private SesionService sesionService;

    // Mostrar la página de inicio de conjuntos
    @GetMapping
    public String inicioConjunto(HttpSession session, Model model) {
        // Verificar si la sesión está activa
        if (!sesionService.isSesionActiva(session)) {
            return "redirect:/login";
        }
        // Obtener el ID de la bodega de la sesión
        Integer idBodega = obtenerIdBodegaSesion(session);
        if (idBodega == null) {
            return "error/error";
        }
        // Agregar los conjuntos al modelo para mostrarlos en la vista
        model.addAttribute("conjuntos", conjuntoService.getConjuntosByBodegaId(idBodega));
        return "catalogos/conjuntos";
    }

    // Listar todos los conjuntos de la bodega asociada a la sesión
    @GetMapping("/listar")
    @ResponseBody
    public ResponseEntity<?> listar(HttpSession session) {
        // Validar la sesión
        ResponseEntity<Map<String, String>> errorSesion = validarSesion(session);
        if (errorSesion != null) {
            return errorSesion;
        }
        // Obtener el ID de la bodega de la sesión
        Integer idBodega = obtenerIdBodegaSesion(session);
        if (idBodega == null) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "La sesión no tiene una bodega seleccionada.");
        }
        // Devolver la lista de conjuntos asociados a la bodega
        return ResponseEntity.ok(conjuntoService.getConjuntosByBodegaId(idBodega));
    }

    // Listar todos los productos activos de la bodega asociada a la sesión
    @GetMapping("/productos")
    @ResponseBody
    public ResponseEntity<?> productos(HttpSession session) {
        // Validar la sesión
        ResponseEntity<Map<String, String>> errorSesion = validarSesion(session);
        if (errorSesion != null) {
            return errorSesion;
        }
        // Obtener el ID de la bodega de la sesión
        Integer idBodega = obtenerIdBodegaSesion(session);
        if (idBodega == null) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "La sesión no tiene una bodega seleccionada.");
        }
        // Devolver la lista de productos activos asociados a la bodega
        return ResponseEntity.ok(productoService.getProductosActivosByBodega(idBodega));
    }

    // Listar todos los estados posibles de un conjunto
    @GetMapping("/estados")
    @ResponseBody
    public ResponseEntity<?> estados(HttpSession session) {
        // Validar la sesión
        ResponseEntity<Map<String, String>> errorSesion = validarSesion(session);
        if (errorSesion != null) {
            return errorSesion;
        }
        // Devolver la lista de estados posibles de un conjunto
        return ResponseEntity.ok(conjuntoService.getEstados());
    }

    // Obtener un conjunto por su ID y la bodega asociada a la sesión
    @GetMapping("/{idConjunto}")
    @ResponseBody
    public ResponseEntity<?> obtener(HttpSession session, @PathVariable String idConjunto) {
        // Validar la sesión
        ResponseEntity<Map<String, String>> errorSesion = validarSesion(session);
        if (errorSesion != null) {
            return errorSesion;
        }
        // Obtener el ID de la bodega de la sesión
        Integer idBodega = obtenerIdBodegaSesion(session);
        if (idBodega == null) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "La sesión no tiene una bodega seleccionada.");
        }
        // Devolver el conjunto si existe, o un 404 si no se encuentra
        return conjuntoService.getById(idConjunto, idBodega)
                .map(conjunto -> (ResponseEntity<?>) ResponseEntity.ok(conjunto))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Guardar un nuevo conjunto
    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<?> guardar(HttpSession session, @RequestBody ConjuntoAltaDTO conjuntoAltaDTO) {
        // Validar la sesión
        ResponseEntity<Map<String, String>> errorSesion = validarSesion(session);
        if (errorSesion != null) {
            return errorSesion;
        }
        try {
            // Obtener el ID de la bodega de la sesión
            Integer idBodega = obtenerIdBodegaSesion(session);
            if (idBodega == null) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "La sesión no tiene una bodega seleccionada.");
            }
            // Guardar el conjunto y devolverlo con un estado 201 Created
            ConjuntoDTO conjunto = conjuntoService.addConjunto(conjuntoAltaDTO, idBodega);
            return ResponseEntity.status(HttpStatus.CREATED).body(conjunto);
        } catch (IllegalArgumentException exception) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            return buildErrorResponse(HttpStatus.CONFLICT, "No fue posible guardar el conjunto por un conflicto de integridad.");
        } catch (DataAccessException exception) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible guardar el conjunto.");
        }
    }

    // Actualizar un conjunto existente
    @PutMapping("/actualizar")
    @ResponseBody
    public ResponseEntity<?> actualizar(HttpSession session, @RequestBody ConjuntoEdicionDTO conjuntoEdicionDTO) {
        // Validar la sesión
        ResponseEntity<Map<String, String>> errorSesion = validarSesion(session);
        if (errorSesion != null) {
            return errorSesion;
        }
        try {
            // Obtener el ID de la bodega de la sesión
            Integer idBodega = obtenerIdBodegaSesion(session);
            if (idBodega == null) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "La sesión no tiene una bodega seleccionada.");
            }
            // Actualizar el conjunto y devolverlo con un estado 200 OK
            return ResponseEntity.ok(conjuntoService.updateConjunto(conjuntoEdicionDTO, idBodega));
        } catch (IllegalArgumentException exception) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (IllegalStateException exception) {
            return buildErrorResponse(HttpStatus.CONFLICT, exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            return buildErrorResponse(HttpStatus.CONFLICT, "No fue posible actualizar el conjunto por un conflicto de integridad.");
        } catch (DataAccessException exception) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible actualizar el conjunto.");
        }
    }

    // Eliminar un conjunto existente
    @DeleteMapping("/eliminar/{idConjunto}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminar(HttpSession session, @PathVariable String idConjunto) {
        // Validar la sesión
        ResponseEntity<Map<String, String>> errorSesion = validarSesion(session);
        if (errorSesion != null) {
            return errorSesion;
        }
        try {
            // Obtener el ID de la bodega de la sesión
            Integer idBodega = obtenerIdBodegaSesion(session);
            if (idBodega == null) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "La sesión no tiene una bodega seleccionada.");
            }
            // Eliminar el conjunto
            conjuntoService.deleteConjunto(idConjunto, idBodega);
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Conjunto eliminado correctamente.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            return buildErrorResponse(HttpStatus.CONFLICT, "No se puede eliminar el conjunto porque tiene dependencias.");
        } catch (DataAccessException exception) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible eliminar el conjunto.");
        }
    }

    // Obtener el ID de la bodega de la sesión
    private Integer obtenerIdBodegaSesion(HttpSession session) {
        return (Integer) session.getAttribute("idBodega");
    }

    // Validar la sesión
    private ResponseEntity<Map<String, String>> validarSesion(HttpSession session) {
        if (!sesionService.isSesionActiva(session)) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "La sesión ha expirado.");
        }
        return null;
    }

    // Construir una respuesta de error
    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String mensaje) {
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", mensaje == null ? "La operación no es válida." : mensaje);
        return ResponseEntity.status(status).body(response);
    }
}

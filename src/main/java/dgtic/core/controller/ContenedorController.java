package dgtic.core.controller;

import dgtic.core.model.dto.ContenedorAltaDTO;
import dgtic.core.model.dto.ContenedorDTO;
import dgtic.core.model.dto.ContenedorEdicionDTO;
import dgtic.core.service.ContenedorService;
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
@RequestMapping("/contenedor")
public class ContenedorController {
    @Autowired
    private ContenedorService contenedorService;

    @Autowired
    private SesionService sesionService;

    // Endpoint para mostrar la página de inicio de contenedores
    @GetMapping
    public String inicioContenedor(HttpSession session, Model model) {
        // Validar si la sesión está activa
        if (!sesionService.isSesionActiva(session)) {
            return "redirect:/login";
        }
        // Agregar los contenedores al modelo para que puedan ser mostrados en la vista
        model.addAttribute("contenedores", contenedorService.getContenedores());
        return "catalogos/contenedores";
    }

    // Endpoint para obtener la lista de contenedores en formato JSON
    @GetMapping("/listar")
    @ResponseBody
    public ResponseEntity<?> listar(HttpSession session) {
        // Validar la sesión antes de procesar la solicitud
        ResponseEntity<Map<String, String>> errorSesion = validarSesion(session);
        if (errorSesion != null) {
            return errorSesion;
        }
        // Intentar obtener la lista de contenedores y manejar posibles excepciones
        try {
            return ResponseEntity.ok(contenedorService.getContenedores());
        } catch (DataAccessException exception) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar los contenedores.");
        }
    }
    // Endpoint para obtener la lista de estados de contenedores en formato JSON
    @GetMapping("/estados")
    @ResponseBody
    public ResponseEntity<?> estados(HttpSession session) {
        // Validar la sesión antes de procesar la solicitud
        ResponseEntity<Map<String, String>> errorSesion = validarSesion(session);
        if (errorSesion != null) {
            return errorSesion;
        }
        // Intentar obtener la lista de estados de contenedores y manejar posibles excepciones
        try {
            return ResponseEntity.ok(contenedorService.getEstados());
        } catch (DataAccessException exception) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar los estados de contenedor.");
        }
    }

    // Endpoint para obtener un contenedor por su ID en formato JSON
    @GetMapping("/{idContenedor}")
    @ResponseBody
    public ResponseEntity<?> obtener(HttpSession session, @PathVariable Integer idContenedor) {
        // Validar la sesión antes de procesar la solicitud
        ResponseEntity<Map<String, String>> errorSesion = validarSesion(session);
        if (errorSesion != null) {
            return errorSesion;
        }
        // Intentar obtener el contenedor por su ID y manejar posibles excepciones
        try {
            return contenedorService.getById(idContenedor)
                    .map(contenedor -> (ResponseEntity<?>) ResponseEntity.ok(contenedor))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException exception) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException exception) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar el contenedor.");
        }
    }
    // Endpoint para guardar un nuevo contenedor en formato JSON
    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<?> guardar(HttpSession session, @RequestBody ContenedorAltaDTO contenedorAltaDTO) {
        // Validar la sesión antes de procesar la solicitud
        ResponseEntity<Map<String, String>> errorSesion = validarSesion(session);
        if (errorSesion != null) {
            return errorSesion;
        }
        // Intentar guardar el nuevo contenedor y manejar posibles excepciones
        try {
            ContenedorDTO contenedor = contenedorService.addContenedor(contenedorAltaDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(contenedor);
        } catch (IllegalArgumentException exception) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (IllegalStateException exception) {
            return buildErrorResponse(HttpStatus.CONFLICT, exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            return buildErrorResponse(HttpStatus.CONFLICT,
                    "No fue posible guardar el contenedor por un conflicto de integridad.");
        } catch (DataAccessException exception) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible guardar el contenedor.");
        }
    }
    // Endpoint para actualizar un contenedor existente en formato JSON
    @PutMapping("/actualizar/{idContenedor}")
    @ResponseBody
    public ResponseEntity<?> actualizar(HttpSession session, @PathVariable Integer idContenedor, @RequestBody ContenedorEdicionDTO contenedorEdicionDTO) {
        // Validar la sesión antes de procesar la solicitud
        ResponseEntity<Map<String, String>> errorSesion = validarSesion(session);
        if (errorSesion != null) {
            return errorSesion;
        }

        // Intentar actualizar el contenedor y manejar posibles excepciones
        try {
            return ResponseEntity.ok(
                    contenedorService.updateContenedor(idContenedor, contenedorEdicionDTO));
        } catch (IllegalArgumentException exception) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (IllegalStateException exception) {
            return buildErrorResponse(HttpStatus.CONFLICT, exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            return buildErrorResponse(HttpStatus.CONFLICT,
                    "No fue posible actualizar el contenedor por un conflicto de integridad.");
        } catch (DataAccessException exception) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible actualizar el contenedor.");
        }
    }

    // Endpoint para eliminar un contenedor existente en formato JSON
    @DeleteMapping("/eliminar/{idContenedor}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminar(HttpSession session, @PathVariable Integer idContenedor) {
        // Validar la sesión antes de procesar la solicitud
        ResponseEntity<Map<String, String>> errorSesion = validarSesion(session);
        if (errorSesion != null) {
            return errorSesion;
        }
        // Intentar eliminar el contenedor y manejar posibles excepciones
        try {
            contenedorService.deleteContenedor(idContenedor);
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Contenedor eliminado correctamente.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            return buildErrorResponse(HttpStatus.CONFLICT,
                    "No se puede eliminar el contenedor porque tiene dependencias.");
        } catch (DataAccessException exception) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible eliminar el contenedor.");
        }
    }
    // Metodo privado para validar la sesión antes de procesar las solicitudes
    private ResponseEntity<Map<String, String>> validarSesion(HttpSession session) {
        // Verificar si la sesión está activa utilizando el servicio de sesión
        if (!sesionService.isSesionActiva(session)) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "La sesión ha expirado.");
        }
        return null;
    }
    // Metodo privado para construir una respuesta de error con un mensaje y un estado HTTP
    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String mensaje) {
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", mensaje == null ? "La operación no es válida." : mensaje);
        return ResponseEntity.status(status).body(response);
    }
}

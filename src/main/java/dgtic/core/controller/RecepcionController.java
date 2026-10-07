package dgtic.core.controller;

import dgtic.core.model.dto.RecepcionEntradaRequest;
import dgtic.core.model.dto.RecepcionEntradaResponse;
import dgtic.core.model.dto.RecepcionEstadoDTO;
import dgtic.core.model.dto.RecepcionLiberarContenedorResponse;
import dgtic.core.model.dto.RecepcionSeleccionarContenedorRequest;
import dgtic.core.model.dto.RecepcionSeleccionarContenedorResponse;
import dgtic.core.service.RecepcionService;
import dgtic.core.service.SesionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/hh/recepcion")
public class RecepcionController {
    @Autowired
    private SesionService sesionService;

    @Autowired
    private RecepcionService recepcionService;
    // Endpoint para mostrar la página de recepción
    @GetMapping
    public String inicio(HttpSession session) {
        // Verificar si la sesión está activa
        if (!sesionService.isSesionActiva(session)) {
            return "redirect:/login";
        }
        return "hh/recepcion";
    }
    // Endpoint para mostrar la página de recepción con un ID de hoja específico
    @GetMapping("/{idHoja}")
    public String mostrarHoja(@PathVariable Integer idHoja, HttpSession session, Model model) {
        // Verificar si la sesión está activa
        if (!sesionService.isSesionActiva(session)) {
            return "redirect:/login";
        }
        model.addAttribute("idHoja", idHoja);
        return "hh/recepcion";
    }

    // Endpoint para obtener el estado de una hoja específica
    @GetMapping("/api/hojas/{idHoja}/estado")
    @ResponseBody
    public ResponseEntity<?> obtenerEstado(@PathVariable Integer idHoja, HttpSession session) {
        // Validar el contexto de la sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar obtener el estado de la hoja
        try {
            RecepcionEstadoDTO estado = recepcionService.obtenerEstado(
                    idHoja, obtenerIdBodega(session));
            return ResponseEntity.ok(estado);
        } catch (IllegalArgumentException exception) {
            return respuestaError(codigoParaRecurso(exception), exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar el estado de recepción.");
        }
    }

    // Endpoint para seleccionar un contenedor para una hoja específica
    @PostMapping("/api/hojas/{idHoja}/contenedores")
    @ResponseBody
    public ResponseEntity<?> seleccionarContenedor(
            @PathVariable Integer idHoja,
            @Valid @RequestBody RecepcionSeleccionarContenedorRequest request,
            BindingResult bindingResult,
            HttpSession session) {
        // Validar el contexto de la sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Validar los datos de entrada
        if (bindingResult.hasErrors()) {
            return respuestaError(HttpStatus.BAD_REQUEST,
                    bindingResult.getFieldError().getDefaultMessage());
        }
        try {
            // Intentar seleccionar el contenedor
            RecepcionSeleccionarContenedorResponse response = recepcionService.seleccionarContenedor(
                    idHoja,
                    request.getCodigo(),
                    obtenerIdUsuario(session),
                    obtenerIdBodega(session));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return respuestaError(codigoParaRecurso(exception), exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible seleccionar el contenedor.");
        }
    }
    // Endpoint para registrar una entrada en una hoja específica
    @PostMapping("/api/hojas/{idHoja}/entradas")
    @ResponseBody
    public ResponseEntity<?> registrarEntrada(
            @PathVariable Integer idHoja,
            @Valid @RequestBody RecepcionEntradaRequest request,
            BindingResult bindingResult,
            HttpSession session) {
        // Validar el contexto de la sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Validar los datos de entrada
        if (bindingResult.hasErrors()) {
            return respuestaError(HttpStatus.BAD_REQUEST,
                    bindingResult.getFieldError().getDefaultMessage());
        }
        try {
            // Intentar registrar la entrada
            RecepcionEntradaResponse response = recepcionService.registrarEntrada(
                    idHoja,
                    request.getCodigoConjunto(),
                    request.getIdHojaContenedor(),
                    obtenerIdUsuario(session),
                    obtenerIdBodega(session));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return respuestaError(codigoParaRecurso(exception), exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible registrar la entrada.");
        }
    }
    // Endpoint para liberar un contenedor de una hoja específica
    @PostMapping("/api/hojas/{idHoja}/contenedores/{idHojaContenedor}/liberar")
    @ResponseBody
    public ResponseEntity<?> liberarContenedor(
            @PathVariable Integer idHoja,
            @PathVariable Integer idHojaContenedor,
            HttpSession session) {
        // Validar el contexto de la sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        try {
            // Intentar liberar el contenedor
            RecepcionLiberarContenedorResponse response = recepcionService.liberarContenedor(
                    idHoja,
                    idHojaContenedor,
                    obtenerIdUsuario(session),
                    obtenerIdBodega(session));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return respuestaError(codigoParaRecurso(exception), exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible liberar el contenedor.");
        }
    }
    // Método privado para validar el contexto de la sesión
    private ResponseEntity<Map<String, String>> validarContexto(HttpSession session) {
        // Verificar si la sesión está activa
        if (!sesionService.isSesionActiva(session)) {
            return respuestaError(HttpStatus.UNAUTHORIZED, "La sesión ha expirado.");
        }
        // Verificar si la sesión tiene el contexto operativo necesario
        if (obtenerIdUsuario(session) == null || obtenerIdBodega(session) == null) {
            return respuestaError(HttpStatus.UNAUTHORIZED,
                    "La sesión no tiene contexto operativo.");
        }
        return null;
    }
    // Métodos privados para obtener el ID de usuario y el ID de bodega desde la sesión
    private Integer obtenerIdUsuario(HttpSession session) {
        return (Integer) session.getAttribute("idUsuario");
    }

    private Integer obtenerIdBodega(HttpSession session) {
        return (Integer) session.getAttribute("idBodega");
    }
    // Método privado para determinar el código de estado HTTP basado en la excepción
    private HttpStatus codigoParaRecurso(IllegalArgumentException exception) {
        String mensaje = exception.getMessage();
        if (mensaje != null && mensaje.toLowerCase().contains("no existe")) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_REQUEST;
    }

    // Método privado para construir una respuesta de error
    private ResponseEntity<Map<String, String>> respuestaError(
            HttpStatus status, String mensaje) {
        Map<String, String> cuerpo = new HashMap<>();
        cuerpo.put("mensaje", mensaje == null ? "La operación no es válida." : mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }
}

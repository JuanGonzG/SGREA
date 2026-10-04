package dgtic.core.controller;

import dgtic.core.model.dto.SurtidoAsignarContenedorRequest;
import dgtic.core.model.dto.SurtidoAsignarContenedorResponse;
import dgtic.core.model.dto.SurtidoCerrarCargaResponse;
import dgtic.core.model.dto.SurtidoEstadoDTO;
import dgtic.core.model.dto.SurtidoSalidaRequest;
import dgtic.core.model.dto.SurtidoSalidaResponse;
import dgtic.core.service.SesionService;
import dgtic.core.service.SurtidoService;
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
@RequestMapping("/hh/surtido")
public class SurtidoController {
    @Autowired
    private SesionService sesionService;

    @Autowired
    private SurtidoService surtidoService;

    // Enpoint para mostrar la página de surtido
    @GetMapping
    public String inicio(HttpSession session) {
        // Verificar si la sesión está activa
        if (!sesionService.isSesionActiva(session)) {
            return "redirect:/login";
        }
        return "hh/surtido";
    }
    // Endpoint para mostrar la página de surtido con un idHoja específico
    @GetMapping("/{idHoja}")
    public String mostrarHoja(@PathVariable Integer idHoja, HttpSession session, Model model) {
        // Verificar si la sesión está activa
        if (!sesionService.isSesionActiva(session)) {
            return "redirect:/login";
        }
        model.addAttribute("idHoja", idHoja);
        return "hh/surtido";
    }
    // Endpoint para obtener el estado de surtido de una hoja específica
    @GetMapping("/api/hojas/{idHoja}/estado")
    @ResponseBody
    public ResponseEntity<?> obtenerEstado(@PathVariable Integer idHoja, HttpSession session) {
        // Validar el contexto de la sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar obtener el estado de surtido
        try {
            SurtidoEstadoDTO estado = surtidoService.obtenerEstado(idHoja, obtenerIdBodega(session));
            return ResponseEntity.ok(estado);
        } catch (IllegalArgumentException exception) {
            return respuestaError(codigoParaRecurso(exception), exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar el estado de surtido.");
        }
    }
    // Endpoint para asignar un contenedor a una hoja específica
    @PostMapping("/api/hojas/{idHoja}/contenedores")
    @ResponseBody
    public ResponseEntity<?> asignarContenedor(
            @PathVariable Integer idHoja,
            @Valid @RequestBody SurtidoAsignarContenedorRequest request,
            BindingResult bindingResult,
            HttpSession session) {
        // Validar el contexto de la sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Validar los errores de binding
        if (bindingResult.hasErrors()) {
            return respuestaError(HttpStatus.BAD_REQUEST,
                    bindingResult.getFieldError().getDefaultMessage());
        }
        try {
            // Intentar asignar el contenedor a la hoja
            SurtidoAsignarContenedorResponse response = surtidoService.asignarContenedor(
                    idHoja,
                    request.getCodigo(),
                    obtenerIdUsuario(session),
                    obtenerIdBodega(session));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException exception) {
            return respuestaError(codigoParaRecurso(exception), exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible asignar el contenedor.");
        }
    }
    // Endpoint para registrar la salida de un contenedor de una hoja específica
    @PostMapping("/api/hojas/{idHoja}/salidas")
    @ResponseBody
    public ResponseEntity<?> registrarSalida(
            @PathVariable Integer idHoja,
            @Valid @RequestBody SurtidoSalidaRequest request,
            BindingResult bindingResult,
            HttpSession session) {
        // Validar el contexto de la sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Validar los errores de binding
        if (bindingResult.hasErrors()) {
            return respuestaError(HttpStatus.BAD_REQUEST,
                    bindingResult.getFieldError().getDefaultMessage());
        }
        try {
            // Intentar registrar la salida del contenedor
            SurtidoSalidaResponse response = surtidoService.registrarSalida(
                    idHoja,
                    request.getIdHojaContenedor(),
                    request.getCodigoConjunto(),
                    obtenerIdUsuario(session),
                    obtenerIdBodega(session));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return respuestaError(codigoParaRecurso(exception), exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible registrar la salida.");
        }
    }

    // Endpoint para cerrar la carga de un contenedor de una hoja específica
    @PostMapping("/api/hojas/{idHoja}/contenedores/{idHojaContenedor}/cerrar")
    @ResponseBody
    public ResponseEntity<?> cerrarCarga(
            @PathVariable Integer idHoja,
            @PathVariable Integer idHojaContenedor,
            HttpSession session) {
        // Validar el contexto de la sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar cerrar la carga del contenedor
        try {
            SurtidoCerrarCargaResponse response = surtidoService.cerrarCarga(
                    idHoja,
                    idHojaContenedor,
                    obtenerIdUsuario(session),
                    obtenerIdBodega(session));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return respuestaError(codigoParaRecurso(exception), exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible cerrar la carga.");
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
    // Método privado para obtener el idUsuario
    private Integer obtenerIdUsuario(HttpSession session) {
        return (Integer) session.getAttribute("idUsuario");
    }

    // Método privado para obtener el idBodega
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
            HttpStatus status,
            String mensaje) {
        Map<String, String> cuerpo = new HashMap<>();
        cuerpo.put("mensaje", mensaje == null ? "La operación no es válida." : mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }
}

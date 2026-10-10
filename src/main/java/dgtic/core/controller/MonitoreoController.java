package dgtic.core.controller;

import dgtic.core.exception.MonitoreoNoAutorizadoException;
import dgtic.core.exception.MonitoreoRecursoNoEncontradoException;
import dgtic.core.model.dto.monitoreo.MonitoreoEstadoDTO;
import dgtic.core.model.dto.monitoreo.MonitoreoHojaDTO;
import dgtic.core.service.MonitoreoService;
import dgtic.core.service.SesionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/monitoreo")
public class MonitoreoController {
    private final MonitoreoService monitoreoService;
    private final SesionService sesionService;

    public MonitoreoController(MonitoreoService monitoreoService, SesionService sesionService) {
        this.monitoreoService = monitoreoService;
        this.sesionService = sesionService;
    }

    // Endpoint para mostrar la vista de inicio del monitoreo
    @GetMapping
    public Object inicio(HttpSession session) {
        // Validar la sesión y el acceso al monitoreo
        Object error = validarVista(session);
        return error == null ? "monitoreo/index" : error;
    }

    // Endpoint para mostrar la vista de surtido
    @GetMapping("/surtido/{idHoja}")
    public Object mostrarSurtido(@PathVariable Integer idHoja, HttpSession session, Model model) {
        // Validar la sesión y el acceso al monitoreo
        Object error = validarVista(session);
        if (error != null) {
            return error;
        }
        model.addAttribute("idHoja", idHoja);
        return "monitoreo/surtido";
    }
    // Endpoint para mostrar la vista de recepción
    @GetMapping("/recepcion/{idHoja}")
    public Object mostrarRecepcion(@PathVariable Integer idHoja, HttpSession session, Model model) {
        // Validar la sesión y el acceso al monitoreo
        Object error = validarVista(session);
        if (error != null) {
            return error;
        }
        model.addAttribute("idHoja", idHoja);
        return "monitoreo/recepcion";
    }

    @GetMapping("/api/surtidos")
    @ResponseBody
    public ResponseEntity<?> obtenerSurtidos(HttpSession session) {
        // Validar la sesión y el contexto operativo
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar obtener los surtidos activos desde el servicio de monitoreo
        try {
            List<MonitoreoHojaDTO> surtidos = monitoreoService.obtenerSurtidosActivos(obtenerIdUsuario(session), obtenerIdBodega(session));
            return ResponseEntity.ok(surtidos);
        } catch (MonitoreoNoAutorizadoException exception) {
            return respuestaError(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar los surtidos.");
        }
    }

    // Endpoint para obtener las recepciones activas
    @GetMapping("/api/recepciones")
    @ResponseBody
    public ResponseEntity<?> obtenerRecepciones(HttpSession session) {
        // Validar la sesión y el contexto operativo
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar obtener las recepciones activas desde el servicio de monitoreo
        try {
            List<MonitoreoHojaDTO> recepciones = monitoreoService.obtenerRecepcionesActivas(
                    obtenerIdUsuario(session), obtenerIdBodega(session));
            return ResponseEntity.ok(recepciones);
        } catch (MonitoreoNoAutorizadoException exception) {
            return respuestaError(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar las recepciones.");
        }
    }
    // Endpoint para obtener el estado de un surtido específico
    @GetMapping("/api/surtidos/{idHoja}")
    @ResponseBody
    public ResponseEntity<?> obtenerSurtido(@PathVariable Integer idHoja, HttpSession session) {
        // Validar la sesión y el contexto operativo
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar obtener el estado del surtido desde el servicio de monitoreo
        try {
            MonitoreoEstadoDTO estado = monitoreoService.obtenerSurtido(
                    obtenerIdUsuario(session), obtenerIdBodega(session), idHoja);
            return ResponseEntity.ok(estado);
        } catch (MonitoreoNoAutorizadoException exception) {
            return respuestaError(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (MonitoreoRecursoNoEncontradoException exception) {
            return respuestaError(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return respuestaError(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar el surtido.");
        }
    }
    // Endpoint para obtener el estado de una recepción específica
    @GetMapping("/api/recepciones/{idHoja}")
    @ResponseBody
    public ResponseEntity<?> obtenerRecepcion(@PathVariable Integer idHoja, HttpSession session) {
        // Validar la sesión y el contexto operativo
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar obtener el estado de la recepción desde el servicio de monitoreo
        try {
            MonitoreoEstadoDTO estado = monitoreoService.obtenerRecepcion(
                    obtenerIdUsuario(session), obtenerIdBodega(session), idHoja);
            return ResponseEntity.ok(estado);
        } catch (MonitoreoNoAutorizadoException exception) {
            return respuestaError(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (MonitoreoRecursoNoEncontradoException exception) {
            return respuestaError(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return respuestaError(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar la recepción.");
        }
    }
    // Método privado para validar la sesión y el acceso a la vista de monitoreo
    private Object validarVista(HttpSession session) {
        // Validar si la sesión está activa
        if (!sesionService.isSesionActiva(session)) {
            return "redirect:/login";
        }
        // Validar si el usuario tiene acceso al monitoreo
        if (obtenerIdUsuario(session) == null || obtenerIdBodega(session) == null) {
            return "redirect:/login";
        }
        // Intentar validar el acceso al monitoreo mediante el servicio de monitoreo
        try {
            monitoreoService.validarAccesoMonitoreo(
                    obtenerIdUsuario(session), obtenerIdBodega(session));
            return null;
        } catch (MonitoreoNoAutorizadoException exception) {
            return respuestaError(HttpStatus.FORBIDDEN, exception.getMessage());
        }
    }
    // Método privado para validar la sesión y el contexto operativo
    private ResponseEntity<Map<String, String>> validarContexto(HttpSession session) {
        // Validar si la sesión está activa
        if (!sesionService.isSesionActiva(session)) {
            return respuestaError(HttpStatus.UNAUTHORIZED, "La sesión ha expirado.");
        }
        // Validar si el usuario tiene contexto operativo (idUsuario y idBodega)
        if (obtenerIdUsuario(session) == null || obtenerIdBodega(session) == null) {
            return respuestaError(HttpStatus.UNAUTHORIZED,
                    "La sesión no tiene contexto operativo.");
        }
        return null;
    }
    // Método privado para obtener el id del usuario desde la sesión
    private Integer obtenerIdUsuario(HttpSession session) {
        return (Integer) session.getAttribute("idUsuario");
    }
    // Método privado para obtener el id de la bodega desde la sesión
    private Integer obtenerIdBodega(HttpSession session) {
        return (Integer) session.getAttribute("idBodega");
    }
    // Método privado para construir una respuesta de error con un mensaje y un estado HTTP
    private ResponseEntity<Map<String, String>> respuestaError(
            HttpStatus status, String mensaje) {
        Map<String, String> cuerpo = new HashMap<>();
        cuerpo.put("mensaje", mensaje == null ? "La operación no es válida." : mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }
}

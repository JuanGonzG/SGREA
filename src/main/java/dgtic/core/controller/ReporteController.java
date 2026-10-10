package dgtic.core.controller;

import dgtic.core.exception.ReporteNoAutorizadoException;
import dgtic.core.exception.ReporteRecursoNoEncontradoException;
import dgtic.core.model.dto.reporte.ReporteConjuntoDTO;
import dgtic.core.model.dto.reporte.ReporteConjuntoFiltroDTO;
import dgtic.core.model.dto.reporte.ReporteContenedorDTO;
import dgtic.core.model.dto.reporte.ReporteContenedorFiltroDTO;
import dgtic.core.model.dto.reporte.ReporteHojaDTO;
import dgtic.core.model.dto.reporte.ReporteHojaFiltroDTO;
import dgtic.core.model.dto.reporte.ReporteMovimientoDTO;
import dgtic.core.model.dto.reporte.ReporteMovimientoFiltroDTO;
import dgtic.core.model.dto.reporte.ReporteOperacionHojaDTO;
import dgtic.core.service.ReporteService;
import dgtic.core.service.SesionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reportes/api")
public class ReporteController {
    private final ReporteService reporteService;
    private final SesionService sesionService;

    public ReporteController(ReporteService reporteService, SesionService sesionService) {
        this.reporteService = reporteService;
        this.sesionService = sesionService;
    }
    // Endpoint para buscar hojas
    @GetMapping("/hojas")
    @ResponseBody
    public ResponseEntity<?> buscarHojas(@ModelAttribute ReporteHojaFiltroDTO filtro, HttpSession session) {
        // Validar contexto de sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar buscar hojas con el servicio de reporte
        try {
            List<ReporteHojaDTO> resultado = reporteService.buscarHojas(obtenerIdUsuario(session), obtenerIdBodega(session), filtro);
            return ResponseEntity.ok(resultado);
        } catch (ReporteNoAutorizadoException exception) {
            return respuestaError(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return respuestaError(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar las hojas.");
        }
    }
    // Endpoint para buscar movimientos
    @GetMapping("/movimientos")
    @ResponseBody
    public ResponseEntity<?> buscarMovimientos(@ModelAttribute ReporteMovimientoFiltroDTO filtro, HttpSession session) {
        // Validar contexto de sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar buscar movimientos con el servicio de reporte
        try {
            List<ReporteMovimientoDTO> resultado = reporteService.buscarMovimientos(obtenerIdUsuario(session), obtenerIdBodega(session), filtro);
            return ResponseEntity.ok(resultado);
        } catch (ReporteNoAutorizadoException exception) {
            return respuestaError(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return respuestaError(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar los movimientos.");
        }
    }
    // Endpoint para buscar contenedores
    @GetMapping("/contenedores")
    @ResponseBody
    public ResponseEntity<?> buscarContenedores(@ModelAttribute ReporteContenedorFiltroDTO filtro, HttpSession session) {
        // Validar contexto de sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar buscar contenedores con el servicio de reporte
        try {
            List<ReporteContenedorDTO> resultado = reporteService.buscarContenedores(obtenerIdUsuario(session), obtenerIdBodega(session), filtro);
            return ResponseEntity.ok(resultado);
        } catch (ReporteNoAutorizadoException exception) {
            return respuestaError(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return respuestaError(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar los contenedores.");
        }
    }
    // Endpoint para buscar inventario de conjuntos
    @GetMapping("/conjuntos")
    @ResponseBody
    public ResponseEntity<?> buscarInventario(@ModelAttribute ReporteConjuntoFiltroDTO filtro, HttpSession session) {
        // Validar contexto de sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar buscar inventario con el servicio de reporte
        try {
            List<ReporteConjuntoDTO> resultado = reporteService.buscarInventario(obtenerIdUsuario(session), obtenerIdBodega(session), filtro);
            return ResponseEntity.ok(resultado);
        } catch (ReporteNoAutorizadoException exception) {
            return respuestaError(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return respuestaError(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar el inventario.");
        }
    }
    // Endpoint para obtener el surtido histórico de una hoja
    @GetMapping("/hojas/{idHoja}/surtido")
    @ResponseBody
    public ResponseEntity<?> obtenerSurtido(@PathVariable Integer idHoja, HttpSession session) {
        // Validar contexto de sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar obtener el surtido histórico con el servicio de reporte
        try {
            ReporteOperacionHojaDTO resultado = reporteService.obtenerSurtido(obtenerIdUsuario(session), obtenerIdBodega(session), idHoja);
            return ResponseEntity.ok(resultado);
        } catch (ReporteNoAutorizadoException exception) {
            return respuestaError(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (ReporteRecursoNoEncontradoException exception) {
            return respuestaError(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return respuestaError(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar el surtido histórico.");
        }
    }
    // Endpoint para obtener la recepción histórica de una hoja
    @GetMapping("/hojas/{idHoja}/recepcion")
    @ResponseBody
    public ResponseEntity<?> obtenerRecepcion(@PathVariable Integer idHoja, HttpSession session) {
        // Validar contexto de sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar obtener la recepción histórica con el servicio de reporte
        try {
            ReporteOperacionHojaDTO resultado = reporteService.obtenerRecepcion(obtenerIdUsuario(session), obtenerIdBodega(session), idHoja);
            return ResponseEntity.ok(resultado);
        } catch (ReporteNoAutorizadoException exception) {
            return respuestaError(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (ReporteRecursoNoEncontradoException exception) {
            return respuestaError(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return respuestaError(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException exception) {
            return respuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible consultar la recepción histórica.");
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
    // Método privado para obtener el ID de bodega desde la sesión
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

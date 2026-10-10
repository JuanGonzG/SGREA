package dgtic.core.controller;

import dgtic.core.exception.ReporteNoAutorizadoException;
import dgtic.core.exception.ReporteRecursoNoEncontradoException;
import dgtic.core.model.dto.reporte.ReporteHojaDetalleDTO;
import dgtic.core.model.dto.reporte.ReporteOperacionHojaDTO;
import dgtic.core.service.ReportePdfService;
import dgtic.core.service.ReporteService;
import dgtic.core.service.SesionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/reportes")
public class ReportePdfController {
    private static final MediaType PDF_MEDIA_TYPE = MediaType.APPLICATION_PDF;

    private final ReporteService reporteService;
    private final ReportePdfService reportePdfService;
    private final SesionService sesionService;

    public ReportePdfController(
            ReporteService reporteService,
            ReportePdfService reportePdfService,
            SesionService sesionService) {
        this.reporteService = reporteService;
        this.reportePdfService = reportePdfService;
        this.sesionService = sesionService;
    }
    // Endpoint para exportar la hoja de trabajo en PDF
    @GetMapping("/hojas/{idHoja}/pdf")
    @ResponseBody
    public ResponseEntity<?> exportarHoja(@PathVariable Integer idHoja, HttpSession session) {
        // Validar que la sesión esté activa y tenga contexto operativo
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar obtener la hoja de trabajo y generar el PDF
        try {
            // Obtener los datos de la hoja de trabajo
            ReporteHojaDetalleDTO reporte = reporteService.obtenerHoja(idUsuario(session), idBodega(session), idHoja);
            // Generar el PDF de la hoja de trabajo
            byte[] archivo = reportePdfService.generarHoja(reporte, nombreBodega(session), nombreUsuario(session));
            // Devolver el PDF como respuesta
            return archivo(archivo, "SGREA_Hoja_" + idHoja + ".pdf");
        } catch (ReporteNoAutorizadoException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (ReporteRecursoNoEncontradoException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException | IllegalStateException exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible exportar la hoja.");
        }
    }
    // Endpoint para exportar el surtido histórico en PDF
    @GetMapping("/hojas/{idHoja}/surtido/pdf")
    @ResponseBody
    public ResponseEntity<?> exportarSurtido(@PathVariable Integer idHoja, HttpSession session) {
        // Llamar al método privado para exportar la operación de surtido
        return exportarOperacion(idHoja, session, true);
    }
    // Endpoint para exportar la recepción histórica en PDF
    @GetMapping("/hojas/{idHoja}/recepcion/pdf")
    @ResponseBody
    public ResponseEntity<?> exportarRecepcion(@PathVariable Integer idHoja, HttpSession session) {
        // Llamar al método privado para exportar la operación de recepción
        return exportarOperacion(idHoja, session, false);
    }
    // Método privado para exportar la operación de surtido o recepción
    private ResponseEntity<?> exportarOperacion(Integer idHoja, HttpSession session, boolean surtido) {
        // Validar que la sesión esté activa y tenga contexto operativo
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar obtener la operación y generar el PDF
        try {
            // Obtener los datos de la operación de surtido o recepción
            ReporteOperacionHojaDTO reporte = surtido
                    ? reporteService.obtenerSurtido(idUsuario(session), idBodega(session), idHoja)
                    : reporteService.obtenerRecepcion(idUsuario(session), idBodega(session), idHoja);
            // Generar el PDF de la operación de surtido o recepción
            byte[] archivo = surtido
                    ? reportePdfService.generarSurtido(reporte, nombreBodega(session), nombreUsuario(session))
                    : reportePdfService.generarRecepcion(reporte, nombreBodega(session), nombreUsuario(session));
            // Devolver el PDF como respuesta
            String sufijo = surtido ? "Surtido" : "Recepcion";
            // Devolver el PDF como respuesta
            return archivo(archivo, "SGREA_Hoja_" + idHoja + "_" + sufijo + ".pdf");
        } catch (ReporteNoAutorizadoException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (ReporteRecursoNoEncontradoException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException | IllegalStateException exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR,
                    surtido ? "No fue posible exportar el surtido histórico."
                            : "No fue posible exportar la recepción histórica.");
        }
    }
    // Método privado para construir la respuesta con el archivo PDF
    private ResponseEntity<byte[]> archivo(byte[] contenido, String nombre) {
        // Construir la respuesta con el archivo PDF y los encabezados adecuados
        return ResponseEntity.ok()
                .contentType(PDF_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombre + "\"")
                .body(contenido);
    }
    // Método privado para validar que la sesión esté activa y tenga contexto operativo
    private ResponseEntity<Map<String, String>> validarContexto(HttpSession session) {
        // Validar que la sesión esté activa
        if (!sesionService.isSesionActiva(session)) {
            return error(HttpStatus.UNAUTHORIZED, "La sesión ha expirado.");
        }
        // Validar que la sesión tenga contexto operativo
        if (idUsuario(session) == null || idBodega(session) == null) {
            return error(HttpStatus.UNAUTHORIZED, "La sesión no tiene contexto operativo.");
        }
        return null;
    }
    // Métodos privados para obtener el ID de usuario desde la sesión
    private Integer idUsuario(HttpSession session) {
        return (Integer) session.getAttribute("idUsuario");
    }
    // Método privado para obtener el ID de la bodega desde la sesión
    private Integer idBodega(HttpSession session) {
        return (Integer) session.getAttribute("idBodega");
    }
    // Método privado para obtener el nombre de la bodega desde la sesión
    private String nombreBodega(HttpSession session) {
        Object nombre = session.getAttribute("nombreBodega");
        // Si no se encuentra el nombre de la bodega, se devuelve el ID de la bodega como cadena
        return nombre == null ? String.valueOf(idBodega(session)) : String.valueOf(nombre);
    }
    // Método privado para obtener el nombre del usuario desde la sesión
    private String nombreUsuario(HttpSession session) {
        Object nombre = session.getAttribute("nombreCompletoUsuario");
        if (nombre == null) {
            nombre = session.getAttribute("nombreUsuario");
        }
        return nombre == null ? String.valueOf(idUsuario(session)) : String.valueOf(nombre);
    }
    // Método privado para construir la respuesta de error con un mensaje y un código de estado HTTP
    private ResponseEntity<Map<String, String>> error(HttpStatus status, String mensaje) {
        Map<String, String> cuerpo = new HashMap<>();
        cuerpo.put("mensaje", mensaje == null ? "La operación no es válida." : mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }
}

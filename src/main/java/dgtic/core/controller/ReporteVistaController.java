package dgtic.core.controller;

import dgtic.core.exception.ReporteNoAutorizadoException;
import dgtic.core.service.ReporteService;
import dgtic.core.service.SesionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reportes")
public class ReporteVistaController {
    private final ReporteService reporteService;
    private final SesionService sesionService;

    public ReporteVistaController(ReporteService reporteService, SesionService sesionService) {
        this.reporteService = reporteService;
        this.sesionService = sesionService;
    }
    // Endpoint para mostrar la página de inicio de reportes
    @GetMapping
    public Object inicio(HttpSession session) {
        // Validar que la sesión esté activa y tenga contexto operativo
        Object error = validarVista(session);
        return error == null ? "reportes/index" : error;
    }
    // Endpoint para mostrar la página de surtido de una hoja de trabajo
    @GetMapping("/hojas/{idHoja}/surtido")
    public Object mostrarSurtido(@PathVariable Integer idHoja, HttpSession session, Model model) {
        // Validar que la sesión esté activa y tenga contexto operativo
        Object error = validarVista(session);
        if (error != null) {
            return error;
        }
        // Agregar el ID de la hoja al modelo para que esté disponible en la vista
        model.addAttribute("idHoja", idHoja);
        return "reportes/surtido";
    }
    // Endpoint para mostrar la página de recepción de una hoja de trabajo
    @GetMapping("/hojas/{idHoja}/recepcion")
    public Object mostrarRecepcion(@PathVariable Integer idHoja, HttpSession session, Model model) {
        // Validar que la sesión esté activa y tenga contexto operativo
        Object error = validarVista(session);
        if (error != null) {
            return error;
        }
        // Agregar el ID de la hoja al modelo para que esté disponible en la vista
        model.addAttribute("idHoja", idHoja);
        return "reportes/recepcion";
    }
    // Método privado para validar la sesión y el acceso a los reportes
    private Object validarVista(HttpSession session) {
        // Validar que la sesión esté activa
        if (!sesionService.isSesionActiva(session)) {
            return "redirect:/login";
        }
        // Obtener el ID del usuario y de la bodega desde la sesión
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        Integer idBodega = (Integer) session.getAttribute("idBodega");
        // Validar que el ID del usuario y de la bodega no sean nulos
        if (idUsuario == null || idBodega == null) {
            return "redirect:/login";
        }
        // Intentar validar el acceso a los reportes para el usuario y la bodega
        try {
            reporteService.validarAccesoReportes(idUsuario, idBodega);
            return null;
        } catch (ReporteNoAutorizadoException exception) {
            return "error/error";
        }
    }
}

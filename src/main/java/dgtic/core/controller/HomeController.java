package dgtic.core.controller;

import dgtic.core.model.dto.monitoreo.MonitoreoHojaDTO;
import dgtic.core.service.MonitoreoService;
import dgtic.core.service.SesionService;
import dgtic.core.security.TokenCookieService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;

import java.util.List;

@Controller
public class HomeController {
    @Autowired
    private SesionService sesionService;

    @Autowired
    private MonitoreoService monitoreoService;

    @Autowired
    private TokenCookieService tokenCookieService;
    // Maneja la solicitud GET para la página de inicio
    @GetMapping("/")
    public String inicio(HttpSession session, Model model){
        // Verificar si hay una sesión activa en el servidor
        Boolean sesionActiva = sesionService.isSesionActiva(session);
        if (!sesionActiva) {
            return "redirect:/login";
        }

        // Obtener el rol del usuario desde la sesión
        Integer idRol = (Integer) session.getAttribute("idRol");
        // Determinar los permisos del usuario según su rol
        boolean esAdministrador = Integer.valueOf(1).equals(idRol);
        boolean esSupervisor = Integer.valueOf(2).equals(idRol);
        boolean esOperador = Integer.valueOf(3).equals(idRol);
        boolean puedeSupervisar = esAdministrador || esSupervisor;
        // Agregar los permisos al modelo para que estén disponibles en la vista
        model.addAttribute("esAdministrador", esAdministrador);
        model.addAttribute("esSupervisor", esSupervisor);
        model.addAttribute("esOperador", esOperador);
        model.addAttribute("puedeSupervisar", puedeSupervisar);
        // Si el usuario tiene permisos de supervisión, obtener los surtidos y recepciones activas
        if (puedeSupervisar) {
            // Obtener el idUsuario y idBodega desde la sesión
            Integer idUsuario = (Integer) session.getAttribute("idUsuario");
            Integer idBodega = (Integer) session.getAttribute("idBodega");
            // Obtener los surtidos y recepciones activas para el usuario y bodega
            List<MonitoreoHojaDTO> surtidosActivos = monitoreoService
                    .obtenerSurtidosActivos(idUsuario, idBodega);
            // Obtener las recepciones activas para el usuario y bodega
            List<MonitoreoHojaDTO> recepcionesActivas = monitoreoService
                    .obtenerRecepcionesActivas(idUsuario, idBodega);
            // Agregar los surtidos y recepciones activas al modelo para que estén disponibles en la vista
            model.addAttribute("surtidosActivos", surtidosActivos);
            model.addAttribute("recepcionesActivas", recepcionesActivas);
        } else {
            // Si el usuario no tiene permisos de supervisión, agregar listas vacías al modelo
            model.addAttribute("surtidosActivos", List.of());
            model.addAttribute("recepcionesActivas", List.of());
        }

        return "home/home";
    }
    // Maneja la solicitud POST para cerrar sesión
    @PostMapping("/logout")
    public String cerrarSesion(HttpSession session, HttpServletResponse response) {
        // Obtener el idUsuario y idSesion desde la sesión
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        Integer idSesion = (Integer) session.getAttribute("idSesion");
        // Si ambos valores son no nulos, cerrar la sesión en el servicio de sesiones
        if (idUsuario != null && idSesion != null) {
            sesionService.closeSesion(idUsuario, idSesion);
        }
        // Limpiar el contexto de seguridad y invalidar la sesión
        SecurityContextHolder.clearContext();
        session.invalidate();
        // Expirar las cookies de token JWT y Refresh Token
        tokenCookieService.expireTokenCookies(response);
        return "redirect:/login";
    }
}

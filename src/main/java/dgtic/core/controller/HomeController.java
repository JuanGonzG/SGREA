package dgtic.core.controller;

import dgtic.core.service.SesionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @Autowired
    private SesionService sesionService;

    @GetMapping("/")
    public String inicio(HttpSession session){
        // Verificar si hay una sesión activa en el servidor
        Boolean sesionActiva = sesionService.isSesionActiva(session);
        if (!sesionActiva) {
            return "redirect:/login";
        }

        return "home/home";
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        // Cerrar la sesión en la base de datos con los datos de la sesión actual
        Boolean sesionCerrada = sesionService.closeSesion((Integer) session.getAttribute("idUsuario"), (Integer) session.getAttribute("idSesion"), (String) session.getAttribute("token"));

        if (!sesionCerrada) {
            // Manejar el caso en que la sesión no se pudo cerrar correctamente
            // Por ejemplo, podrías redirigir a una página de error o mostrar un mensaje
            return "error/error"; // Asegúrate de tener una vista de error adecuada
        }

        // Eliminar los atributos de la sesión
        session.invalidate();
        return "redirect:/login";
    }
}

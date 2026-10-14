package dgtic.core.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class ErrorPageController {
    // Maneja la página de error 403 (Acceso denegado)
    @GetMapping("/error/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accesoDenegado() {
        return "error/403";
    }
    // Maneja la página de error 404 (Página no encontrada)
    @GetMapping("/error/404")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String paginaNoEncontrada() {
        return "error/404";
    }
}

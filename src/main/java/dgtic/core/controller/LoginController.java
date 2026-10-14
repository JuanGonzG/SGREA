package dgtic.core.controller;

import dgtic.core.model.dto.catalogo.BodegaDTO;
import dgtic.core.model.dto.seguridad.LoginDTO;
import dgtic.core.model.dto.catalogo.UsuarioDTO;
import dgtic.core.service.LoginService;
import dgtic.core.service.SesionService;
import dgtic.core.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class LoginController {
    @Autowired
    private LoginService loginService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SesionService sesionService;

    // Endpoint para mostrar la página de login
    @GetMapping("login")
    public String login(Model model, HttpSession session){
        // Validar si ya hay una sesión activa
        Boolean sesionActiva = sesionService.isSesionActiva(session);
        if (sesionActiva) {
            return "redirect:/";
        }
        // Limpiar cualquier contexto pendiente de login
        loginService.limpiarContextoPendiente(session);
        model.addAttribute("usuario", new LoginDTO());
        return "login/login";
    }

    // Endpoint para procesar el login
    @PostMapping("login-bodega")
    public String postLogin(@ModelAttribute("usuario") LoginDTO usuario,
                            HttpServletRequest request,
                            HttpServletResponse response){
        // Limpiar cualquier contexto pendiente de login
        loginService.limpiarContextoPendiente(request.getSession());
        // Intentar autenticar al usuario
        UsuarioDTO usuarioLogin;
        try {
            usuarioLogin = loginService.login(usuario.getUsername(), usuario.getPassword());
        } catch (DisabledException exception) {
            request.setAttribute("error", "Usuario inactivo");
            return "login/login";
        } catch (AuthenticationException exception) {
            request.setAttribute("error", "Usuario o contraseña incorrectos");
            return "login/login";
        }

        // Consultar bodegas
        List<BodegaDTO> bodegas = usuarioService.getBodegasByUsuarioID(usuarioLogin.getIdUsuario());
        // Guardar el contexto pendiente de login en la sesión
        loginService.guardarContextoPendiente(request.getSession(), usuarioLogin);
        // Validar la cantidad de bodegas asociadas al usuario
        if(bodegas.isEmpty()){
            request.setAttribute("error", "Usuario no tiene bodegas asociadas");
            return "login/login";
        } else if (bodegas.size() == 1) {
            // Si tiene una sola bodega, finalizar el login automáticamente
            if (!loginService.finalizarLogin(request.getSession(), bodegas.get(0).getIdBodega(), response)) {
                request.setAttribute("error", "No fue posible seleccionar la bodega");
                return "login/login";
            }
            return "redirect:/";
        } else {
            // Si tiene más de una bodega, mostrar la lista de bodegas
            request.setAttribute("bodegas", bodegas);
            usuario.setUsername(usuarioLogin.getUsername());
            request.setAttribute("usuario", usuario);
            return "login/login";
        }
    }

    // Endpoint para procesar la selección de bodega y finalizar el login
    @PostMapping("login-enter")
    public String postLoginEnter(@ModelAttribute("usuario") LoginDTO usuario,
                                 Model model,
                                 HttpSession session,
                                 HttpServletResponse response){
        // Obtener el idUsuario y username desde la sesión
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        String username = (String) session.getAttribute("username");
        // Validar que el idUsuario y username no sean nulos
        if (idUsuario == null || username == null) {
            return "redirect:/login";
        }

        // Validar selección de bodega
        if(usuario.getBodega() == null){
            model.addAttribute("error", "Debe seleccionar una bodega");
            model.addAttribute("bodegas", usuarioService.getBodegasByUsuarioID(idUsuario));
            return "login/login";
        }
        // Finalizar el login con la bodega seleccionada
        if (!loginService.finalizarLogin(session, usuario.getBodega(), response)) {
            model.addAttribute("error", "Bodega no autorizada");
            model.addAttribute("bodegas", usuarioService.getBodegasByUsuarioID(idUsuario));
            return "login/login";
        }

        return "redirect:/";
    }

}

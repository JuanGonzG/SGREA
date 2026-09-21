package dgtic.core.controller;

import dgtic.core.model.dto.BodegaDTO;
import dgtic.core.model.dto.LoginDTO;
import dgtic.core.model.dto.SesionDTO;
import dgtic.core.model.dto.UsuarioDTO;
import dgtic.core.service.LoginService;
import dgtic.core.service.SesionService;
import dgtic.core.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

        model.addAttribute("usuario", new LoginDTO());
        return "login/login";
    }

    // Endpoint para procesar el login
    @PostMapping("login-bodega")
    public String postLogin(@ModelAttribute("usuario") LoginDTO usuario, HttpServletRequest request){
        // Validar el usuario y la contraseña en DB
        UsuarioDTO usuarioLogin = loginService.login(usuario.getUsername(), usuario.getPassword());
        if (usuarioLogin == null) {
            request.setAttribute("error", "Usuario o contraseña incorrectos");
            return "login/login";
        }

        // Validar si el usuario está activo
        if (!usuarioLogin.getActivo()) {
            request.setAttribute("error", "Usuario inactivo");
            return "login/login";
        }

        // Consultar bodegas
        List<BodegaDTO> bodegas = usuarioService.getBodegasByUsuarioID(usuarioLogin.getIdUsuario());
        if(bodegas.isEmpty()){
            request.setAttribute("error", "Usuario no tiene bodegas asociadas");
            return "login/login";
        } else if (bodegas.size() == 1) {
            // Si solo tiene una bodega, redirigir al endpoint de creación de sesión
            usuario.setBodega(bodegas.get(0).getIdBodega());
            usuario.setIdUsuario(usuarioLogin.getIdUsuario());
            request.setAttribute("usuario", usuario);
            return "forward:/login/crear-sesion";
        } else {
            // Si tiene más de una bodega, mostrar la lista de bodegas
            request.setAttribute("bodegas", bodegas);
            usuario.setIdUsuario(usuarioLogin.getIdUsuario());
            request.setAttribute("usuario", usuario);
            return "login/login";
        }
    }

    // Endpoint para procesar la selección de bodega y crear la sesión
    @PostMapping("login-enter")
    public String postLoginEnter(@ModelAttribute("usuario") LoginDTO usuario, Model model){
        // Validar selección de bodega
        if(usuario.getBodega() == null){
            model.addAttribute("error", "Debe seleccionar una bodega");
            return "login/login";
        }

        return "forward:/login/crear-sesion";
    }

    // Endpoint para crear la sesión
    @PostMapping("login/crear-sesion")
    public String crearSesion(@RequestAttribute("usuario") LoginDTO usuario, HttpSession session){
        // Crear sesión en DB
        Optional<SesionDTO> sesion = Optional.ofNullable(sesionService.addSesion(usuario.getIdUsuario(), usuario.getBodega(), UUID.randomUUID().toString()));

        //Validar si la sesión se creó correctamente
        if(!sesion.isPresent()){
            return "redirect:/login";
        } else {
            // Obtener el usuario por username
            UsuarioDTO usuarioDTO = usuarioService.getUsuarioByUsername(usuario.getUsername());

            // Guardar la sesión en el HttpSession
            session.setAttribute("idSesion", sesion.get().getIdSesion());
            session.setAttribute("idUsuario", sesion.get().getUsuario().getIdUsuario());
            session.setAttribute("idBodega", sesion.get().getBodega().getIdBodega());
            session.setAttribute("nombreBodega", sesion.get().getBodega().getNombre());
            session.setAttribute("token", sesion.get().getToken());

            // Guardar el usuario en el HttpSession
            session.setAttribute("nombreUsuario", usuarioDTO.getNombre().split(" ")[0]);
            session.setAttribute("nombreCompletoUsuario", usuarioDTO.getNombre());
            session.setAttribute("rolUsuario", usuarioDTO.getRol().getNombre());
            session.setAttribute("idRol", usuarioDTO.getRol().getIdRol());

            return "redirect:/";
        }
    }
}

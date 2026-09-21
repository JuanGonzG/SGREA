package dgtic.core.controller;

import dgtic.core.service.SesionService;
import dgtic.core.model.dto.UsuarioAltaDTO;
import dgtic.core.model.dto.UsuarioDTO;
import dgtic.core.model.dto.UsuarioEdicionDTO;
import dgtic.core.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UsuarioController {
    @Autowired
    private SesionService sesionService;

    @Autowired
    private UsuarioService usuarioService;

    // Muestra la vista de usuarios
    @GetMapping("/usuarios")
    public String usuario(HttpSession session) {
        // Verifica si la sesión está activa
        if (!sesionService.isSesionActiva(session)) {
            return "redirect:/login";
        }
        return "usuario/usuario";
    }

    // Endpoint para listar todos los usuarios, retornando un ResponseEntity en JSON
    @GetMapping("/usuarios/listar")
    @ResponseBody
    public ResponseEntity<?> listar(HttpSession session) {
        // Verifica si la sesión está activa
        if (!sesionService.isSesionActiva(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        // Devuelve la lista de usuarios
        return ResponseEntity.ok(usuarioService.getUsuarios());
    }

    // Endpoint para obtener un usuario por su ID, retornando un ResponseEntity en JSON
    @GetMapping("/usuarios/get/{idUsuario}")
    @ResponseBody
    public ResponseEntity<UsuarioDTO> obtener(HttpSession session, @PathVariable Integer idUsuario) {
        // Verifica si la sesión está activa
        if (!sesionService.isSesionActiva(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        // Devuelve el usuario correspondiente
        return usuarioService.getUsuarioById(idUsuario).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint para obtener las bodegas disponibles, retornando un ResponseEntity en JSON
    @GetMapping("/usuarios/bodegas")
    @ResponseBody
    public ResponseEntity<?> catalogoBodegas(HttpSession session) {
        // Verifica si la sesión está activa
        if (!sesionService.isSesionActiva(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        // Devuelve la lista de bodegas
        return ResponseEntity.ok(usuarioService.getBodegas());
    }
    // Endpoint para obtener las bodegas asociadas a un usuario, retornando un ResponseEntity en JSON
    @GetMapping("/usuarios/{idUsuario}/bodegas")
    @ResponseBody
    public ResponseEntity<?> bodegas(HttpSession session, @PathVariable Integer idUsuario) {
        // Verifica si la sesión está activa
        if (!sesionService.isSesionActiva(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(usuarioService.getBodegasConEstado(idUsuario));
        } catch (IllegalArgumentException ex) {
            return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    // Endpoint para actualizar las bodegas asociadas a un usuario, retornando un ResponseEntity con un mensaje en JSON
    @PostMapping("/usuarios/{idUsuario}/bodegas")
    @ResponseBody
    public ResponseEntity<Map<String, String>> actualizarBodegas(HttpSession session,
                                                                  @PathVariable Integer idUsuario,
                                                                  @RequestBody List<Integer> idsBodegas) {
        // Verifica si la sesión está activa
        if (!sesionService.isSesionActiva(session)) return respuesta(HttpStatus.UNAUTHORIZED, "La sesión ha expirado.");
        try {
            // Intenta actualizar las bodegas asociadas al usuario
            usuarioService.actualizarBodegas(idUsuario, idsBodegas);
            return respuesta(HttpStatus.OK, "Bodegas asociadas actualizadas correctamente.");
        } catch (IllegalArgumentException ex) {
            return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (IllegalStateException ex) {
            return respuesta(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    // Endpoint para guardar un nuevo usuario, retornando un ResponseEntity con un mensaje en JSON
    @PostMapping("/usuarios/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardar(HttpSession session, @Valid @RequestBody UsuarioAltaDTO usuario) {
        // Verifica si la sesión está activa
        if (!sesionService.isSesionActiva(session)) return respuesta(HttpStatus.UNAUTHORIZED, "La sesión ha expirado.");
        try {
            // Intenta guardar el usuario
            usuarioService.crear(usuario);
            return respuesta(HttpStatus.OK, "Usuario guardado correctamente.");
        } catch (IllegalArgumentException ex) {
            return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    // Endpoint para actualizar un usuario existente, retornando un ResponseEntity con un mensaje en JSON
    @PostMapping("/usuarios/actualizar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> actualizar(HttpSession session, @Valid @RequestBody UsuarioEdicionDTO usuario) {
        // Verifica si la sesión está activa
        if (!sesionService.isSesionActiva(session)) return respuesta(HttpStatus.UNAUTHORIZED, "La sesión ha expirado.");
        try {
            // Intenta actualizar el usuario
            usuarioService.actualizar(usuario);
            return respuesta(HttpStatus.OK, "Usuario actualizado correctamente.");
        } catch (IllegalArgumentException ex) {
            return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    // Endpoint para eliminar un usuario por su ID, retornando un ResponseEntity con un mensaje en JSON
    @DeleteMapping("/usuarios/eliminar/{idUsuario}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminar(HttpSession session, @PathVariable Integer idUsuario) {
        // Verifica si la sesión está activa
        if (!sesionService.isSesionActiva(session)) return respuesta(HttpStatus.UNAUTHORIZED, "La sesión ha expirado.");
        try {
            // Intenta eliminar el usuario
            usuarioService.eliminar(idUsuario);
            return respuesta(HttpStatus.OK, "Usuario eliminado correctamente.");
        } catch (IllegalArgumentException ex) {
            return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (IllegalStateException ex) {
            return respuesta(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    // Metodo general para construir una respuesta con un mensaje y un código de estado HTTP
    private ResponseEntity<Map<String, String>> respuesta(HttpStatus status, String mensaje) {
        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(respuesta);
    }
}

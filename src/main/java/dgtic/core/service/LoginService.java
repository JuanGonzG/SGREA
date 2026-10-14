package dgtic.core.service;

import dgtic.core.model.dto.catalogo.UsuarioDTO;
import dgtic.core.model.dto.seguridad.SesionDTO;
import dgtic.core.repository.UsuarioBodegaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import dgtic.core.security.SgreaUserDetails;
import dgtic.core.security.JWTTokenProvider;
import dgtic.core.security.TokenCookieService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpSession;

import java.util.UUID;

@Service
@Slf4j
public class LoginService {
    private final AuthenticationManager authenticationManager;
    private final UsuarioBodegaRepository usuarioBodegaRepository;
    private final SesionService sesionService;
    private final UsuarioService usuarioService;
    private final JWTTokenProvider jwtTokenProvider;
    private final TokenCookieService tokenCookieService;

    @Autowired
    public LoginService(AuthenticationManager authenticationManager,
                        UsuarioBodegaRepository usuarioBodegaRepository,
                        SesionService sesionService,
                        UsuarioService usuarioService,
                        JWTTokenProvider jwtTokenProvider,
                        TokenCookieService tokenCookieService) {
        this.authenticationManager = authenticationManager;
        this.usuarioBodegaRepository = usuarioBodegaRepository;
        this.sesionService = sesionService;
        this.usuarioService = usuarioService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenCookieService = tokenCookieService;
    }
    // Método para autenticar al usuario y devolver un objeto UsuarioDTO con la información del usuario autenticado
    public UsuarioDTO login(String username, String passwordIngresada) {
        try {
            // Intentar autenticar al usuario con el AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, passwordIngresada));
            // Verificar que el principal de autenticación sea una instancia de SgreaUserDetails
            if (!(authentication.getPrincipal() instanceof SgreaUserDetails userDetails)) {
                throw new AuthenticationServiceException("Principal de autenticación no reconocido");
            }
            // Construir y devolver un objeto UsuarioDTO con la información del usuario autenticado
            return UsuarioDTO.builder()
                    .idUsuario(userDetails.getIdUsuario())
                    .username(userDetails.getUsername())
                    .activo(userDetails.isEnabled())
                    .build();
        } catch (AuthenticationServiceException exception) {
            log.error("Error inesperado durante la autenticación username={}", username, exception);
            throw exception;
        } catch (DisabledException exception) {
            log.warn("Intento de login de usuario inactivo username={}", username);
            throw exception;
        } catch (AuthenticationException exception) {
            log.warn("Intento de login inválido username={}", username);
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Error inesperado durante la autenticación username={}", username, exception);
            throw exception;
        }
    }
    // Método para guardar el contexto pendiente en la sesión HTTP
    public void guardarContextoPendiente(HttpSession session, UsuarioDTO usuario) {
        // Guardar el ID del usuario y el nombre de usuario en la sesión HTTP
        session.setAttribute("idUsuario", usuario.getIdUsuario());
        session.setAttribute("username", usuario.getUsername());
    }
    // Método para limpiar el contexto pendiente de la sesión HTTP si no hay un ID de sesión presente
    public void limpiarContextoPendiente(HttpSession session) {
        // Eliminar el ID del usuario y el nombre de usuario de la sesión HTTP
        if (session.getAttribute("idSesion") == null) {
            session.removeAttribute("idUsuario");
            session.removeAttribute("username");
        }
    }
    // Método para finalizar el login, crear la sesión y generar los tokens de acceso y actualización
    public boolean finalizarLogin(HttpSession session, Integer idBodega, HttpServletResponse response) {
        // Obtener el ID del usuario y el nombre de usuario de la sesión HTTP
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        String username = (String) session.getAttribute("username");
        // Verificar que el contexto de la sesión HTTP esté completo
        if (idUsuario == null || username == null || idBodega == null) {
            log.warn("Finalización de login rechazada por contexto incompleto idUsuario={}, idBodega={}",
                    idUsuario, idBodega);
            return false;
        }
        // Verificar que el usuario tenga autorización para acceder a la bodega seleccionada
        if (!usuarioBodegaRepository.existsByUsuario_IdUsuarioAndBodega_IdBodega(idUsuario, idBodega)) {
            log.warn("Selección de bodega rechazada por falta de autorización idUsuario={}, idBodega={}",
                    idUsuario, idBodega);
            return false;
        }
        // Intentar crear la sesión y generar los tokens de acceso y actualización
        try {
            // Generar un identificador único para el token de actualización
            String refreshJti = UUID.randomUUID().toString();
            // Crear una nueva sesión en el servicio de sesiones
            SesionDTO sesion = sesionService.addSesion(idUsuario, idBodega, refreshJti);
            // Obtener la información del usuario autenticado
            UsuarioDTO usuario = usuarioService.getUsuarioByUsername(username);
            // Guardar la información de la sesión y del usuario en la sesión HTTP
            session.setAttribute("idSesion", sesion.getIdSesion());
            session.setAttribute("idBodega", sesion.getBodega().getIdBodega());
            session.setAttribute("nombreBodega", sesion.getBodega().getNombre());
            session.setAttribute("token", sesion.getToken());
            session.setAttribute("nombreUsuario", usuario.getNombre().split(" ")[0]);
            session.setAttribute("nombreCompletoUsuario", usuario.getNombre());
            session.setAttribute("rolUsuario", usuario.getRol().getNombre());
            session.setAttribute("idRol", usuario.getRol().getIdRol());
            // Generar los tokens de acceso y actualización utilizando el proveedor de tokens JWT
            String accessToken = jwtTokenProvider.generateAccessToken(
                    usuario.getUsername(),
                    idUsuario,
                    sesion.getIdSesion(),
                    sesion.getBodega().getIdBodega(),
                    resolveAuthority(usuario.getRol().getIdRol()));
            String refreshToken = jwtTokenProvider.generateRefreshToken(
                    usuario.getUsername(),
                    idUsuario,
                    sesion.getIdSesion(),
                    sesion.getBodega().getIdBodega(),
                    refreshJti,
                    resolveAuthority(usuario.getRol().getIdRol()));
            // Escribir los tokens en las cookies de la respuesta HTTP
            tokenCookieService.writeTokenCookies(response, accessToken, refreshToken);

            log.info("Bodega seleccionada y login SGREA completado idUsuario={}, idBodega={}",
                    idUsuario, idBodega);
            return true;
        } catch (RuntimeException exception) {
            log.error("Error inesperado al finalizar login idUsuario={}, idBodega={}",
                    idUsuario, idBodega, exception);
            throw exception;
        }
    }
    // Método privado para resolver la autoridad del usuario según su ID de rol
    private String resolveAuthority(Integer idRol) {
        return switch (idRol == null ? 0 : idRol) {
            case 1 -> "ROLE_ADMINISTRADOR";
            case 2 -> "ROLE_SUPERVISOR";
            case 3 -> "ROLE_OPERADOR";
            default -> throw new IllegalStateException("Rol SGREA no reconocido");
        };
    }
}

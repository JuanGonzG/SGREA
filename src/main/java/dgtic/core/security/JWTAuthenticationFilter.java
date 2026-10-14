package dgtic.core.security;

import dgtic.core.model.entity.SesionEntity;
import dgtic.core.repository.SesionRepository;
import dgtic.core.service.RefreshTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private final JWTTokenProvider jwtTokenProvider;
    private final SgreaUserDetailsService userDetailsService;
    private final SesionRepository sesionRepository;
    private final RefreshTokenService refreshTokenService;
    private final TokenCookieService tokenCookieService;

    public JWTAuthenticationFilter(JWTTokenProvider jwtTokenProvider,
                                   SgreaUserDetailsService userDetailsService,
                                   SesionRepository sesionRepository,
                                   RefreshTokenService refreshTokenService,
                                   TokenCookieService tokenCookieService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.sesionRepository = sesionRepository;
        this.refreshTokenService = refreshTokenService;
        this.tokenCookieService = tokenCookieService;
    }
    // Método para determinar si se debe omitir el filtro para ciertas rutas
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Omitir el filtro para rutas de login, refresh, error y recursos estáticos
        String path = request.getServletPath();
        return "/login".equals(path)
                || "/login-bodega".equals(path)
                || "/login-enter".equals(path)
                || "/refresh".equals(path)
                || "/error".equals(path)
                || "/favicon.ico".equals(path)
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/img/")
                || path.startsWith("/uploads/productos/");
    }
    // Método principal del filtro que se ejecuta para cada solicitud HTTP
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Intentar autenticar usando el token de acceso
        String accessToken = findCookie(request, "token");
        if (accessToken != null && authenticateAccessToken(accessToken, request)) {
            filterChain.doFilter(request, response);
            return;
        }
        // Si el token de acceso no es válido, intentar rotar el token de actualización
        String refreshToken = findCookie(request, "refresh_token");
        // Si el token de actualización es válido, rotar y autenticar con el nuevo token de acceso
        if (refreshToken != null) {
            // Intentar rotar el token de actualización y obtener un nuevo par de tokens
            RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotate(refreshToken);
            // Si se obtiene un nuevo par de tokens y el token de acceso es válido
            if (tokenPair != null && authenticateAccessToken(tokenPair.accessToken(), request)) {
                // Escribir las cookies con los nuevos tokens
                tokenCookieService.writeTokenCookies(
                        response, tokenPair.accessToken(), tokenPair.refreshToken());
                log.info("Access Token renovado automáticamente request={}", request.getRequestURI());
                // Continuar con la cadena de filtros
                filterChain.doFilter(request, response);
                return;
            }
        }
        // Limpiar el contexto de seguridad si no se pudo autenticar
        SecurityContextHolder.clearContext();
        // Continuar con la cadena de filtros sin autenticación
        filterChain.doFilter(request, response);
    }
    // Método para autenticar el token de acceso y establecer la autenticación en el contexto de seguridad
    private boolean authenticateAccessToken(String token, HttpServletRequest request) {
        try {
            // Validar el token de acceso usando el proveedor de tokens JWT
            if (!jwtTokenProvider.validateAccessToken(token)) {
                log.warn("Access JWT inválido o expirado en request={}", request.getRequestURI());
                return false;
            }
            // Extraer los claims del token de acceso
            Claims claims = jwtTokenProvider.extractClaims(token);
            String username = claims.getSubject();
            Integer idUsuario = claims.get("uid", Integer.class);
            Integer idSesion = claims.get("sid", Integer.class);
            Integer idBodega = claims.get("bid", Integer.class);
            // Verificar que los claims necesarios estén presentes
            if (username == null || idUsuario == null || idSesion == null || idBodega == null) {
                log.warn("Access JWT rechazado por claims incompletos request={}", request.getRequestURI());
                return false;
            }
            // Verificar que la sesión esté activa y que los datos de la sesión coincidan con los claims del token
            SesionEntity sesion = sesionRepository.findWithUsuarioAndBodegaByIdSesion(idSesion).orElse(null);
            if (sesion == null || !Boolean.TRUE.equals(sesion.getActiva())) {
                log.warn("Access JWT rechazado por sesión inactiva idSesion={}", idSesion);
                return false;
            }
            // Verificar que los datos de la sesión coincidan con los claims del token
            if (sesion.getUsuario() == null
                    || !idUsuario.equals(sesion.getUsuario().getIdUsuario())
                    || !username.equalsIgnoreCase(sesion.getUsuario().getUsername())
                    || sesion.getBodega() == null
                    || !idBodega.equals(sesion.getBodega().getIdBodega())) {
                log.warn("Access JWT rechazado por contexto de sesión inconsistente idSesion={}", idSesion);
                return false;
            }
            // Cargar los detalles del usuario y verificar que esté habilitado y que el idUsuario coincida
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!(userDetails instanceof SgreaUserDetails sgreaUserDetails)
                    || !sgreaUserDetails.isEnabled()
                    || !idUsuario.equals(sgreaUserDetails.getIdUsuario())) {
                log.warn("Access JWT rechazado por usuario inválido o inactivo idUsuario={}", idUsuario);
                return false;
            }
            // Construir el objeto de autenticación y establecerlo en el contexto de seguridad
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // Establecer la autenticación en el contexto de seguridad
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return true;
        } catch (UsernameNotFoundException | IllegalArgumentException | IllegalStateException exception) {
            log.warn("Access JWT rechazado durante la reconstrucción de autenticación request={}",
                    request.getRequestURI());
        } catch (RuntimeException exception) {
            log.error("Error inesperado al reconstruir autenticación JWT request={}",
                    request.getRequestURI(), exception);
        }
        return false;
    }
    // Método auxiliar para encontrar el valor de una cookie por su nombre en la solicitud HTTP
    private String findCookie(HttpServletRequest request, String name) {
        // Obtener todas las cookies de la solicitud
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        // Iterar sobre las cookies y buscar la cookie con el nombre especificado
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

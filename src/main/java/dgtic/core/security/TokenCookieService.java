package dgtic.core.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenCookieService {

    private final JWTTokenProvider jwtTokenProvider;
    private final boolean jwtCookieSecure;
    // Constructor que recibe el proveedor de tokens JWT y la configuración de seguridad de cookies
    public TokenCookieService(
            JWTTokenProvider jwtTokenProvider,
            @Value("${app.security.jwt.cookie-secure:false}") boolean jwtCookieSecure) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtCookieSecure = jwtCookieSecure;
    }
    // Método para escribir las cookies de token en la respuesta HTTP
    public void writeTokenCookies(HttpServletResponse response,
                                  String accessToken,
                                  String refreshToken) {
        // Agregar la cookie de token de acceso a la respuesta
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                "token", accessToken, jwtTokenProvider.getAccessTokenExpirationMs()));
        // Agregar la cookie de token de actualización a la respuesta
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                "refresh_token", refreshToken, jwtTokenProvider.getRefreshTokenExpirationMs()));
    }
    // Método para expirar las cookies de token en la respuesta HTTP
    public void expireTokenCookies(HttpServletResponse response) {
        // Agregar la cookie de token de acceso con valor vacío y tiempo de vida 0 para expirar
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("token", "", 0));
        // Agregar la cookie de token de actualización con valor vacío y tiempo de vida 0 para expirar
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("refresh_token", "", 0));
    }
    // Método privado para construir una cookie con los parámetros especificados
    private String buildCookie(String name, String value, long maxAgeMs) {
        return ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(true)
                .secure(jwtCookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMillis(maxAgeMs))
                .build()
                .toString();
    }
}

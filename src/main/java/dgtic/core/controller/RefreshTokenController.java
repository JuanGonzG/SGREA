package dgtic.core.controller;

import dgtic.core.security.TokenCookieService;
import dgtic.core.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;
    private final TokenCookieService tokenCookieService;

    public RefreshTokenController(RefreshTokenService refreshTokenService,
                                  TokenCookieService tokenCookieService) {
        this.refreshTokenService = refreshTokenService;
        this.tokenCookieService = tokenCookieService;
    }
    // Endpoint para renovar el Access Token utilizando el Refresh Token
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        // Intentar renovar el Access Token utilizando el Refresh Token
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotate(refreshToken);
        // Si el Refresh Token es inválido, devolver un error 401 (Unauthorized)
        if (tokenPair == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh Token inválido"));
        }
        // Escribir los nuevos tokens en las cookies de la respuesta
        tokenCookieService.writeTokenCookies(
                response, tokenPair.accessToken(), tokenPair.refreshToken());
        return ResponseEntity.ok(Map.of("mensaje", "Access Token renovado"));
    }
}

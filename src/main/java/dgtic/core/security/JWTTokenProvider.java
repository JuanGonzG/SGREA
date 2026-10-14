package dgtic.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTTokenProvider {

    private final String secret;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    // Constructor que recibe los valores de configuración desde application.properties o application.yml
    public JWTTokenProvider(
            @Value("${app.security.jwt.secret:}") String secret,
            @Value("${app.security.jwt.access-expiration-ms:900000}") long accessTokenExpirationMs,
            @Value("${app.security.jwt.refresh-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.secret = secret;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }
    // Método para generar un Access Token JWT
    public String generateAccessToken(
            String username,
            Integer idUsuario,
            Integer idSesion,
            Integer idBodega,
            String role) {
        // Validar los datos necesarios para generar el token
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El username es obligatorio");
        }
        if (idUsuario == null || idSesion == null || idBodega == null) {
            throw new IllegalArgumentException("Los identificadores del Access Token son obligatorios");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("La authority del Access Token es obligatoria");
        }
        // Validar que la expiración del token sea positiva
        if (accessTokenExpirationMs <= 0) {
            throw new IllegalStateException("La expiración del Access Token debe ser positiva");
        }
        // Calcular las fechas de emisión y expiración del token
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + accessTokenExpirationMs);
        // Construir y firmar el token JWT con los claims necesarios
        return Jwts.builder()
                .header()
                    .add("typ", "access")
                .and()
                .subject(username)
                .claim("uid", idUsuario)
                .claim("sid", idSesion)
                .claim("bid", idBodega)
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }
    // Método para obtener la expiración del Access Token en milisegundos
    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    // Método para generar un Refresh Token JWT
    public String generateRefreshToken(
            String username,
            Integer idUsuario,
            Integer idSesion,
            Integer idBodega,
            String jti,
            String role) {
        // Validar los datos necesarios para generar el token
        validateTokenData(username, idUsuario, idSesion, idBodega, role);
        // Validar que el jti (identificador único del token) no sea nulo o vacío
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("El jti del Refresh Token es obligatorio");
        }
        // Validar que la expiración del token sea positiva
        if (refreshTokenExpirationMs <= 0) {
            throw new IllegalStateException("La expiración del Refresh Token debe ser positiva");
        }
        // Calcular las fechas de emisión y expiración del token
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + refreshTokenExpirationMs);
        // Construir y firmar el token JWT con los claims necesarios
        return Jwts.builder()
                .header()
                    .add("typ", "refresh")
                .and()
                .id(jti)
                .subject(username)
                .claim("uid", idUsuario)
                .claim("sid", idSesion)
                .claim("bid", idBodega)
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }
    // Método para validar un Refresh Token JWT
    public boolean validateRefreshToken(String token) {
        // Validar que el token no sea nulo o vacío
        if (token == null || token.isBlank()) {
            return false;
        }
        // Intentar parsear y validar el token JWT
        try {
            Jws<Claims> parsed = parseSignedClaims(token);
            return "refresh".equals(parsed.getHeader().getType())
                    && parsed.getPayload().getId() != null;
        } catch (JwtException | IllegalArgumentException | IllegalStateException exception) {
            return false;
        }
    }
    // Método para obtener la expiración del Refresh Token en milisegundos
    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
    // Método para validar un Access Token JWT
    public boolean validateAccessToken(String token) {
        // Validar que el token no sea nulo o vacío
        if (token == null || token.isBlank()) {
            return false;
        }
        // Intentar parsear y validar el token JWT
        try {
            Jws<Claims> parsed = parseSignedClaims(token);
            return "access".equals(parsed.getHeader().getType());
        } catch (JwtException | IllegalArgumentException | IllegalStateException exception) {
            return false;
        }
    }
    // Método para extraer los claims de un token JWT
    public Claims extractClaims(String token) {
        return parseSignedClaims(token).getPayload();
    }
    // Método para extraer el username de un token JWT
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }
    // Método para extraer el idUsuario de un token JWT
    public Integer extractIdUsuario(String token) {
        return extractClaims(token).get("uid", Integer.class);
    }
    // Método para extraer el idSesion de un token JWT
    public Integer extractIdSesion(String token) {
        return extractClaims(token).get("sid", Integer.class);
    }
    // Método para extraer el idBodega de un token JWT
    public Integer extractIdBodega(String token) {
        return extractClaims(token).get("bid", Integer.class);
    }
    // Método para extraer el role de un token JWT
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }
    // Método para extraer el jti (identificador único del token) de un token JWT
    public String extractJti(String token) {
        return extractClaims(token).getId();
    }
    // Método privado para validar los datos necesarios para generar un token JWT
    private void validateTokenData(String username,
                                   Integer idUsuario,
                                   Integer idSesion,
                                   Integer idBodega,
                                   String role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El username es obligatorio");
        }
        if (idUsuario == null || idSesion == null || idBodega == null) {
            throw new IllegalArgumentException("Los identificadores del token son obligatorios");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("La authority del token es obligatoria");
        }
    }
    // Método privado para parsear y validar los claims de un token JWT firmado
    private Jws<Claims> parseSignedClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token);
    }
    // Método privado para obtener la clave de firma a partir del secreto configurado
    private SecretKey signingKey() {
        // Validar que el secreto no sea nulo o vacío
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Configure SGREA_JWT_SECRET para usar JWT");
        }
        // Convertir el secreto a bytes usando UTF-8
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Validar que el secreto tenga al menos 32 bytes para la seguridad de la firma
        if (secretBytes.length < 32) {
            throw new IllegalStateException("SGREA_JWT_SECRET debe tener al menos 32 bytes");
        }
        // Crear y devolver la clave de firma HMAC-SHA a partir del secreto
        return Keys.hmacShaKeyFor(secretBytes);
    }
}

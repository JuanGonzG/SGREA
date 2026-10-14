package dgtic.core.service;

import dgtic.core.model.entity.SesionEntity;
import dgtic.core.repository.SesionRepository;
import dgtic.core.security.JWTTokenProvider;
import dgtic.core.security.SgreaUserDetails;
import dgtic.core.security.SgreaUserDetailsService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class RefreshTokenService {

    private final JWTTokenProvider jwtTokenProvider;
    private final SesionRepository sesionRepository;
    private final SgreaUserDetailsService userDetailsService;

    public RefreshTokenService(JWTTokenProvider jwtTokenProvider,
                               SesionRepository sesionRepository,
                               SgreaUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sesionRepository = sesionRepository;
        this.userDetailsService = userDetailsService;
    }
    // Método para rotar el refresh token y generar un nuevo par de tokens (access token y refresh token)
    @Transactional
    public TokenPair rotate(String refreshToken) {
        // Validar el refresh token recibido
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            log.warn("Refresh Token inválido o expirado");
            return null;
        }
        // Extraer los claims del refresh token y obtener la información necesaria para la rotación
        try {
            Claims claims = jwtTokenProvider.extractClaims(refreshToken);
            String username = claims.getSubject();
            Integer idUsuario = claims.get("uid", Integer.class);
            Integer idSesion = claims.get("sid", Integer.class);
            Integer idBodega = claims.get("bid", Integer.class);
            String jti = claims.getId();
            // Validar la sesión asociada al refresh token y verificar que sea válida y activa
            SesionEntity sesion = sesionRepository.findWithUsuarioAndBodegaByIdSesion(idSesion).orElse(null);
            if (sesion == null || !Boolean.TRUE.equals(sesion.getActiva())
                    || sesion.getUsuario() == null
                    || sesion.getBodega() == null
                    || !idUsuario.equals(sesion.getUsuario().getIdUsuario())
                    || !idBodega.equals(sesion.getBodega().getIdBodega())
                    || !jti.equals(sesion.getToken())) {
                log.warn("Refresh Token rechazado por sesión o jti no válidos idSesion={}", idSesion);
                return null;
            }

            // Cargar los detalles del usuario y validar que esté habilitado
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!(userDetails instanceof SgreaUserDetails sgreaUserDetails)
                    || !sgreaUserDetails.isEnabled()
                    || !idUsuario.equals(sgreaUserDetails.getIdUsuario())) {
                log.warn("Refresh Token rechazado por usuario inválido o inactivo idUsuario={}", idUsuario);
                return null;
            }
            // Authority del usuario para generar los nuevos tokens
            String authority = sgreaUserDetails.getAuthorities().iterator().next().getAuthority();
            // Generar un nuevo jti y crear un nuevo par de tokens (access token y refresh token)
            String newJti = UUID.randomUUID().toString();
            String accessToken = jwtTokenProvider.generateAccessToken(
                    sgreaUserDetails.getUsername(),
                    idUsuario,
                    idSesion,
                    idBodega,
                    authority);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                    sgreaUserDetails.getUsername(),
                    idUsuario,
                    idSesion,
                    idBodega,
                    newJti,
                    authority);
            // Actualizar el jti en la sesión y guardar los cambios en la base de datos
            sesion.setToken(newJti);
            sesionRepository.save(sesion);
            log.info("Refresh Token rotado idSesion={}, idUsuario={}", idSesion, idUsuario);
            return new TokenPair(accessToken, newRefreshToken);
        } catch (RuntimeException exception) {
            log.warn("Refresh Token rechazado durante su validación");
            return null;
        }
    }
    // Clase interna para representar un par de tokens (access token y refresh token)
    public record TokenPair(String accessToken, String refreshToken) {
    }
}

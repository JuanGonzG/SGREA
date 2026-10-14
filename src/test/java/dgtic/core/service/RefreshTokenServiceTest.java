package dgtic.core.service;

import dgtic.core.model.entity.BodegaEntity;
import dgtic.core.model.entity.SesionEntity;
import dgtic.core.model.entity.UsuarioEntity;
import dgtic.core.repository.SesionRepository;
import dgtic.core.security.JWTTokenProvider;
import dgtic.core.security.SgreaUserDetailsService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private JWTTokenProvider jwtTokenProvider;

    @Mock
    private SesionRepository sesionRepository;

    @Mock
    private SgreaUserDetailsService userDetailsService;

    @Mock
    private Claims claims;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(
                jwtTokenProvider, sesionRepository, userDetailsService);
    }

    @Test
    void tokenInvalidoNoConsultaSesionNiRotaToken() {
        when(jwtTokenProvider.validateRefreshToken("token-invalido")).thenReturn(false);

        assertNull(refreshTokenService.rotate("token-invalido"));

        verifyNoInteractions(sesionRepository, userDetailsService);
    }

    @Test
    void tokenDeSesionInactivaEsRechazado() {
        when(jwtTokenProvider.validateRefreshToken("token-revocado")).thenReturn(true);
        when(jwtTokenProvider.extractClaims("token-revocado")).thenReturn(claims);
        configurarClaims(7, 12, 3, "jti-vigente");

        SesionEntity sesion = SesionEntity.builder()
                .idSesion(12)
                .token("jti-vigente")
                .activa(false)
                .usuario(UsuarioEntity.builder().idUsuario(7).build())
                .bodega(BodegaEntity.builder().idBodega(3).build())
                .build();
        when(sesionRepository.findWithUsuarioAndBodegaByIdSesion(12))
                .thenReturn(Optional.of(sesion));

        assertNull(refreshTokenService.rotate("token-revocado"));

        verify(sesionRepository, never()).save(any(SesionEntity.class));
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void tokenConJtiRevocadoEsRechazado() {
        when(jwtTokenProvider.validateRefreshToken("token-rotado")).thenReturn(true);
        when(jwtTokenProvider.extractClaims("token-rotado")).thenReturn(claims);
        configurarClaims(7, 12, 3, "jti-anterior");

        SesionEntity sesion = SesionEntity.builder()
                .idSesion(12)
                .token("jti-actual")
                .activa(true)
                .usuario(UsuarioEntity.builder().idUsuario(7).build())
                .bodega(BodegaEntity.builder().idBodega(3).build())
                .build();
        when(sesionRepository.findWithUsuarioAndBodegaByIdSesion(12))
                .thenReturn(Optional.of(sesion));

        assertNull(refreshTokenService.rotate("token-rotado"));

        verify(sesionRepository, never()).save(any(SesionEntity.class));
        verifyNoInteractions(userDetailsService);
    }

    private void configurarClaims(Integer idUsuario, Integer idSesion,
                                  Integer idBodega, String jti) {
        when(claims.getSubject()).thenReturn("usuario-prueba");
        when(claims.get("uid", Integer.class)).thenReturn(idUsuario);
        when(claims.get("sid", Integer.class)).thenReturn(idSesion);
        when(claims.get("bid", Integer.class)).thenReturn(idBodega);
        when(claims.getId()).thenReturn(jti);
    }
}

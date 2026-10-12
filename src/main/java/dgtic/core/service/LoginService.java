package dgtic.core.service;

import dgtic.core.model.dto.UsuarioDTO;
import dgtic.core.model.entity.UsuarioEntity;
import dgtic.core.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoginService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder encoder;

    public UsuarioDTO login(String username, String passwordIngresada) {
        // Validar el usuario exista
        try{
            UsuarioEntity usuario = usuarioRepository.findByUsernameIgnoreCase(username);
            if (usuario != null) {
                // Validar la contraseña
                if(passwordIngresada != null && encoder.matches(passwordIngresada, usuario.getPassword())) {
                    // Validar que el usuario esté activo
                    if (Boolean.FALSE.equals(usuario.getActivo())) {
                        log.warn("Intento de login de usuario inactivo username={}", usuario.getUsername());
                    }
                    return UsuarioDTO.builder()
                            .idUsuario(usuario.getIdUsuario())
                            .username(usuario.getUsername())
                            .activo(usuario.getActivo())
                            .build();
                }
            }
            log.warn("Intento de login inválido username={}", username);
        } catch (Exception e) {
            log.error("Error inesperado durante la autenticación username={}", username, e);
        }
        return null;
    }
}

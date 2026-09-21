package dgtic.core.service;

import dgtic.core.model.dto.UsuarioDTO;
import dgtic.core.model.entity.UsuarioEntity;
import dgtic.core.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
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
                    return UsuarioDTO.builder()
                            .idUsuario(usuario.getIdUsuario())
                            .username(usuario.getUsername())
                            .activo(usuario.getActivo())
                            .build();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

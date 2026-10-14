package dgtic.core.security;

import dgtic.core.model.entity.UsuarioEntity;
import dgtic.core.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SgreaUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public SgreaUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }
    // Implementación del método loadUserByUsername para cargar los detalles del usuario por su nombre de usuario
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Validar que el nombre de usuario no sea nulo o vacío
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }
        // Buscar el usuario en la base de datos ignorando mayúsculas y minúsculas
        UsuarioEntity usuario = usuarioRepository.findByUsernameIgnoreCase(username.trim());
        if (usuario == null) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }
        // Intentar crear un objeto SgreaUserDetails a partir del usuario encontrado
        try {
            return new SgreaUserDetails(usuario);
        } catch (IllegalArgumentException exception) {
            throw new UsernameNotFoundException("Usuario sin configuración de seguridad válida", exception);
        }
    }
}

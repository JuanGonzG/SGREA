package dgtic.core.security;

import dgtic.core.model.entity.RolCatalogo;
import dgtic.core.model.entity.RolEntity;
import dgtic.core.model.entity.UsuarioEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

public class SgreaUserDetails implements UserDetails {

    private final Integer idUsuario;
    private final String username;
    private final String password;
    private final boolean activo;
    private final GrantedAuthority authority;
    // Constructor que recibe un objeto UsuarioEntity y mapea sus propiedades a los campos de SgreaUserDetails
    public SgreaUserDetails(UsuarioEntity usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }

        this.idUsuario = usuario.getIdUsuario();
        this.username = usuario.getUsername();
        this.password = usuario.getPassword();
        this.activo = Boolean.TRUE.equals(usuario.getActivo());
        this.authority = new SimpleGrantedAuthority(resolveAuthority(usuario.getRol()));
    }
    // Método para obtener el ID del usuario
    public Integer getIdUsuario() {
        return idUsuario;
    }
    // Método para obtener las autoridades del usuario
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(authority);
    }
    // Obtener el password del usuario
    @Override
    public String getPassword() {
        return password;
    }
    // Obtener el username del usuario
    @Override
    public String getUsername() {
        return username;
    }
    // Métodos para verificar el estado de la cuenta del usuario
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    // Método para verificar si las credenciales del usuario no han expirado
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    // Método para verificar si el usuario está activo
    @Override
    public boolean isEnabled() {
        return activo;
    }
    // Método privado para resolver la autoridad del usuario a partir de su rol
    private static String resolveAuthority(RolEntity rol) {
        // Validar que el rol no sea nulo
        if (rol == null) {
            throw new IllegalArgumentException("El usuario debe tener un rol");
        }
        // Iterar sobre los valores del enum RolCatalogo para encontrar una coincidencia con el ID del rol
        for (RolCatalogo rolCatalogo : RolCatalogo.values()) {
            if (rolCatalogo.getId().equals(rol.getIdRol())) {
                return "ROLE_" + rolCatalogo.name();
            }
        }
        // Si no se encuentra una coincidencia por ID, intentar normalizar el nombre del rol y buscar una coincidencia por nombre
        if (rol.getNombre() != null) {
            String nombreNormalizado = rol.getNombre()
                    .trim()
                    .toUpperCase(Locale.ROOT)
                    .replace(' ', '_');
            // Iterar nuevamente sobre los valores del enum RolCatalogo para encontrar una coincidencia por nombre
            for (RolCatalogo rolCatalogo : RolCatalogo.values()) {
                // Comparar el nombre normalizado del rol con el nombre del enum RolCatalogo
                if (rolCatalogo.name().equals(nombreNormalizado)
                        || rolCatalogo.getNombre().toUpperCase(Locale.ROOT).equals(rol.getNombre().trim().toUpperCase(Locale.ROOT))) {
                    return "ROLE_" + rolCatalogo.name();
                }
            }
        }

        throw new IllegalArgumentException("Rol SGREA no reconocido");
    }
}

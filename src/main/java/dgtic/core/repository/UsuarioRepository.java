package dgtic.core.repository;

import dgtic.core.model.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Integer> {
    // Buscar un usuario por su nombre de usuario (ignorando mayúsculas y minúsculas)
    UsuarioEntity findByUsernameIgnoreCase(String username);
    // Verificar si un usuario existe por su nombre de usuario (ignorando mayúsculas y minúsculas)
    boolean existsByUsernameIgnoreCase(String username);
    // Verificar si un usuario existe por su nombre de usuario (ignorando mayúsculas y minúsculas) excluyendo un usuario específico por idUsuario
    boolean existsByUsernameIgnoreCaseAndIdUsuarioNot(String username, Integer idUsuario);

}

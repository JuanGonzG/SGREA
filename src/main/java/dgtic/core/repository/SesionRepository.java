package dgtic.core.repository;

import dgtic.core.model.entity.SesionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SesionRepository extends JpaRepository<SesionEntity, Integer> {
    //Buscar si un usuario tiene una sesión activa
    Optional<SesionEntity> findByUsuario_IdUsuarioAndActivaTrue(Integer idUsuario);

    // Buscar una sesión por token, activa y por usuario
    Optional<SesionEntity> findByUsuario_IdUsuarioAndTokenAndActivaTrue(Integer idUsuario, String token);

    // Buscar sesiones registradas por bodega
    boolean existsByBodega_IdBodega(Integer idBodega);

    // Buscar sesiones registradas por usuario
    boolean existsByUsuario_IdUsuario(Integer idUsuario);
}

package dgtic.core.repository;

import dgtic.core.model.entity.UsuarioBodega.UsuarioBodegaEntity;
import dgtic.core.model.entity.UsuarioBodega.UsuarioBodegaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioBodegaRepository extends JpaRepository<UsuarioBodegaEntity, UsuarioBodegaId> {
    // Obtener todas las bodegas asociadas a un usuario
    List<UsuarioBodegaEntity> findByUsuario_IdUsuario(Integer usuarioId);

    // Validar si hay registros por bodega
    boolean existsByBodega_IdBodega(Integer idBodega);

    // Eliminar todas las asociaciones de bodegas para un usuario
    void deleteByUsuario_IdUsuario(Integer usuarioId);

    // Eliminar una asociación específica entre un usuario y una bodega
    void deleteByUsuario_IdUsuarioAndBodega_IdBodega(Integer usuarioId, Integer idBodega);
}

package dgtic.core.repository;

import dgtic.core.model.entity.ContenedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContenedorRepository extends JpaRepository<ContenedorEntity, Integer> {
    // Buscar todos los contenedores ordenados por código ascendente
    List<ContenedorEntity> findAllByOrderByCodigoAsc();
    // Buscar el contenedor con el código más alto
    Optional<ContenedorEntity> findTopByOrderByCodigoDesc();
    // Validar si existe un contenedor con un código específico
    boolean existsByCodigo(String codigo);
    // Validar si existe un contenedor con un código específico, excluyendo un contenedor específico por idContenedor
    List<ContenedorEntity> findByEstadoContenedor_IdEstadoContenedorOrderByCodigoAsc(Integer idEstadoContenedor);
}

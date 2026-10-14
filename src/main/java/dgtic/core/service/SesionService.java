package dgtic.core.service;

import dgtic.core.mapping.Mapper;
import dgtic.core.model.dto.seguridad.SesionDTO;
import dgtic.core.model.entity.BodegaEntity;
import dgtic.core.model.entity.SesionEntity;
import dgtic.core.model.entity.UsuarioEntity;
import dgtic.core.repository.BodegaRepository;
import dgtic.core.repository.SesionRepository;
import dgtic.core.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class SesionService {
    @Autowired
    private SesionRepository sesionRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private BodegaRepository bodegaRepository;

    // Agregar una nueva sesión para un usuario y bodega específicos
    @Transactional
    public SesionDTO addSesion(Integer idUsuario, Integer idBodega, String token) {
        try{
            // Buscar si el usuario ya tiene una sesión activa
            Optional<SesionEntity> sesionActiva = sesionRepository.findByUsuario_IdUsuarioAndActivaTrue(idUsuario);

            //Si existe, finalizarla
            if(sesionActiva.isPresent()){
                // Cerrar la sesión activa existente
                SesionEntity sesionExpirada = sesionActiva.get();
                // Marcar la sesión como inactiva y establecer la fecha de fin
                sesionExpirada.setActiva(false);
                sesionExpirada.setFechaFin(LocalDateTime.now());
                // Guardar los cambios en la base de datos
                sesionRepository.save(sesionExpirada);
                log.info("Sesión previa cerrada usuarioId={}, username={}",
                        idUsuario, sesionExpirada.getUsuario().getUsername());
            }

            // Buscar el usuario y la bodega
            Optional<UsuarioEntity> usuario = usuarioRepository.findById(idUsuario);
            if(!usuario.isPresent()){
                throw new RuntimeException("Usuario no encontrado");
            }
            Optional<BodegaEntity> bodega = bodegaRepository.findById(idBodega);
            if(!bodega.isPresent()){
                throw new RuntimeException("Bodega no encontrada");
            }

            // Crear la nueva sesión
            SesionEntity nuevaSesion = SesionEntity.builder()
                    .usuario(usuario.get())
                    .bodega(bodega.get())
                    .token(token)
                    .fechaInicio(LocalDateTime.now())
                    .fechaFin(null)
                    .activa(true)
                    .build();
            // Guardar la nueva sesión en la base de datos
            nuevaSesion = sesionRepository.save(nuevaSesion);
            log.info("Nueva sesión creada usuarioId={}, username={}, bodegaId={}",
                    idUsuario, usuario.get().getUsername(), idBodega);
            return Mapper.toSesionDTO(nuevaSesion);
        } catch (Exception e) {
            log.error("Error inesperado al crear sesión usuarioId={}, bodegaId={}",
                    idUsuario, idBodega, e);
            throw new RuntimeException("Error al crear la sesión");
        }
    }

    // Cerrar la sesión activa de un usuario específico
    public Boolean closeSesion(Integer idUsuario, Integer idSesion, String token) {
        try {
            // Buscar la sesión activa del usuario
            Optional<SesionEntity> sesionOpt = sesionRepository.findByUsuario_IdUsuarioAndTokenAndActivaTrue(idUsuario, token);
            if (!sesionOpt.isPresent()) {
                throw new RuntimeException("Sesión no encontrada o ya cerrada");
            }
            // Obtener la sesión
            SesionEntity sesion = sesionOpt.get();

            // Cerrar la sesión
            sesion.setActiva(false);
            sesion.setFechaFin(LocalDateTime.now());
            sesionRepository.save(sesion);
            log.info("Sesión cerrada usuarioId={}, username={}",
                    idUsuario, sesion.getUsuario().getUsername());

            return true;
        } catch (Exception e) {
            log.error("Error inesperado al cerrar sesión usuarioId={}, sesionId={}",
                    idUsuario, idSesion, e);
            throw new RuntimeException("Error al cerrar la sesión");
        }
    }
    // Cerrar la sesión activa de un usuario específico por idSesion
    @Transactional
    public Boolean closeSesion(Integer idUsuario, Integer idSesion) {
        try {
            // Buscar la sesión por idSesion
            Optional<SesionEntity> sesionOpt = sesionRepository.findById(idSesion);
            if (sesionOpt.isEmpty()) {
                log.warn("Cierre de sesión rechazado: sesión no encontrada sesionId={}", idSesion);
                return false;
            }
            // Verificar que la sesión esté activa y que el usuario coincida
            SesionEntity sesion = sesionOpt.get();
            if (!Boolean.TRUE.equals(sesion.getActiva())
                    || sesion.getUsuario() == null
                    || !idUsuario.equals(sesion.getUsuario().getIdUsuario())) {
                log.warn("Cierre de sesión rechazado: sesión inactiva o usuario inconsistente sesionId={}, usuarioId={}",
                        idSesion, idUsuario);
                return false;
            }
            // Cerrar la sesión
            sesion.setActiva(false);
            // Establecer la fecha de fin de la sesión
            sesion.setFechaFin(LocalDateTime.now());
            // Guardar los cambios en la base de datos
            sesionRepository.save(sesion);
            log.info("Sesión cerrada usuarioId={}, username={}",
                    idUsuario, sesion.getUsuario().getUsername());
            return true;
        } catch (RuntimeException exception) {
            log.error("Error inesperado al cerrar sesión sesionId={}, usuarioId={}",
                    idSesion, idUsuario, exception);
            throw new RuntimeException("Error al cerrar la sesión", exception);
        }
    }

    // Verificar si la sesión está activa
    public Boolean isSesionActiva(HttpSession session) {
        // Verificar si hay una sesión activa en el servidor
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        Integer idSesion = (Integer) session.getAttribute("idSesion");
        String token = (String) session.getAttribute("token");

        // Verificar si los atributos de sesión son nulos
        if (idUsuario == null || idSesion == null || token == null) {
            return false;
        }

        // Verificar si la sesión está activa en la base de datos
        Optional<SesionEntity> sesionOpt = sesionRepository.findById(idSesion);
        if (!sesionOpt.isPresent()) {
            return false;
        }
        // Retornar el estado de la sesión
        SesionEntity sesion = sesionOpt.get();
        return sesion.getActiva();
    }
}

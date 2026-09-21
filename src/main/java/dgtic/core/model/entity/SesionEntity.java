package dgtic.core.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sesion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Integer idSesion;

    @Column(nullable = false, length = 255, unique = true)
    private String token;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(nullable = false)
    private Boolean activa;

    // Relación N:1 con Usuario --> muchas sesiones pertenecen a un usuario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity usuario;

    // Relación N:1 con Bodega --> muchas sesiones pertenecen a una bodega
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bodega", nullable = false)
    private BodegaEntity bodega;
}

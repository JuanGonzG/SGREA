package dgtic.core.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hojacontenedor")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HojaContenedorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hoja_contenedor")
    private Integer idHojaContenedor;

    // Relación N:1 con HojaProduccionEntity
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_hoja", nullable = false)
    private HojaProduccionEntity hojaProduccion;

    // Relación N:1 con ContenedorEntity
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contenedor", nullable = false)
    private ContenedorEntity contenedor;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    @Column(name = "fecha_cierre_carga")
    private LocalDateTime fechaCierreCarga;

    @Column(name = "fecha_liberacion")
    private LocalDateTime fechaLiberacion;

    // Relación N:1 con UsuarioEntity para el usuario que asigna la hoja al contenedor
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_asignacion", nullable = false)
    private UsuarioEntity usuarioAsignacion;

    // Relación N:1 con UsuarioEntity para el usuario que cierra la carga
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_cierre")
    private UsuarioEntity usuarioCierre;

    // Relación N:1 con UsuarioEntity para el usuario que libera la hoja del contenedor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_liberacion")
    private UsuarioEntity usuarioLiberacion;

    @Column(length = 255)
    private String observaciones;
}

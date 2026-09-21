package dgtic.core.model.entity;

/** Catálogo cerrado de roles funcionales del sistema. */
public enum RolCatalogo {
    ADMINISTRADOR(1, "Administrador"),
    SUPERVISOR(2, "Supervisor"),
    OPERADOR(3, "Operador");

    private final Integer id;
    private final String nombre;

    RolCatalogo(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}

# SGREA

Sistema de Gestión de Recursos y Almacenes (SGREA), desarrollado con Spring Boot, Thymeleaf, Spring Data JPA y MariaDB.

SGREA representa la operación de una organización que administra equipo técnico para proyectos, producciones y eventos.
Los recursos se organizan por bodegas y cada salida conserva qué unidades físicas fueron entregadas, quién realizó la
operación, en qué contenedor viajaron y cuándo regresaron.

El proyecto tiene alcance académico y demuestra un flujo completo de inventario operativo, trazabilidad, autenticación,
autorización, logging y reportes. Las reglas críticas se validan en servidor; la interfaz no sustituye las reglas de
negocio ni la autorización.

## Conceptos del dominio

- **Bodega:** contexto operativo que agrupa productos y delimita las operaciones del usuario.
- **Producto:** tipo de recurso administrado por una bodega.
- **Conjunto:** unidad física individual de un producto, identificada con un código global como `CON-000001`.
- **Hoja de producción:** solicitud de recursos para un proyecto. La misma hoja se utiliza durante preparación, salida,
  retorno y cierre.
- **Detalle de hoja:** producto y cantidades solicitadas, surtidas y devueltas.
- **Contenedor:** recurso reutilizable con código escaneable y capacidad máxima de conjuntos.
- **Hoja–Contenedor:** asociación temporal y auditable entre un contenedor y una hoja.
- **Movimiento:** evento histórico e inmutable de `SALIDA` o `ENTRADA`.
- **Sesión operativa:** contexto persistente que vincula al usuario autenticado con una bodega seleccionada.

## Roles y alcance

| Área | Administrador | Supervisor | Operador |
|---|:---:|:---:|:---:|
| Catálogos administrativos | Sí | No | No |
| Hojas de producción | Sí | Sí | No |
| Monitoreo y reportes | Sí | Sí | No |
| HH Surtido y HH Recepción | Sí | No | Sí |

Una persona puede tener varias bodegas autorizadas, pero cada sesión operativa utiliza una sola bodega activa.

La autorización real se aplica en backend mediante Spring Security. La visibilidad del menú con Thymeleaf Security es
sólo una ayuda de presentación.

## Flujo operativo

### Inicio de sesión

1. El usuario proporciona username y contraseña.
2. Spring Security valida las credenciales con los usuarios persistidos y BCrypt.
3. Se rechazan credenciales inválidas y usuarios inactivos.
4. Se cargan las bodegas autorizadas del usuario.
5. Una única bodega se selecciona automáticamente; varias bodegas requieren selección explícita.
6. El servidor valida nuevamente la relación Usuario–Bodega.
7. Se cierra cualquier sesión operativa anterior del mismo usuario.
8. Se crea una nueva `SesionEntity`.
9. Se emiten Access Token y Refresh Token mediante cookies HttpOnly.

La sesión de negocio y los JWT cumplen responsabilidades distintas: `SesionEntity` conserva contexto, auditoría y
revocación; los JWT permiten reconstruir la autenticación web.

### Hoja de producción

El usuario autorizado crea una hoja con proyecto, cliente, fechas y bodega activa, y posteriormente agrega sus detalles.

Reglas principales:

- hoja y productos deben pertenecer a la bodega activa;
- un producto no puede repetirse dentro de la misma hoja;
- `cantidadSurtida` y `cantidadDevuelta` se administran por los flujos operativos;
- las hojas dejan de ser editables desde el CRUD al entrar en estados operativos.

Ciclo implementado:

```text
Creada -> Por surtir -> Surtiendo -> En llamado -> Recibiendo -> Completado
```

### HH Surtido / SALIDA

1. Seleccionar una hoja autorizada.
2. Asignar un contenedor disponible.
3. Escanear o seleccionar un conjunto físico.
4. Validar bodega, producto, detalle, disponibilidad, carga abierta y capacidad.
5. Registrar `Movimiento SALIDA`.
6. Cambiar el conjunto de `DISPONIBLE` a `SURTIDO`.
7. Incrementar `cantidadSurtida`.

### HH Recepción / ENTRADA

1. Consultar conjuntos con SALIDA pendiente.
2. Identificar el conjunto que regresa.
3. Exigir una SALIDA previa para esa hoja y conjunto.
4. Resolver el detalle correspondiente.
5. Registrar `Movimiento ENTRADA`.
6. El retorno puede usar un contenedor distinto o no utilizar contenedor.
7. Cambiar el conjunto de `SURTIDO` a `DISPONIBLE`.
8. Incrementar `cantidadDevuelta`.
9. Permitir recepciones parciales.
10. Completar la hoja sólo cuando todo el material haya regresado y no existan utilizaciones de contenedor pendientes
    de liberar.

### Contenedores reutilizables

Los contenedores no pertenecen permanentemente a una hoja. Su uso se registra mediante `HojaContenedor`.

- nuevos contenedores inician en `DISPONIBLE`;
- una asignación operativa los lleva a `EN_USO`;
- cierre y liberación registran usuario y fecha;
- un contenedor con historial no se elimina físicamente;
- `FUERA_DE_SERVICIO` permite retirarlo de operación conservando su historial.

### Monitoreo y reportes

Monitoreo muestra operaciones activas de surtido y recepción, avances, contenedores y contexto de movimientos.

Los reportes son de consulta y permiten salida JSON, Excel y PDF según el tipo de reporte. La trazabilidad persistente
permanece en `Movimiento`, `SesionEntity` y `HojaContenedor`.

## Requisitos

- Java JDK 17.
- MariaDB 10.4 o superior; el esquema fue validado con MariaDB 10.6.
- Git.
- Cliente MariaDB (`mariadb` o `mysql`) para restaurar el esquema.
- Conexión a Internet sólo cuando Maven necesite descargar dependencias que no estén disponibles localmente.

No es necesario instalar Maven: el proyecto incluye Maven Wrapper (`mvnw` y `mvnw.cmd`).

## Base de datos

La aplicación usa una base `sgrea` y `spring.jpa.hibernate.ddl-auto=validate`. Hibernate valida el esquema existente y
no crea ni modifica tablas.

Crear una base vacía:

```sql
CREATE DATABASE sgrea CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Restaurar el esquema:

```text
mariadb -u <usuario> -p sgrea < docs/esquema-actual.sql
```

> `docs/esquema-actual.sql` contiene `DROP TABLE` y está pensado para preparar una base de demostración vacía.

El dump incluye datos académicos mínimos de catálogos y una cuenta inicial de demostración. Esa cuenta existe
exclusivamente para arranque local y debe cambiarse o desactivarse después del primer acceso fuera de una demostración.

La cuenta bootstrap actual es:

```text
Usuario: admin
Contraseña: 12345
Bodega: Bodega General
Rol: Administrador
```

> Esta contraseña bootstrap proviene del seed SQL y no representa la política actual del CRUD de usuarios. Los usuarios
> creados desde la aplicación deben cumplir la longitud mínima configurada por el sistema.

Las credenciales reales de MariaDB no se almacenan en el repositorio.

## Variables de entorno

El archivo `.env.example` documenta las variables requeridas. Spring Boot no carga `.env` automáticamente; las
variables deben exportarse en la terminal, configurarse en el IDE o proporcionarse mediante el entorno de ejecución.

Ejemplo en PowerShell:

```powershell
$env:SGREA_JWT_SECRET = "cambia-este-secreto-por-uno-de-32-bytes-o-mas"
$env:SGREA_JWT_ACCESS_EXPIRATION_MS = "900000"
$env:SGREA_JWT_REFRESH_EXPIRATION_MS = "604800000"
$env:SGREA_JWT_COOKIE_SECURE = "false"

$env:SPRING_DATASOURCE_USERNAME = "<usuario-mariadb>"
$env:SPRING_DATASOURCE_PASSWORD = "<contraseña-mariadb>"
```

Opcionalmente puede sobrescribirse también `SPRING_DATASOURCE_URL`.

El secreto JWT debe tener al menos 32 bytes. En HTTPS debe utilizarse:

```text
SGREA_JWT_COOKIE_SECURE=true
```

No subir secretos, contraseñas de infraestructura, cookies, JWT ni Refresh Tokens al repositorio.

## Ejecutar localmente

Desde la raíz del proyecto:

```powershell
New-Item -ItemType Directory -Force uploads\productos | Out-Null
New-Item -ItemType Directory -Force logs | Out-Null
.\mvnw.cmd spring-boot:run
```

Aplicación:

```text
http://localhost:8080/
```

Las imágenes se almacenan en `uploads/productos` y los logs en `logs/sgrea.log`. Ambos directorios están excluidos de
Git.

## Compilar y probar

Compilar:

```powershell
.\mvnw.cmd clean package
```

Pruebas:

```powershell
.\mvnw.cmd test
```

Las pruebas que levantan el contexto Spring requieren una instancia MariaDB compatible con `docs/esquema-actual.sql`.

## Seguridad

La implementación actual incluye:

- Spring Security;
- usuarios persistidos y BCrypt;
- `ROLE_ADMINISTRADOR`, `ROLE_SUPERVISOR` y `ROLE_OPERADOR`;
- Thymeleaf Security;
- Access JWT en cookie HttpOnly;
- Refresh JWT en cookie HttpOnly;
- rotación y revocación de Refresh Token mediante `SesionEntity`;
- CSRF habilitado para formularios y peticiones mutables;
- logout con cierre de sesión operativa, limpieza del `SecurityContext` y expiración de cookies.

`POST /refresh` puede ejecutarse sin un Access Token válido porque su propósito es renovar la autenticación, pero sigue
requiriendo CSRF y un Refresh Token válido asociado a una sesión activa.

## Estructura principal

```text
src/main/java/dgtic/core/
├── config/       Configuración de aplicación, seguridad y recursos web
├── controller/   Controladores MVC y endpoints JSON
├── mapping/      Conversión explícita entre DTOs y entidades
├── model/        Entidades y DTOs
├── repository/   Repositorios Spring Data JPA
├── security/     JWT, filtros y servicios de autenticación
└── service/      Reglas de negocio y persistencia

src/main/resources/
├── paginas/      Vistas Thymeleaf
├── static/       CSS, JavaScript y librerías frontend locales
└── application.properties

docs/
├── entity-specification.md
└── esquema-actual.sql
```

## Documentación relacionada

- [Entity and Business Rules Specification](docs/entity-specification.md)
- `docs/esquema-actual.sql` — baseline físico de persistencia.

## Consideraciones de desarrollo

- Mantener `spring.thymeleaf.prefix=classpath:/paginas/`.
- No cambiar `ddl-auto=validate` por `create`, `create-drop` o `update` en un entorno con datos.
- Mantener reglas de bodega, sesión operativa y autorización en servidor.
- No registrar passwords, hashes BCrypt, JWT completos, Refresh Tokens, secretos o cookies completas.
- Reflejar cualquier cambio de esquema primero en el DDL y en la documentación correspondiente.

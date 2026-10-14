# SGREA

Sistema de Gestión de Recursos y Almacenes (SGREA), desarrollado con Spring Boot, Thymeleaf, Spring Data JPA y MariaDB.

SGREA representa la operación de una organización que presta o administra equipo técnico para proyectos, producciones y eventos. Los recursos se organizan por bodegas —por ejemplo, iluminación o audio— y cada salida hacia un proyecto debe conservar quién solicitó el material, qué unidades físicas se entregaron, en qué contenedor viajaron y cuándo regresaron.

El proyecto tiene alcance académico: busca demostrar un flujo completo de inventario operativo, trazabilidad, autenticación y autorización, manteniendo las reglas importantes en el servidor. La interfaz facilita el trabajo de almacén, pero la fuente de verdad siempre es la sesión, la base de datos y el histórico de movimientos.

## Conceptos del dominio

- **Bodega:** espacio lógico que agrupa productos y delimita el contexto operativo del usuario.
- **Producto:** tipo de recurso administrado por una bodega, como una luminaria, consola o cable.
- **Conjunto:** unidad física individual de un producto. Tiene un identificador global escaneable (`CON-000001`) y un estado operativo.
- **Hoja de producción:** solicitud de recursos para un proyecto y cliente. Es la misma hoja durante preparación, salida, retorno y cierre; no existe una hoja de devolución separada.
- **Detalle de hoja:** producto y cantidades solicitadas, surtidas y devueltas.
- **Contenedor:** recurso reutilizable con código escaneable y capacidad máxima de conjuntos.
- **Hoja–Contenedor:** asociación temporal de un contenedor con una hoja concreta.
- **Movimiento:** evento histórico e inmutable de `SALIDA` o `ENTRADA` sobre un conjunto.
- **Sesión operativa:** contexto persistente que vincula al usuario autenticado con la bodega seleccionada.

## Actores y alcance funcional

SGREA maneja tres roles principales:

- **Administrador:** administra catálogos y usuarios, además de consultar supervisión, monitoreo, reportes y operación.
- **Supervisor:** consulta y supervisa hojas, monitoreo y reportes de su contexto autorizado.
- **Operador:** ejecuta los flujos de almacén de surtido y recepción.

El acceso efectivo también depende de la bodega asociada al usuario. Una persona puede tener varias bodegas autorizadas, pero cada operación se ejecuta dentro de una única bodega activa.

## Flujos importantes

### 1. Inicio de sesión y selección de bodega

1. El usuario proporciona sus credenciales.
2. El sistema rechaza credenciales incorrectas o usuarios inactivos.
3. Se consultan las bodegas asociadas mediante la relación usuario–bodega.
4. Si existe una sola bodega, se selecciona automáticamente; si existen varias, el usuario debe elegir una.
5. La selección se valida en servidor y se crea el contexto operativo.
6. Si el usuario ya tenía una sesión activa, esta se cierra antes de crear la nueva.

La sesión de negocio no es equivalente al JWT: la sesión registra el contexto operativo y la auditoría de SGREA, mientras que los tokens permiten autenticar las peticiones web.

### 2. Navegación por rol

Después de iniciar sesión, Home muestra el contexto del usuario y la bodega activa. Las opciones se presentan según el rol:

| Área | Administrador | Supervisor | Operador |
| --- | --- | --- | --- |
| Catálogos administrativos | Sí | No | No |
| Hojas de producción | Sí | Sí | Según operación autorizada |
| Monitoreo y reportes | Sí | Sí | No |
| Surtido y recepción | Sí | No | Sí |

Además de la interfaz, cada operación valida en servidor la sesión, el rol cuando aplica y la pertenencia a la bodega activa.

### 3. Preparación de una hoja de producción

El usuario autorizado crea una hoja con proyecto, cliente, fechas y bodega de sesión. Después agrega detalles de productos y cantidades solicitadas.

Las reglas principales son:

- La hoja y sus productos deben pertenecer a la bodega activa.
- Un producto no se repite dentro de la misma hoja.
- La cantidad surtida inicia en cero.
- La hoja se puede editar mientras su estado lo permita.
- Al avanzar a estados operativos, la hoja pasa a un régimen de solo lectura para proteger la trazabilidad.

### 4. HH Surtido y SALIDA

El operador trabaja con la hoja desde la pantalla de Surtido (`HH Surtido`):

1. Selecciona una hoja lista para surtir.
2. Asigna un contenedor disponible; la relación Hoja–Contenedor registra usuario y fecha.
3. Escanea o selecciona un conjunto físico.
4. El sistema verifica que el conjunto esté disponible, pertenezca al producto del detalle y corresponda a la bodega de la hoja.
5. Se valida que el contenedor aún tenga capacidad y que el conjunto no tenga una salida pendiente.
6. Se registra un `Movimiento SALIDA` con hoja, detalle, conjunto, contenedor, usuario y fecha.
7. La operación transaccional cambia el conjunto de `DISPONIBLE` a `SURTIDO` e incrementa `cantidadSurtida`.

La capacidad del contenedor, los dobles escaneos y los productos incorrectos se rechazan en el servidor; la interfaz solo proporciona retroalimentación visual.

### 5. HH Recepción y ENTRADA

Cuando el material regresa del proyecto, el operador utiliza `HH Recepción` sobre la misma hoja:

1. Consulta los conjuntos que tienen una salida pendiente.
2. Escanea o identifica el conjunto que regresa.
3. El sistema exige una salida previa para esa hoja y conjunto.
4. Se resuelve el detalle correspondiente por hoja y producto.
5. Se registra un `Movimiento ENTRADA`, que puede asociarse al contenedor de retorno o no tener contenedor.
6. El conjunto cambia de `SURTIDO` a `DISPONIBLE` y aumenta `cantidadDevuelta`.
7. La recepción puede ser parcial; la hoja se completa cuando todos sus detalles cumplen `cantidadDevuelta == cantidadSurtida` y se liberan las utilizaciones de contenedor que correspondan.

Una entrada duplicada, una devolución sin salida previa o una cantidad superior a la surtida se rechaza y no altera el histórico.

### 6. Ciclo de estados de una hoja

El ciclo operativo de una hoja es:

```text
Creada -> Por surtir -> Surtiendo -> En llamado
                                      |
                                      v
                              Recibiendo -> Completado
```

La salida y la recepción son etapas del mismo proceso. `Movimiento` conserva los eventos físicos, mientras `DetalleHoja` mantiene los acumulados operativos.

### 7. Contenedores reutilizables

Un contenedor no pertenece permanentemente a una hoja. Se asigna temporalmente mediante `HojaContenedor`, puede utilizarse en distintas operaciones a lo largo del tiempo y conserva su historial.

- Un contenedor nuevo inicia como `DISPONIBLE`.
- Al asignarse a una carga pasa a `EN_USO`.
- El cierre de carga y la liberación registran usuario y fecha.
- No se permite reducir su capacidad mientras está en uso.
- Un contenedor con historial no se elimina físicamente; debe conservarse y retirarse mediante su estado operativo.

### 8. Monitoreo y reportes

Monitoreo presenta operaciones activas de surtido y recepción, sus hojas, contenedores, avances y tiempos derivados. Los reportes consultan hojas, movimientos, conjuntos y contenedores dentro del contexto autorizado, con exportación a Excel o PDF cuando corresponde.

Los reportes no sustituyen la trazabilidad: `Movimiento`, `SesionEntity` y `HojaContenedor` conservan la evidencia persistente de la operación.

## Requisitos

- Java JDK 17.
- MariaDB 10.4 o superior; el esquema utilizado se validó con MariaDB 10.6.
- Git.
- Conexión a Internet únicamente durante la primera descarga de dependencias Maven, si no están disponibles localmente.
- Cliente MariaDB (`mariadb` o `mysql`) para restaurar el esquema.

No es necesario instalar Maven: el proyecto incluye Maven Wrapper (`mvnw` y `mvnw.cmd`).

## Configuración de la base de datos

La aplicación espera una base llamada `sgrea` en `localhost:3306` y utiliza `spring.jpa.hibernate.ddl-auto=validate`; Hibernate no crea ni modifica tablas automáticamente.

1. Crear una base vacía:

   ```sql
   CREATE DATABASE sgrea CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Restaurar el esquema vigente en una base vacía:

   ```text
   mariadb -u <usuario> -p sgrea < docs/esquema-actual.sql
   ```

   El archivo `docs/esquema-actual.sql` es la fuente de verdad del esquema físico. Contiene instrucciones `DROP TABLE`; no debe ejecutarse sobre una base con datos que se quieran conservar.

3. Verificar que el usuario tenga permisos sobre `sgrea`.

El proyecto no documenta credenciales reales. Se recomienda proporcionar usuario y contraseña mediante variables de entorno o una configuración externa.

Al restaurar el dump se crea una cuenta académica inicial para poder acceder al sistema:

```text
Usuario:    admin
Contraseña: 12345
Bodega:     Bodega General
Rol:        Administrador
```

Esta contraseña es únicamente para el arranque local del proyecto. Debe cambiarse o desactivarse después del primer acceso en cualquier entorno que no sea una demostración académica.

## Variables de entorno

El JWT requiere un secreto de al menos 32 bytes. En desarrollo local puede configurarse así en PowerShell:

El archivo `.env.example` contiene la plantilla de variables requeridas. Spring Boot no carga archivos `.env` automáticamente; deben exportarse en la terminal, configurarse en el IDE o mediante el mecanismo de configuración del entorno.

```powershell
$env:SGREA_JWT_SECRET = "cambia-este-secreto-por-uno-de-32-bytes-o-mas"
$env:SGREA_JWT_ACCESS_EXPIRATION_MS = "900000"
$env:SGREA_JWT_REFRESH_EXPIRATION_MS = "604800000"
$env:SGREA_JWT_COOKIE_SECURE = "false"
```

Para sobrescribir la conexión local sin modificar `application.properties`:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mariadb://localhost:3306/sgrea?useSSL=false&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME = "<usuario-mariadb>"
$env:SPRING_DATASOURCE_PASSWORD = "<contraseña-mariadb>"
```

En un entorno HTTPS, cambiar `SGREA_JWT_COOKIE_SECURE` a `true`. No subir secretos, contraseñas, cookies, JWT ni refresh tokens al repositorio.

## Ejecutar localmente

Desde la raíz del proyecto:

```powershell
New-Item -ItemType Directory -Force uploads\productos | Out-Null
New-Item -ItemType Directory -Force logs | Out-Null
.\mvnw.cmd spring-boot:run
```

La aplicación quedará disponible en:

```text
http://localhost:8080/
```

Las imágenes de productos se almacenan en `uploads/productos` y se sirven mediante `/uploads/productos/**`. Los logs se escriben en consola y en `logs/sgrea.log`; esta carpeta está excluida de Git.

Para detener la aplicación, presionar `Ctrl+C` en la terminal donde se está ejecutando.

## Compilar y probar

Compilar el proyecto:

```powershell
.\mvnw.cmd clean package
```

Ejecutar todas las pruebas:

```powershell
.\mvnw.cmd test
```

Las pruebas de integración que levantan el contexto Spring requieren que MariaDB esté disponible y que la base `sgrea` sea compatible con el DDL vigente.

Ejecutar una prueba específica:

```powershell
.\mvnw.cmd '-Dtest=SecurityErrorPageTest' test
```

## Seguridad

- Spring Security protege vistas Thymeleaf y endpoints.
- La autenticación utiliza los usuarios persistidos en SGREA y hashes BCrypt.
- Los roles se expresan como `ROLE_ADMINISTRADOR`, `ROLE_SUPERVISOR` y `ROLE_OPERADOR`.
- El Access Token y el Refresh Token se transportan mediante cookies.
- CSRF permanece habilitado porque la autenticación usa cookies.
- El logout finaliza la sesión operativa e invalida el contexto de autenticación.
- Las páginas personalizadas de error cubren `403`, `404`, `400` procesado por Spring y los fallos `5xx` gestionados por la aplicación.

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

## Consideraciones de desarrollo

- No cambiar el prefijo de Thymeleaf: `classpath:/paginas/`.
- No sustituir `ddl-auto=validate` por `create`, `create-drop` o `update` en un entorno con datos.
- Mantener la validación de bodega, sesión operativa y reglas de negocio en servidor.
- No registrar contraseñas, hashes, JWT completos, refresh tokens, secretos ni cookies completas.
- Antes de modificar el esquema, actualizar el DDL vigente y la documentación correspondiente.

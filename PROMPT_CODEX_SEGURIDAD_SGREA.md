# Prompt maestro para Codex — Spring Security SGREA por fases

Trabaja sobre el proyecto **SGREA** actual.

## 1. Antes de modificar código

Debes leer e inspeccionar primero:

1. `AGENTS.md`
2. `docs/entity-specification.md`
3. `pom.xml`
4. `src/main/resources/application.properties`
5. Login actual:
   - `LoginController`
   - `LoginService`
   - `SesionService`
   - vista `login/login.html`
6. Seguridad/modelo:
   - `UsuarioEntity`
   - `RolEntity`
   - `UsuarioBodegaEntity`
   - `SesionEntity`
   - repositorios relacionados
7. Layout/vistas:
   - `master.html`
   - Home
   - Sidebar
   - formularios Thymeleaf
8. JavaScript con operaciones `POST`, `PUT` y `DELETE`
9. Logging V1 ya implementado

Los documentos son el contrato funcional vigente.

No inventes tablas, columnas, relaciones, endpoints o reglas que contradigan la documentación.

---

# 2. Referencia académica

La implementación debe mantenerse conceptualmente alineada al repositorio del profesor:

`https://github.com/devil861109/DIP_SPRING_SECURITY`

Ramas principales de referencia:

```text
11-spring-security-jwt-thymeleaf
11-1-spring-security-jwt-thymeleaf-refresh
```

Usar como referencia sus patrones de:

- `SecurityFilterChain`
- `AuthenticationManager`
- `DaoAuthenticationProvider`
- `UserDetailsService`
- implementación de `UserDetails`
- BCrypt
- authorities
- Thymeleaf Security
- JWT
- `OncePerRequestFilter`
- JWT mediante Cookie
- Access Token + Refresh Token
- rotación de Refresh Token
- logout personalizado

No copies literalmente el proyecto del profesor.

SGREA tiene reglas propias que prevalecen.

---

# 3. Objetivo final de SGREA

La implementación debe llegar únicamente a:

```text
Spring Security
+
usuarios reales SGREA desde BD
+
roles SGREA
+
Thymeleaf Security
+
JWT Access Token
+
JWT mediante Cookie HttpOnly
+
Refresh Token mediante Cookie HttpOnly
+
rotación y revocación
+
logout completo
+
autorización efectiva por rol
+
CSRF habilitado
```

Quedan fuera de alcance:

- OAuth2
- GitHub OAuth
- Keycloak
- LDAP
- OpenID Connect
- Authorization Server
- autenticación REST paralela
- Bearer Token como mecanismo principal
- JWT en localStorage
- JWT en sessionStorage

---

# 4. Regla crítica de ejecución

## IMPLEMENTAR UNA SOLA FASE POR EJECUCIÓN

Implementa únicamente la fase indicada por el usuario.

No adelantes código de fases posteriores.

Al terminar:

1. compila el proyecto;
2. ejecuta las pruebas existentes aplicables;
3. verifica que no rompiste funcionalidades previas;
4. lista archivos creados;
5. lista archivos modificados;
6. explica brevemente las decisiones tomadas;
7. indica qué validaste;
8. indica pendientes para la siguiente fase;
9. DETENTE.

No continúes con otra fase sin instrucción explícita.

---

# 5. Restricciones globales

No modificar DDL salvo instrucción expresa.

No crear tabla nueva de Refresh Token.

No cambiar PK/FK ni relaciones por conveniencia.

No crear modelos alternativos de Usuario/Rol.

No borrar funcionalidades existentes para simplificar Spring Security.

No almacenar JWT completo en BD.

No almacenar Refresh JWT completo en `SesionEntity.token`.

No registrar:

- password
- hash BCrypt
- JWT completo
- Refresh Token completo
- cookies completas
- secreto JWT
- credenciales datasource

Conservar Logging V1 con SLF4J/Logback.

Usar logging parametrizado.

No usar `System.out.println`.

Preferir inyección por constructor en código nuevo.

Evitar sobreingeniería.

---

# 6. Flujo de login congelado

Regla:

```text
credenciales válidas
        !=
login SGREA exitoso
```

El login SGREA sólo está completo cuando existen:

```text
credenciales válidas
+
Bodega autorizada
+
SesionEntity creada
+
Access Token emitido
+
Refresh Token emitido
```

Endpoints finales:

```text
GET  /login
POST /login-bodega
POST /login-enter
POST /refresh
POST /logout
```

`POST /login/crear-sesion` debe desaparecer del diseño final como endpoint público y pasar a lógica interna de Service cuando corresponda.

---

# 7. Autenticación de credenciales

Flujo final:

```text
LoginController
    ->
AuthenticationManager
    ->
DaoAuthenticationProvider
    ->
SgreaUserDetailsService
    ->
UsuarioRepository
    ->
BCrypt PasswordEncoder
```

Authorities:

```text
Rol 1 -> ROLE_ADMINISTRADOR
Rol 2 -> ROLE_SUPERVISOR
Rol 3 -> ROLE_OPERADOR
```

`UsuarioEntity.activo` debe mapearse a:

```java
UserDetails.isEnabled()
```

---

# 8. Selección de Bodega

Después de validar credenciales:

```text
0 bodegas -> login rechazado
1 bodega  -> selección automática
N bodegas -> login pendiente + selector
```

Para login pendiente usar `HttpSession` únicamente como almacenamiento temporal seguro.

Guardar como máximo:

```text
idUsuario
username
```

No guardar password ni credenciales reutilizables.

En `POST /login-enter`, el cliente envía la Bodega.

El backend debe volver a validar:

```text
Usuario -> UsuarioBodega -> Bodega
```

Nunca confiar en un `idUsuario` enviado desde el navegador.

---

# 9. SesionEntity

Al completar login:

```text
cerrar sesión activa anterior
    ->
generar refreshId/jti
    ->
crear nueva SesionEntity
```

Conservar:

```text
una sola SesionEntity activa por usuario
```

La nueva sesión debe contener:

```text
Usuario
Bodega
fechaInicio
activa=true
token=refreshId
```

`SesionEntity.token` no es el JWT.

Debe cumplirse:

```text
Refresh JWT.jti == SesionEntity.token
```

---

# 10. Access JWT

Claims mínimos:

```text
sub  = username
uid  = idUsuario
sid  = idSesion
bid  = idBodega
role = ROLE_...
typ  = access
iat
exp
```

No serializar objetos completos.

Duración baseline configurable:

```text
15 minutos
```

---

# 11. Refresh JWT

Claims mínimos:

```text
sub = username
uid = idUsuario
sid = idSesion
jti = refreshId
typ = refresh
iat
exp
```

Duración baseline configurable:

```text
8 horas
```

Debe ser mayor que la del Access Token.

---

# 12. Cookies

Cookies:

```text
token
refresh_token
```

Propiedades:

```text
HttpOnly=true
Path=/
SameSite apropiado
Secure=true en HTTPS
```

`Secure` debe ser configurable para desarrollo local.

JavaScript no debe leer directamente los JWT.

---

# 13. JWTAuthenticationFilter

Debe extender:

```java
OncePerRequestFilter
```

Validar:

```text
cookie token
firma
expiración
typ=access
uid
sid
bid
SesionEntity existente
SesionEntity activa
usuario coherente
bodega coherente
usuario activo
```

Después reconstruir:

```text
SgreaUserDetailsService
    ->
UsernamePasswordAuthenticationToken
    ->
SecurityContextHolder
```

Un JWT válido criptográficamente debe rechazarse si su `SesionEntity` está inactiva.

---

# 14. Refresh

`POST /refresh` debe funcionar aunque Access haya expirado.

Validar:

```text
firma
typ=refresh
exp
sid
uid
usuario activo
SesionEntity activa
jti == SesionEntity.token
```

Si es válido:

```text
nuevo refreshId
    ->
actualizar SesionEntity.token
    ->
nuevo Access JWT
    ->
nuevo Refresh JWT
    ->
reemplazar cookies
```

El Refresh anterior queda invalidado.

---

# 15. Logout

`POST /logout` debe terminar realizando:

```text
cerrar SesionEntity
+
invalidar Refresh
+
eliminar cookie token
+
eliminar cookie refresh_token
+
limpiar SecurityContext
+
invalidar HttpSession auxiliar
+
redirect /login
```

Borrar cookies por sí solo no constituye revocación.

---

# 16. Matriz de autorización

```text
Inicio:
ADMINISTRADOR
SUPERVISOR
OPERADOR

Bodegas:
ADMINISTRADOR

Usuarios:
ADMINISTRADOR

Productos:
ADMINISTRADOR

Conjuntos:
ADMINISTRADOR

Contenedores:
ADMINISTRADOR

Hojas de producción:
ADMINISTRADOR
SUPERVISOR

Monitoreo:
ADMINISTRADOR
SUPERVISOR

Reportes:
ADMINISTRADOR
SUPERVISOR

Surtido:
ADMINISTRADOR
OPERADOR

Recepción:
ADMINISTRADOR
OPERADOR
```

Aplicar tanto en backend Spring Security como en Thymeleaf.

La UI nunca sustituye autorización backend.

---

# 17. CSRF

No adoptar como solución final:

```java
csrf.disable()
```

JWT viaja mediante Cookies, así que el estado final requiere CSRF.

Cuando corresponda:

- integrar formularios Thymeleaf;
- integrar `POST`;
- integrar `PUT`;
- integrar `DELETE`;
- integrar `fetch()`;
- centralizar el soporte CSRF cuando sea posible.

No resolver errores deshabilitando CSRF globalmente.

---

# 18. Logging de seguridad

Integrar con Logging V1.

`INFO`:

```text
login exitoso
selección de Bodega
refresh exitoso
logout
cierre/revocación de sesión
```

`WARN`:

```text
credenciales inválidas
usuario inactivo
usuario sin Bodega
Bodega no autorizada
Access inválido
Refresh inválido
Refresh reutilizado
acceso denegado
```

`DEBUG`:

```text
Access expirado
resolución técnica del SecurityContext
```

El login exitoso sólo ocurre después de:

```text
Bodega
+
SesionEntity
+
tokens/cookies
```

---

# 19. Fases

## S1 — Dependencias e infraestructura base

Objetivo:

Preparar el proyecto para Spring Security, Thymeleaf Security y JJWT sin cambiar todavía el comportamiento funcional del login.

Revisar `pom.xml`.

Incorporar sólo las dependencias necesarias para:

```text
spring-boot-starter-security
thymeleaf-extras-springsecurity6
JJWT
```

Revisar si `spring-security-crypto` queda redundante.

No crear todavía:

- emisión JWT;
- filtro JWT funcional;
- Refresh Token;
- autorización final.

Criterios:

```text
proyecto compila
dependencias correctas
BCrypt actual sigue funcionando
sin cambios funcionales
```

---

## S2 — Usuario Spring Security real

Objetivo:

Integrar Usuario/Rol reales con Spring Security.

Crear:

```text
SgreaUserDetails
SgreaUserDetailsService
```

`SgreaUserDetailsService` implementa `UserDetailsService`.

Usar `UsuarioRepository`.

Mapear:

```text
username
password BCrypt
activo
rol
```

Authorities:

```text
ROLE_ADMINISTRADOR
ROLE_SUPERVISOR
ROLE_OPERADOR
```

Configurar:

```text
DaoAuthenticationProvider
AuthenticationManager
```

No migrar todavía el login.

No emitir JWT.

Criterios:

```text
usuarios reales cargan por UserDetailsService
roles correctos
usuario inactivo disabled
BCrypt correcto
proyecto compila
```

---

## S3 — Migrar credenciales a AuthenticationManager

Objetivo:

Reemplazar la validación manual de usuario/password por Spring Security.

Modificar:

```text
POST /login-bodega
```

para usar:

```text
AuthenticationManager.authenticate(...)
```

Conservar:

- UI actual;
- mensajes;
- flujo de Bodega.

No emitir JWT todavía.

No crear `SesionEntity` antes de Bodega.

Registrar:

```text
WARN credenciales inválidas
WARN usuario inactivo
```

Criterios:

```text
credenciales válidas funcionan
credenciales inválidas fallan
usuario inactivo falla
flujo de Bodega sigue funcionando
sin JWT
```

---

## S4 — Login pendiente y Bodega segura

Objetivo:

Cerrar el flujo de negocio antes de JWT.

Implementar login pendiente seguro.

Con múltiples Bodegas:

```text
HttpSession:
idUsuario
username
```

Eliminar `idUsuario` del navegador como fuente de verdad.

`POST /login-enter` debe validar `UsuarioBodega`.

Centralizar la finalización del login en Service.

Retirar `POST /login/crear-sesion` como endpoint público cuando sea seguro hacerlo.

Conservar una sola sesión activa por usuario.

Criterios:

```text
0/1/N Bodegas correctas
idBodega manipulado rechazado
idUsuario navegador no confiable
sesión anterior cerrada
sesión nueva correcta
```

---

## S5 — SecurityFilterChain y autorización real

Objetivo:

Introducir protección real de rutas.

Implementar `SecurityFilterChain`.

Aplicar la matriz de roles.

Permitir sólo recursos públicos necesarios:

```text
/login
recursos estáticos
rutas necesarias de autenticación
```

No usar:

```text
/**.permitAll()
```

Agregar `@EnableMethodSecurity` si corresponde.

Criterios:

```text
Administrador -> Catálogos/Supervisión/Operación
Supervisor -> Supervisión
Operador -> Operación
acceso directo por URL bloqueado
```

---

## S6 — Thymeleaf Security

Objetivo:

Migrar autorización visual.

Agregar namespace:

```text
xmlns:sec="http://www.thymeleaf.org/extras/spring-security"
```

Migrar principalmente `master.html`.

Reemplazar condiciones basadas exclusivamente en:

```text
session.idRol
```

por:

```text
sec:authorize
```

Usar `sec:authentication` cuando corresponda.

Conservar HttpSession para Bodega/UI mientras sea necesario.

Criterios:

```text
menús correctos por rol
usuario visible
Bodega visible
backend sigue siendo autoridad
```

---

## S7 — JWTTokenProvider + Access JWT

Objetivo:

Implementar generación/validación de Access Token.

Basarse conceptualmente en el provider del repositorio académico.

Configurar secreto y expiración en `application.properties` / variables de entorno.

No hardcodear secretos reales.

Implementar:

```text
generateAccessToken
validateAccessToken
extracción de claims
```

Validar:

```text
typ=access
```

No loggear JWT.

Criterios:

```text
firma válida
expiración
tipo validado
claims mínimos
secret configurable
```

---

## S8 — Cookie Access + JWTAuthenticationFilter

Objetivo:

Reconstruir autenticación desde Cookie JWT.

Emitir:

```text
token=<Access JWT>
```

como Cookie HttpOnly al completar login.

Implementar filtro JWT.

Validar también `SesionEntity`.

Reconstruir `SecurityContext`.

No depender de HttpSession como prueba de autenticación.

Criterios:

```text
Access válido -> autenticado
sin token -> anónimo
token inválido -> no autenticado
sesión inactiva -> rechazado
rol correcto
```

---

## S9 — Refresh Token

Objetivo:

Agregar Refresh JWT siguiendo la rama académica 11-1.

Implementar:

```text
typ=refresh
jti
refresh expiration
```

Emitir:

```text
refresh_token
```

`SesionEntity.token` contiene `jti`, no JWT.

Crear:

```text
POST /refresh
```

No aceptar Access como Refresh ni Refresh como Access.

Criterios:

```text
Access corto
Refresh más largo
tipos separados
jti asociado a SesionEntity
```

---

## S10 — Rotación y revocación de Refresh

Objetivo:

Hacer Refresh revocable.

`POST /refresh`:

```text
validar refresh
validar sesión
comparar jti
generar nuevo jti
actualizar SesionEntity.token
generar Access
generar Refresh
reemplazar cookies
```

El Refresh anterior debe fallar tras rotación.

Criterios:

```text
refresh vigente funciona
refresh anterior falla
sesión inactiva falla
usuario inactivo falla
```

---

## S11 — Logout completo

Objetivo:

Integrar logout Spring Security + SGREA.

`POST /logout`.

Debe:

```text
cerrar SesionEntity
invalidar Refresh
eliminar token
eliminar refresh_token
limpiar SecurityContext
invalidar HttpSession
```

Criterios:

```text
Access anterior falla
Refresh anterior falla
cookies eliminadas
sesión cerrada
```

---

## S12 — CSRF final

Objetivo:

Llegar al estado seguro final basado en Cookies.

CSRF debe quedar habilitado.

Adaptar:

```text
formularios Thymeleaf
POST
PUT
DELETE
fetch()
```

Centralizar soporte JavaScript.

No usar `csrf.disable()` como solución final.

Validar especialmente:

- CRUD
- Hoja
- Surtido
- Recepción
- Reportes
- Refresh
- Logout
- Login

Criterios:

```text
mutaciones legítimas funcionan
sin CSRF -> rechazo
GET no afectados
JWT Cookie sigue funcionando
```

---

## S13 — Validación integral

No agregar nuevas funcionalidades.

Probar:

```text
login válido
login inválido
usuario inactivo

usuario 0 Bodegas
usuario 1 Bodega
usuario N Bodegas
Bodega manipulada

Administrador
Supervisor
Operador

Access válido
Access inválido
Access expirado
sesión revocada

Refresh válido
Refresh inválido
Refresh expirado
Refresh reutilizado

logout

CSRF válido
CSRF ausente

Thymeleaf por rol
acceso directo URL

CRUD
HH Surtido
HH Recepción
Monitoreo
Reportes
Excel
PDF
```

Verificar que logs no expongan secretos.

No modificar DDL.

---

# 20. Formato obligatorio de cierre de cada fase

Al terminar responde exactamente con esta estructura:

```text
Fase implementada:

Archivos creados:

Archivos modificados:

Decisiones tomadas:

Compilación/pruebas:

Comportamiento validado:

Pendientes para la siguiente fase:
```

Después DETENTE.

No implementes ninguna fase posterior.

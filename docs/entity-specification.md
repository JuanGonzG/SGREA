# Entity and Business Rules Specification

Revision: 2026-10-14  
Scope: SGREA entities, persistence rules, operational workflows, security boundaries, and observable service behavior.

This document summarizes the current implemented contract of SGREA. Java class names, database identifiers, endpoint
paths, role names, and status names remain unchanged because they are part of the application contract.

## 1. Domain overview

SGREA (Sistema de Gestión de Recursos y Almacenes) manages technical equipment used in projects, productions, and
events. Resources are organized by warehouses. Physical dispatch and return operations preserve the production sheet,
warehouse context, physical unit, responsible user, timestamps, movement history, and container context when applicable.

Core concepts:

- `BodegaEntity`: warehouse and operational boundary.
- `ProductoEntity`: product type assigned to a warehouse.
- `ConjuntoEntity`: individually identifiable physical unit of a product.
- `HojaProduccionEntity`: production sheet used for preparation, dispatch, return, and closure.
- `DetalleHojaEntity`: requested, dispatched, and returned quantities for a product on a sheet.
- `ContenedorEntity`: reusable container with a global code and maximum capacity.
- `HojaContenedorEntity`: temporary and auditable use of a container within a production sheet.
- `MovimientoEntity`: immutable `Salida` or `Entrada` event for a physical unit.
- `SesionEntity`: persistent operational context that links a user to the selected warehouse.

The physical database baseline is `docs/esquema-actual.sql`. Hibernate runs with
`spring.jpa.hibernate.ddl-auto=validate`; the application does not create or migrate the schema automatically.

## 2. Roles and operational boundaries

Persistent roles:

| ID | Role | Spring Security authority |
|---:|---|---|
| 1 | Administrador | `ROLE_ADMINISTRADOR` |
| 2 | Supervisor | `ROLE_SUPERVISOR` |
| 3 | Operador | `ROLE_OPERADOR` |

The effective operational context is the authenticated user plus one active warehouse. A user may be associated with
multiple warehouses, but each active business session uses exactly one selected warehouse.

Current server-side route matrix:

- Administrative catalogs, users, products, physical units (`Conjunto`) and containers: Administrador.
- Production sheets, monitoring and reports: Administrador or Supervisor.
- HH Surtido and HH Recepción: Administrador or Operador.
- Login, refresh, error pages and static resources: available only as required by the authentication flow.

UI visibility is not the authorization boundary. `SecurityFilterChain` enforces direct URL access on the server.

## 3. Entity contracts

### `UsuarioEntity` and `RolEntity`

- The application requires `nombre`, `username`, `password`, `activo`, and `rol`.
- `username` is trimmed and checked case-insensitively for duplicates.
- Passwords are stored with BCrypt and are never exposed by read DTOs.
- Inactive users cannot authenticate because `activo` maps to `UserDetails.isEnabled()`.
- A user must retain at least one `UsuarioBodegaEntity` association.
- Physical deletion is blocked when sessions, movements, or `HojaContenedor` references exist.
- Supported roles are limited to Administrador, Supervisor and Operador.

### `BodegaEntity`

- `nombre` is required and limited to 100 characters.
- `descripcion` is required and limited to 255 characters.
- Names are trimmed and must be unique case-insensitively at the application level.
- Deletion is blocked when products, production sheets, user associations, or sessions reference the warehouse.

### `UsuarioBodegaEntity`

- Implements the many-to-many relationship between users and warehouses through an explicit association entity.
- The composite key is `(idUsuario, idBodega)`.
- Both key components participate in `equals` and `hashCode`.
- Login warehouse selection is revalidated on the server against this relationship.

### `SesionEntity`

- `token`, `fechaInicio`, and `activa` are required; `fechaFin` is nullable.
- Each session belongs to one user and one warehouse.
- Only one active business session is allowed per user.
- Creating a new session closes the previous active session within the same transaction.
- The `token` column stores the current refresh identifier (`jti`), not a complete JWT.
- Logout closes the business session, clears the Spring Security context, invalidates the auxiliary HTTP session, and
  expires the Access and Refresh cookies.

### `ProductoEntity`

- `nombre`, `descripcion`, `activo`, and `urlImagen` are required by the application.
- `nombre` is limited to 100 characters; `descripcion` and `urlImagen` to 255.
- Every product belongs to one warehouse.
- A new product requires a JPEG, PNG, or WEBP image of at most 5 MB.
- Product queries and mutations are limited to the active authorized warehouse.

### `HojaProduccionEntity`

- `nombreProyecto`, `cliente`, and `fechaSalida` are required.
- `fechaEstimadaRegreso` is optional.
- Every production sheet belongs to one warehouse and one `EstadoHoja`.
- New sheets start in `Creada`.
- The current state is preserved on ordinary updates.
- States with ID greater than or equal to 2 make the administrative CRUD read-only.
- Warehouse context is assigned and validated by the server.

Implemented lifecycle:

```text
Creada -> Por surtir -> Surtiendo -> En llamado -> Recibiendo -> Completado
```

### `DetalleHojaEntity`

- Each detail belongs to one production sheet and one product.
- `(id_hoja, id_producto)` is unique.
- `cantidadSolicitada` must be greater than zero.
- `cantidadSurtida` starts functionally at `0` and remains structurally nullable in the academic schema.
- `cantidadDevuelta` is persisted as `INT NOT NULL DEFAULT 0`.
- Operational invariant:

```text
0 <= cantidadDevuelta <= cantidadSurtida <= cantidadSolicitada
```

- Details cannot be modified after the production sheet becomes read-only.

### Catalog entities

The following catalogs are read-only from the application:

- `EstadoConjuntoEntity`: `1 Disponible`, `2 Surtido`, `3 Mantenimiento`, `4 Fuera de servicio`.
- `EstadoContenedorEntity`: `1 Disponible`, `2 En uso`, `3 Mantenimiento`, `4 Fuera de servicio`.
- `EstadoHojaEntity`: `1 Creada`, `2 Por surtir`, `3 Surtiendo`, `4 En llamado`, `5 Recibiendo`, `6 Completado`.
- `TipoMovimientoEntity`: `1 Salida`, `2 Entrada`.

`SURTIDO` and `EN_USO` are operational states controlled by movement and HH services, not by ordinary administrative
forms. `FUERA_DE_SERVICIO` is terminal in the current version.

### `ConjuntoEntity`

- `idConjunto` is generated by the backend with the global format `CON-000001`.
- `producto`, `estadoConjunto`, and `fechaAlta` are required.
- Warehouse context is derived from `producto.bodega`; no duplicate warehouse FK is stored.
- New physical units start in `DISPONIBLE`.
- The client cannot choose the ID, creation date, initial state, or change the product after creation.
- Administrative transitions exclude `SURTIDO`.
- Operational transitions `DISPONIBLE -> SURTIDO -> DISPONIBLE` are handled by `MovimientoService`.
- A physical unit with movement history cannot be physically deleted; operational retirement uses
  `FUERA_DE_SERVICIO`.

### `ContenedorEntity`

- `idContenedor` is an auto-increment technical key.
- `codigo` is generated by the backend, unique, and follows `CONT-000001`.
- `capacidad` is required and must be greater than zero.
- New containers start in `DISPONIBLE`.
- Capacity and observations can only be edited while the container is not `EN_USO`.
- A container is reusable and is associated with production sheets only through `HojaContenedorEntity`.
- A container with historical `HojaContenedor` usage cannot be physically deleted; operational retirement uses
  `FUERA_DE_SERVICIO`.

### `HojaContenedorEntity`

`HojaContenedorEntity` represents temporary container usage within a production sheet. It stores:

- production sheet;
- container;
- assignment timestamp;
- optional load-closure timestamp;
- optional release timestamp;
- assigning user;
- optional closing user;
- optional releasing user;
- optional observations.

An active assignment remains tied to the operational workflow until the corresponding container usage is released.

### `MovimientoEntity`

- Represents an immutable historical event.
- Required by persistence: movement type, physical unit, production sheet, date, and user.
- `detalleHoja` and `hojaContenedor` are structurally nullable; service rules determine when they are required.
- Historical foreign keys do not use cascading deletes.
- `SALIDA` requires the corresponding production-sheet detail and an open container load.
- `ENTRADA` requires a previous pending `SALIDA` for the same production sheet and physical unit.
- An `ENTRADA` may use a different return container or no container.
- User and timestamp are supplied by the server, never trusted from the browser.

## 4. Operational workflows

### Login and warehouse selection

1. `LoginController` delegates credential validation to `AuthenticationManager`.
2. `DaoAuthenticationProvider` uses `SgreaUserDetailsService` and BCrypt.
3. `SgreaUserDetailsService` loads the persisted `UsuarioEntity`.
4. Invalid credentials and inactive users are rejected.
5. Authorized warehouses are loaded through `UsuarioBodegaEntity`.
6. One warehouse is selected automatically; multiple warehouses require `POST /login-enter`.
7. The selected warehouse is revalidated on the server.
8. The previous active business session is closed.
9. A new `SesionEntity` is created.
10. Access and Refresh JWTs are generated and returned as HttpOnly cookies.

Authentication endpoints:

```text
GET  /login
POST /login-bodega
POST /login-enter
POST /refresh
POST /logout
```

`POST /refresh` is permitted at the authorization layer so it can operate after Access Token expiration. It still
requires CSRF protection and a valid, non-revoked Refresh Token associated with an active `SesionEntity`.

### HH Surtido / `SALIDA`

1. Select an authorized production sheet.
2. Assign an available container and create `HojaContenedor`.
3. Scan or select a physical unit.
4. Validate warehouse, product/detail match, availability, open load, and container capacity.
5. Reject duplicate pending dispatches.
6. Persist `Movimiento SALIDA` with sheet, detail, physical unit, container, user, and server timestamp.
7. Change the physical unit to `SURTIDO` and increment `cantidadSurtida` transactionally.

### HH Recepción / `ENTRADA`

1. Load physical units with a pending dispatch for the production sheet.
2. Identify the returning unit.
3. Require a previous pending `SALIDA`.
4. Resolve the matching detail by production sheet and product.
5. Persist `Movimiento ENTRADA`, optionally linked to a return container.
6. Change the physical unit to `DISPONIBLE` and increment `cantidadDevuelta`.
7. Allow partial returns.
8. Complete the sheet only when all returned quantities match dispatched quantities and all required container usages
   are released.

### Monitoring and reports

Monitoring is read-only and presents active Surtido/Recepción operations, progress, container usage, and recent
movement context.

Reports are read-only from the domain perspective and are warehouse-scoped. They support JSON, Excel, and PDF output.
Report generation does not mutate operational inventory state.

## 5. Security implementation

The implemented security stack includes:

- Spring Security;
- persisted SGREA users and BCrypt;
- Thymeleaf Security;
- JJWT;
- Access JWT in an HttpOnly cookie;
- Refresh JWT in a separate HttpOnly cookie;
- refresh rotation and revocation through `SesionEntity`;
- role-based authorization;
- CSRF protection.

### Access Token

The current Access JWT uses:

JWT header:

```text
typ=access
```

Payload claims:

```text
sub
uid
sid
bid
role
iat
exp
```

The JWT filter validates signature, expiration, token type, required claims, active `SesionEntity`, matching user,
matching warehouse, and current enabled-user status before creating the `SecurityContext`.

The user is loaded again through `SgreaUserDetailsService`, so current authorities come from the persisted user rather
than being trusted only from the token payload.

### Refresh Token

The current Refresh JWT uses:

JWT header:

```text
typ=refresh
```

Payload/session context includes:

```text
sub
uid
sid
bid
role
jti
iat
exp
```

The Refresh Token is accepted only when:

- the JWT is valid and of type `refresh`;
- the referenced `SesionEntity` exists and is active;
- user and warehouse claims match the session;
- the user is still active;
- JWT `jti` matches `SesionEntity.token`.

Rotation generates a new `jti`, updates `SesionEntity.token`, and emits a new Access/Refresh pair. The previous refresh
identifier is therefore no longer accepted after rotation.

The application exposes `POST /refresh` and also supports transparent token renewal from the JWT filter when the Access
Token cannot authenticate and a valid Refresh cookie is available.

### Cookies and CSRF

Authentication cookies use:

```text
HttpOnly=true
Path=/
SameSite=Lax
Secure=<environment configuration>
```

CSRF remains enabled because browsers attach cookies automatically.

Thymeleaf forms include the Spring Security CSRF token. The global `csrf.js` wrapper adds the CSRF header to unsafe
`fetch` requests (`POST`, `PUT`, `DELETE`, and other non-safe methods).

### Logout

`POST /logout`:

1. closes the active `SesionEntity`;
2. clears the `SecurityContext`;
3. invalidates the auxiliary HTTP session;
4. expires Access and Refresh cookies.

An inactive business session prevents both the previous Access context and the previous Refresh Token from being used
successfully.

### Sensitive information

The application must not log:

- passwords;
- BCrypt hashes;
- complete Access JWTs;
- complete Refresh Tokens;
- JWT secrets;
- datasource credentials;
- complete cookie values.

## 6. Persistence baseline

The current DDL includes:

- `detallehoja.cantidad_devuelta INT NOT NULL DEFAULT 0`;
- `tipomovimiento` with IDs 1 (`Salida`) and 2 (`Entrada`);
- `hojacontenedor` assignment, load-closure, release, and audit-user columns;
- `movimiento` with nullable `id_detalle` and `id_hoja_contenedor`;
- indexes supporting sheet/date, physical-unit/date, container/date, and user/date access patterns;
- historical foreign keys without cascading deletes.

`SesionEntity.token` stores the rotating Refresh `jti`; no additional Refresh Token table is required by the current
implementation.

Schema changes must be reflected explicitly in `docs/esquema-actual.sql`; they must not be inferred from JPA mappings.

## 7. Known follow-up and non-goals

The current version is functionally deliverable for the academic scope. The following items remain follow-up work and
must not be described as already implemented:

- broader automated coverage across all three roles and full operational workflows;
- uniform exception-to-HTTP mapping for every CRUD/domain error;
- full Bean Validation coverage and standardized validation responses;
- centralization of repeated warehouse-authorization checks;
- additional date and quantity edge-case validation;
- image content inspection beyond declared MIME type and orphan-file cleanup;
- automatic expiration/cleanup of old business sessions;
- additional persisted administrative audit beyond logs, `SesionEntity`, `Movimiento`, and `HojaContenedor`.

Out of scope for the current academic delivery:

- OAuth2;
- GitHub OAuth;
- Keycloak;
- LDAP;
- OpenID Connect;
- a separate authorization server.

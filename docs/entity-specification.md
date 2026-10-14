# Entity and Business Rules Specification

Revision: 2026-10-14  
Scope: SGREA entities, persistence rules, operational workflows, security boundaries, and observable service behavior.

This document is the English summary of the project specification. Entity names, table names, column names, endpoints,
role names, status names, and Java identifiers remain unchanged because they are part of the application contract.

## 1. Domain overview

SGREA (Sistema de Gestión de Recursos y Almacenes) manages technical equipment used in projects, productions, and
events. Resources are organized by warehouses. Every physical dispatch and return must preserve the project, warehouse,
physical unit, container, operator, timestamps, and historical movement.

Core concepts:

- `BodegaEntity`: warehouse and operational boundary.
- `ProductoEntity`: product type assigned to a warehouse.
- `ConjuntoEntity`: individually identifiable physical unit of a product.
- `HojaProduccionEntity`: production sheet used for preparation, dispatch, return, and closure.
- `DetalleHojaEntity`: requested, dispatched, and returned quantities for a product on a sheet.
- `ContenedorEntity`: reusable container with a global code and capacity.
- `HojaContenedorEntity`: temporary use of a container within a production sheet.
- `MovimientoEntity`: immutable `Salida` or `Entrada` event for a physical unit.
- `SesionEntity`: persistent operational context for a user and selected warehouse.

The database schema in `docs/esquema-actual.sql` is the physical source of truth. Hibernate runs with
`spring.jpa.hibernate.ddl-auto=validate`; the application must not infer or silently alter the DDL.

## 2. Roles and operational boundaries

The persistent roles are:

| ID | Role | Authority |
|---:|---|---|
| 1 | Administrador | `ROLE_ADMINISTRADOR` |
| 2 | Supervisor | `ROLE_SUPERVISOR` |
| 3 | Operador | `ROLE_OPERADOR` |

The effective context is the authenticated user plus one active warehouse. A user may be associated with several
warehouses, but every operation runs inside the selected warehouse.

The current server-side route matrix is:

- Catalogs, users, products, sets, and containers: Administrador.
- Production sheets, monitoring, and reports: Administrador or Supervisor.
- HH Surtido and HH Recepción: Administrador or Operador.
- Login, refresh, error pages, and static resources: public only where required by authentication.

The menu is only a presentation aid. `SecurityFilterChain` is the authority for direct URL access.

## 3. Entity contracts

### `UsuarioEntity` / `RolEntity`

- `nombre`, `username`, `password`, `activo`, and `rol` are required.
- `username` is trimmed and checked case-insensitively for duplicates.
- Passwords are stored with BCrypt and are never exposed by read DTOs.
- Inactive users cannot authenticate because `activo` maps to `UserDetails.isEnabled()`.
- A user must keep at least one `UsuarioBodegaEntity` association.
- Physical deletion is blocked when sessions, movements, or `HojaContenedor` references exist.

### `BodegaEntity`

- `nombre` is required and limited to 100 characters.
- `descripcion` is required and limited to 255 characters.
- Names are trimmed and must be unique case-insensitively.
- Deletion is blocked when products, sheets, user associations, or sessions reference the warehouse.

### `UsuarioBodegaEntity`

- Implements the user–warehouse relationship with `@EmbeddedId` and `@MapsId`.
- The composite key is `(idUsuario, idBodega)`.
- Both key components participate in `equals` and `hashCode`.
- Login selection is validated on the server against this relationship.

### `SesionEntity`

- `token`, `fechaInicio`, and `activa` are required; `fechaFin` is nullable.
- Each session belongs to one user and one warehouse.
- Only one active business session may exist per user.
- Creating a new session closes the previous active session in the same transaction.
- Logout closes the session, clears the security context, invalidates the HTTP session, and expires token cookies.
- The `token` column stores the current refresh identifier (`jti`), not a complete JWT.

### `ProductoEntity`

- `nombre`, `descripcion`, `activo`, and `urlImagen` are required.
- `nombre` is limited to 100 characters; `descripcion` and `urlImagen` to 255.
- Every product belongs to one warehouse.
- A new product requires a JPEG, PNG, or WEBP image of at most 5 MB.
- Product queries and mutations are restricted to the active authorized warehouse.

### `HojaProduccionEntity`

- `nombreProyecto`, `cliente`, and `fechaSalida` are required.
- `fechaEstimadaRegreso` is optional.
- Every sheet belongs to one warehouse and starts with the first `EstadoHoja` catalog value.
- A sheet preserves its current state on update.
- States with ID >= 2 make the sheet read-only.
- The active warehouse is assigned by the server; clients cannot choose an arbitrary warehouse.

### `DetalleHojaEntity`

- Each detail belongs to one sheet and one product.
- `(id_hoja, id_producto)` is unique.
- `cantidadSolicitada > 0`.
- `cantidadSurtida` starts at `0` functionally and remains structurally nullable as required by the academic schema.
- `cantidadDevuelta` is persisted as `INT NOT NULL DEFAULT 0`.
- Operational invariant: `0 <= cantidadDevuelta <= cantidadSurtida <= cantidadSolicitada`.
- Details cannot be changed when the sheet is read-only.

### Catalog entities

Catalogs are closed and are queried by the application; they do not have administrative CRUD:

- `EstadoConjuntoEntity`: `1 Disponible`, `2 Surtido`, `3 Mantenimiento`, `4 Fuera de servicio`.
- `EstadoContenedorEntity`: `1 Disponible`, `2 En uso`, `3 Mantenimiento`, `4 Fuera de servicio`.
- `TipoMovimientoEntity`: `1 Salida`, `2 Entrada`.
- `EstadoHojaEntity`: identifiers and names defined by the current DDL.

`SURTIDO` and `EN_USO` are operational states. They are assigned by movement/HH services, not by ordinary catalog
forms. `FUERA_DE_SERVICIO` is terminal in the first version.

### `ConjuntoEntity`

- `idConjunto` is generated by the backend with the global format `CON-000001`.
- `producto`, `estadoConjunto`, and `fechaAlta` are required.
- The warehouse is derived from `producto.bodega`; no duplicate warehouse FK is stored.
- New units start in `DISPONIBLE`.
- The client cannot choose the ID, creation date, initial state, or product after creation.
- Administrative transitions exclude `SURTIDO`; operational transitions belong to `MovimientoService`.
- A unit with movement history cannot be physically deleted and must be retired as `FUERA_DE_SERVICIO`.

### `ContenedorEntity`

- `idContenedor` is an auto-increment technical key.
- `codigo` is backend-generated, unique, and follows `CONT-000001`.
- `capacidad` is required and greater than zero.
- New containers start in `DISPONIBLE`.
- Capacity and observations can only be edited when the container is not `EN_USO`.
- A container is reusable and is associated with a sheet only through `HojaContenedorEntity`.
- A container with operational history cannot be physically deleted and must be retired as `FUERA_DE_SERVICIO`.

### `HojaContenedorEntity`

The relation is temporary and auditable. It stores the sheet, container, assignment date, optional load closure date,
optional release date, and the user who assigned, closed, or released the container.

### `MovimientoEntity`

- Represents an immutable historical event.
- Required: movement type, physical unit, sheet, date, and user.
- `detalleHoja` and `hojaContenedor` are structurally nullable; business rules determine when they are required.
- Historical foreign keys do not use `ON DELETE CASCADE`.
- `SALIDA` requires a detail and an open sheet-container load.
- `ENTRADA` requires a pending `SALIDA` for the same sheet and unit.
- User and timestamp come from the server, never from the browser.

## 4. Operational workflows

### Login and warehouse selection

1. `LoginController` delegates credentials to `AuthenticationManager` and `DaoAuthenticationProvider`.
2. `SgreaUserDetailsService` loads the real `UsuarioEntity` through `UsuarioRepository`.
3. Invalid or inactive users are rejected.
4. The application obtains the user's associated warehouses.
5. One warehouse is selected automatically; multiple warehouses require `POST /login-enter`.
6. The selected warehouse is revalidated against `UsuarioBodegaEntity` on the server.
7. The previous active business session is closed.
8. A new `SesionEntity`, Access JWT, and Refresh JWT are created and sent as HttpOnly cookies.

Final login endpoints:

```text
GET  /login
POST /login-bodega
POST /login-enter
POST /refresh
POST /logout
```

### HH Surtido / `SALIDA`

1. Select an authorized sheet.
2. Assign an available container and create `HojaContenedor`.
3. Scan or select a physical unit.
4. Verify warehouse, product/detail match, availability, open load, and capacity.
5. Reject duplicate pending dispatches.
6. Persist `Movimiento` with sheet, detail, unit, container, user, and server timestamp.
7. Change the unit to `SURTIDO` and increment `cantidadSurtida` transactionally.

### HH Recepción / `ENTRADA`

1. Load units with a pending dispatch for the sheet.
2. Identify the returning unit.
3. Require a previous pending `SALIDA`.
4. Resolve the detail by sheet and product.
5. Persist `Movimiento ENTRADA`, optionally linked to a return container.
6. Change the unit to `DISPONIBLE` and increment `cantidadDevuelta`.
7. Allow partial returns.
8. Complete the sheet only when all quantities are returned and all container usages are released.

### Monitoring and reports

Monitoring is read-only and presents active Surtido/Recepción operations, progress, containers, and movement history.
Reports are read-only, warehouse-scoped, and support JSON, Excel, and PDF output. Neither module changes the domain.

## 5. Security implementation

The current security stack includes Spring Security, Thymeleaf Security, BCrypt, JJWT, HttpOnly cookies, Refresh Token
rotation, logout revocation, role authorization, and CSRF protection.

Access JWT claims are limited to:

```text
sub, uid, sid, bid, role, typ=access, iat, exp
```

Refresh JWTs use a separate cookie and include `jti` plus the minimum user/session claims. The refresh identifier must
match `SesionEntity.token`; rotation changes it and invalidates the previous refresh token.

The JWT filter validates signature, expiration, type, claims, active session, user, warehouse, and enabled status before
creating the `SecurityContext`.

Cookies are configured with `HttpOnly=true`, `Path=/`, `SameSite=Lax`, and configurable `Secure` behavior. CSRF remains
enabled because browsers send authentication cookies automatically. Thymeleaf forms and the global `fetch` wrapper use
the server-provided CSRF token for unsafe requests.

No password, BCrypt hash, complete JWT, refresh token, secret, datasource credential, or complete cookie may be logged.

## 6. Persistence baseline

The frozen DDL includes:

- `detallehoja.cantidad_devuelta INT NOT NULL DEFAULT 0`;
- `tipomovimiento` with IDs 1 and 2;
- `hojacontenedor` with assignment, closure, release, and audit-user columns;
- `movimiento` with nullable `id_detalle` and `id_hoja_contenedor`;
- indexes by sheet/date, unit/date, container/date, and user/date;
- historical foreign keys without cascading deletes.

The application must not add a Refresh Token table or alter keys, relations, nullability, or column types without an
explicit DDL decision.

## 7. Known non-functional follow-up

The following are intentionally outside this summary's claim of implementation and may require future work:

- full automated and cross-role verification;
- uniform exception-to-HTTP mapping for every CRUD and domain error;
- extending Bean Validation to every DTO and standardizing validation responses;
- centralizing all warehouse authorization checks in one reusable service;
- validating date and quantity edge cases not yet enforced uniformly;
- image content inspection and orphan-file cleanup;
- automatic expiration and cleanup of old business sessions;
- a decision on persisted administrative audit beyond logs, `SesionEntity`, and `Movimiento`.

These follow-up items must not be treated as implemented merely because the corresponding UI control exists.

# Pruebas de la API con Bruno

Esta carpeta contiene la colección de pruebas de la API REST de SIGRA para [Bruno](https://www.usebruno.com), empezando por **RF-03 (Gestión de asignaturas)**.

## ¿Qué es Bruno y por qué lo usamos?

Bruno es un cliente de APIs, parecido a Postman, con una diferencia importante para un equipo: **cada petición es un archivo de texto (`.bru`) que vive en el repositorio**. Eso significa que:

- La colección se versiona en Git junto al código: se revisa en los Pull Request y se ve en el historial qué prueba cambió y por qué.
- No hace falta cuenta, inicio de sesión ni sincronización en la nube.
- Todo el equipo ejecuta exactamente las mismas pruebas, con los mismos datos.

## Contenido

```
bruno/
├── README.md                       Este archivo
└── SIGRA/                          La colección (ábrela desde Bruno)
    ├── bruno.json                  Definición de la colección
    ├── environments/local.bru      Variables del entorno local (URL e ids de prueba)
    ├── seed/seed-asignaturas.sql   Datos de prueba de RF-03 (asignaturas)
    ├── seed/seed-auth.sql          Datos de prueba de RF-04 (login)
    ├── asignaturas/                Pruebas de RF-03, en orden
        ├── 00-salud/               El backend responde (1)
        ├── 01-registrar/           RF-03a: registro y validaciones (14)
        ├── 02-consultar/           RF-03b: consulta con filtros (17)
        ├── 03-obtener-modificar/   RF-03c: consulta por id y modificación (8)
        ├── 04-activar/             RF-03d: activación con 5 a 7 RA activos y reactivación (10)
        ├── 05-inactivar/           RF-03d: inactivación, cascada de RA y reactivación (8)
        └── 06-cors/                CORS para el frontend en http://localhost:4200 (2)
    └── auth/                       Pruebas de RF-04, en orden
        ├── 01-login/               Login correcto y validaciones de entrada
        ├── 02-intentos-fallidos/   Bloqueo tras 5 intentos fallidos (423)
        └── 03-seguridad-respuestas/ Respuestas de error sin detalles técnicos (RNF-17)
```

Cada carpeta tiene un `folder.bru` con su `seq`, que fija el orden de ejecución, y cada petición lleva un prefijo numérico (`01-...`, `02-...`) con su `seq` dentro de la carpeta. **El orden importa**: algunas pruebas cambian el estado de las asignaturas de prueba (por ejemplo, `04-activar` deja `BRU05` en ACTIVA y `05-inactivar` la inactiva después; `04-activar` reactiva `BRU11` y `05-inactivar` reactiva `BRU10` y la vuelve a inactivar).

## Instalación

1. Descarga e instala Bruno desde https://www.usebruno.com/downloads.
2. (Opcional) Para ejecutar la colección desde la terminal, instala el CLI:

   ```bash
   npm i -g @usebruno/cli
   ```

## Requisitos

- El backend corriendo en `http://localhost:8080` (ver el README de la raíz).
- `SIGRA/.env` creado a partir de `SIGRA/.env.example`, con `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`.
- PostgreSQL con la base `postgres` y el **schema** `sigra` creado (`sigra` es un schema, no una base de datos). Para una base vacía, ejecuta `docs/db/SIGRA_SCHEMA_CURRENT.sql`. Hibernate solo valida el esquema (`ddl-auto=validate`): no crea tablas.
- Todos los comandos `psql` de esta guía usan `-d postgres`. Para que las consultas sin prefijo resuelvan dentro de `sigra`, se pasa `PGOPTIONS='-c search_path=sigra,public'`. Así el seed de RF-03 funciona sin cambiar su SQL.

## Cómo ejecutar las pruebas

### 1. Cargar los datos de prueba

Desde la **raíz del repositorio**, en PowerShell. Ajusta la ruta de `psql.exe` a tu versión de PostgreSQL y `-p` al puerto de tu servidor local:

```powershell
$env:PGOPTIONS = "-c search_path=sigra,public"
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -p <PUERTO> -d postgres -f bruno/SIGRA/seed/seed-asignaturas.sql
```

El script crea dos programas (uno activo y uno inactivo), diez asignaturas con el prefijo `BRU` y sus resultados de aprendizaje, y al final muestra un resumen con los RA activos de cada una. Es **idempotente**: borra los datos `BRU` anteriores y los vuelve a crear.

> **Vuelve a ejecutar el seed antes de cada corrida completa de la colección.** Las pruebas cambian el estado de algunas asignaturas y, sin el seed, la segunda corrida fallaría (por ejemplo, `BRU05` ya no estaría en BORRADOR).

### 2. Cargar los datos de RF-04 (autenticación)

El seed de auth opera solo sobre el schema `sigra` (define su propio `search_path` y no toca `public`). Es idempotente.

**Base nueva (vacía):**

1. Crea el esquema actual: `docs/db/SIGRA_SCHEMA_CURRENT.sql`.
2. Arranca el backend (`.\gradlew.bat bootRun` dentro de `SIGRA/`). Con `ddl-auto=validate` debe arrancar sin errores de esquema.
3. Ejecuta el seed de auth (paso siguiente).

**Base existente con el modelo antiguo de `profesor`:**

1. Haz un backup completo de la base.
2. Ejecuta `docs/db/RF04_schema_preflight.sql` (solo lectura) y revisa su resultado.
3. Si el preflight lo permite, ejecuta `docs/db/RF04_usuario_migration.sql`. Contiene cambios de esquema: se ejecuta a mano y con supervisión. En la base de referencia ya está aplicada.
4. Arranca el backend.

**Seed de auth (en ambos casos, y antes de cada corrida de `auth/`):**

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -p <PUERTO> -d postgres -v ON_ERROR_STOP=1 -f bruno/SIGRA/seed/seed-auth.sql
```

Para los logins de administrador y estudiante (`auth/01-login/13` y `14`), ejecuta también `bruno/SIGRA/seed/seed-roles.sql`: crea un administrador y un estudiante de prueba con la misma contraseña de `seed-auth.sql`.

Después ejecuta la colección de `auth/` (sección 4).

### Flujo de RF-04 en Bruno

1. PostgreSQL debe estar activo en el puerto de tu `DB_URL`.
2. El backend debe conectar a `postgres` con `currentSchema=sigra`.
3. El schema `sigra` debe existir y tener las tablas (baseline o migración).
4. Ejecuta el seed de auth si necesitas reiniciar los usuarios de prueba (`profesor.bruno@uco.net.co` y `profesor.bloqueo@uco.net.co`).
5. Arranca el backend.
6. Ejecuta la colección `auth/` (desde Bruno, o con `bru run auth -r --env local`).

### Estados esperados

| Código | Caso |
| --- | --- |
| 200 | Login correcto. Devuelve `token` (JWT), `tipo`, `expiraEn` y `usuario` |
| 400 | Validación de entrada (correo o contraseña vacíos, o dominio distinto de `@uco.net.co`) |
| 401 | Credenciales incorrectas. El mensaje es el mismo aunque el correo no exista |
| 423 | Bloqueo temporal de 15 minutos tras 5 intentos fallidos |

Secuencia de bloqueo (`auth/02-intentos-fallidos/`), sobre `profesor.bloqueo@uco.net.co`:

1. Se envían **5 contraseñas incorrectas** consecutivas → cada una responde 401.
2. El contador se **persiste** en `sigra.usuario.intentos_fallidos` (los fallos se guardan aunque la respuesta sea un error).
3. El **siguiente intento, durante el bloqueo**, responde 423, incluso con la contraseña correcta. No se evalúa la contraseña.

> **Estado de la validación:** esta colección y su flujo **ya fueron ejecutados manualmente** con Bruno. Todos los códigos y mensajes coincidieron con esta guía. Esa validación es manual: no hay una ejecución automatizada de la colección en el repositorio.

### 3. Arrancar el backend

```powershell
cd SIGRA
.\gradlew.bat bootRun
```

### 4. Ejecutar la colección

**Desde Bruno:**

1. *Open Collection* → selecciona la carpeta `bruno/SIGRA`.
2. Arriba a la derecha, elige el entorno **`local`**.
3. Clic derecho sobre la colección → **Run** → *Run Collection*.

**Desde la terminal** (con el CLI instalado), estando en `bruno/SIGRA`:

```bash
bru run . -r --env local
```

`-r` hace que se ejecuten también las subcarpetas.

## Qué se verifica

- Los códigos HTTP de cada caso (201, 200, 400, 401, 404, 409, 423).
- En `auth/`: el login devuelve 200 con token o el código de error que corresponde, y ninguna respuesta de error revela trazas, SQL ni nombres internos de clases (RNF-17).
- Los mensajes reales del backend (por ejemplo, «al menos 5» o «máximo 7» al activar).
- En todas las respuestas de error, que el cuerpo tenga `timestamp`, `status`, `error` y `mensaje`, y que **no** exponga trazas ni detalles técnicos (RNF-17).
- Que la consulta cuente solo los RA **activos** (la asignatura `BRU4I` tiene 4 activos y 3 inactivos, y cuenta 4).
- Que la búsqueda no distinga mayúsculas ni tildes (`programacion bruno` encuentra «Programación Bruno»).
- Que inactivar no borra la asignatura y deja sus RA activos en INACTIVO (cascada).
- Que una asignatura INACTIVA se puede reactivar y recupera **solo** los RA inactivados con ella (`BRU11` y `BRU10` vuelven con 5 RA activos). Es una decisión del equipo de asignaturas, distinta del SRS 3.2.3d, que solo define Borrador → Activa.
- Que CORS permite `http://localhost:4200` y rechaza otros orígenes. La segunda prueba de CORS no revisa el formato de error, porque ese 403 lo genera Spring antes de llegar al manejador de errores del módulo.

### Por qué no hay una prueba con JSON roto

Bruno no puede enviar un JSON sintácticamente inválido: si el cuerpo no es JSON válido, lo envía como una cadena de texto JSON. Por eso el caso de «cuerpo ilegible» se prueba con JSON **válido** que el backend no puede interpretar: un arreglo (`[1, 2, 3]`) en lugar de un objeto, y un `programaId` que no es un UUID. En ambos casos el backend responde 400 con «El cuerpo de la petición no es válido.».

### Comprobar la cascada en la base de datos

Después de ejecutar la colección, los RA de `BRU10` (inactivada en `05-inactivar`, reactivada y vuelta a inactivar) deben haber quedado todos en INACTIVO y marcados con `inactivado_con_asignatura = true`:

```sql
SELECT codigo, estado, inactivado_con_asignatura FROM resultado_aprendizaje WHERE asignatura_id = '00000000-0000-4000-b000-000000000007';
```

## Cómo agregar las pruebas de otro módulo

1. Crea una carpeta bajo `bruno/SIGRA/` con el nombre del módulo (por ejemplo, `profesores/`) y su `folder.bru`:

   ```
   meta {
     name: Profesores (RF-01)
     seq: 2
   }
   ```

   El `seq` define en qué orden se ejecuta respecto a los demás módulos (`asignaturas` tiene `seq: 1`).
2. Dentro, organiza las peticiones en subcarpetas numeradas, cada una con su `folder.bru` y su `seq`, igual que en `asignaturas/`.
3. Si necesitas datos de prueba, agrega un script en `seed/` (idempotente y con un prefijo propio, como `BRU` aquí) y sus ids fijos en `environments/local.bru`.
4. Ejecuta `bru run . -r --env local` para comprobar que todo se lee y corre en orden.

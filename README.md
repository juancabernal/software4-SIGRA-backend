# SIGRA — Backend

**Software Integral de Gestión de Resultados de Aprendizaje**
Universidad Católica de Oriente · Ingeniería de Software 4 · Equipo B · 2026

API REST en Spring Boot que soporta la aplicación web [SIGRA Frontend](https://github.com/juancabernal/software4-SIGRA-frontend).

---

## 1. Integrantes

| Nombre | Rol en el proyecto | Correo |
| --- | --- | --- |
| Juan Camilo Bernal Carmona | Project Manager, Arquitecto y Desarrollador Backend | juan.bernal8928@uco.net.co |
| Jean Paul Ortiz Restrepo | Líder Técnico y Analista de Requisitos | jean.ortiz1424@uco.net.co |
| José Alejandro Valencia | Desarrollador Backend y Base de Datos | jose.valencia1373@uco.net.co |
| Simón Tabares Arias | Analista de Requisitos y Calidad (QA) | simon.tabares2225@uco.net.co |
| Juan José Narváez Marín | Ingeniero de Pruebas (QA/Testing) | juan.narvaez9044@uco.net.co |
| Santiago Torres Castaño | Diseñador UX/UI y Desarrollador Frontend | santiago.torres0381@uco.net.co |
| Andrés Felipe Vélez Alcaraz | Desarrollador Frontend | andres.velez5136@uco.net.co |

Docente y cliente del proyecto: **Luz Mery Ríos Alzate**.

---

## 2. Propósito

SIGRA permite que los profesores evalúen el aprendizaje de sus estudiantes tomando los **Resultados de Aprendizaje (RA)** de cada asignatura como unidad de medida, en lugar de la nota suelta por actividad.

Este repositorio contiene el **backend**: la API REST, el modelo de datos y las reglas de negocio que sostienen el ciclo central del sistema:

> **evaluar → analizar → mejorar → reevaluar**

El backend es responsable de:

- Autenticar usuarios y emitir los tokens de sesión (RF-04).
- Aplicar el control de acceso por rol en cada endpoint, según la Matriz RBAC del SRS (RF-05).
- Mantener el catálogo académico: programas, asignaturas, RA, profesores, estudiantes y semestres (RF-01 a RF-03, RF-06 a RF-09).
- Registrar evaluaciones, calificaciones y observaciones (RF-10 a RF-13).
- Calcular el nivel de logro y las estadísticas descriptivas con **lógica de agregación convencional, sin IA** (RF-14, RF-15).
- Generar conclusiones automáticas y gestionar acciones de mejora (RF-16 a RF-18).
- Conservar un historial de auditoría *append-only* (RF-19, RNF-16).

Queda fuera del alcance del MVP: uso de IA para el análisis, carga de rúbricas en PDF, pasarelas de pago e integraciones con sistemas académicos externos.

---

## 3. Funcionalidad por integrante

### Sprint 1 — Catálogo y seguridad (30/09/2026 – 05/10/2026)

| Integrante | Funcionalidad | Requisitos | Módulo |
| --- | --- | --- | --- |
| José Alejandro Valencia | Inicio de sesión: clase base `Usuario`, catálogo `TipoDocumento`, bcrypt factor 12, JWT HS256 de 60 min, bloqueo de 15 min tras 5 intentos fallidos | RF-04, RNF-10, RNF-17 | `auth` |
| José Alejandro Valencia | Control de acceso por roles: filtro JWT, roles ADMINISTRADOR/PROFESOR/ESTUDIANTE, 401 sin token y 403 sin permiso | RF-05, RNF-13 | `security` |
| Juan Camilo Bernal | Gestión de asignaturas: ciclo Borrador → Activa → Inactiva, código único, activación solo con 5 a 7 RA activos; al inactivar una asignatura se inactivan en cascada sus RA activos | RF-03a–d | `asignaturas` |
| Jean Paul Ortiz | Gestión de profesores: cédula de 6 a 10 dígitos, correo `@uco.net.co`, cédula no modificable, inactivación lógica | RF-01a–d | `profesores` |
| Juan José Narváez | Gestión de Resultados de Aprendizaje: código único por asignatura, tope de 7 RA, mínimo de 5 activos | RF-06a–c | `resultadosaprendizaje` |
| Simón Tabares | Gestión de programas académicos: CRUD con nombre y código únicos y eliminación lógica | RF-02a–d | `programas` |
| Santiago Torres | Gestión de semestres: código único, validación de fechas y rechazo de periodos solapados | RF-Semestre a/b | `semestres` |
| Andrés Vélez | Gestión de estudiantes: `Estudiante` extiende `Usuario`, documento y correo únicos | RF-09a | `estudiantes` |

### Sprint 2 — Operación académica (06/10/2026 – 12/10/2026)

| Integrante | Funcionalidad | Requisitos | Módulo |
| --- | --- | --- | --- |
| Jean Paul Ortiz | Asignación docente de asignaturas: asignación masiva, omisión informada de duplicados, servicio «asignaturas del profesor» | RF-07 | `profesores` |
| Andrés Vélez | Matrícula y desvinculación: unicidad (estudiante, asignatura, semestre), repitencia, conservación de calificaciones al desvincular | RF-08, RF-09 | `estudiantes` |
| Juan José Narváez | Gestión de evaluaciones: suma de porcentajes ≤ 100 por (RA, semestre), bloqueo de fecha y % si ya hay notas | RF-10, RF-11 | `evaluaciones` |
| Simón Tabares | Calificaciones y trazabilidad: nota 0.0–5.0, observación mínima de 10 caracteres, marca `fueraDeFecha` | RF-12, RF-13 | `calificaciones` |
| Santiago Torres | Nivel de logro y estadísticas: catálogo de 4 rangos, recálculo automático, umbral de 3 calificaciones | RF-14, RF-15 | `estadisticas` |
| Juan Camilo Bernal | Conclusiones automáticas: alerta cuando más del 40 % queda en Bajo o Medio, sin duplicar conclusiones vigentes | RF-16 | `estadisticas` |
| José Alejandro Valencia | Historial de auditoría: servicio `registrarEvento`, tabla sin UPDATE ni DELETE, reporte paginado para ADMINISTRADOR | RF-19, RF-21c | `auditoria` |

### Sprint 3 — Cierre del ciclo y reportes (13/10/2026 – 19/10/2026)

| Integrante | Funcionalidad | Requisitos | Módulo |
| --- | --- | --- | --- |
| Juan Camilo Bernal | Acciones de mejora y conversión a evaluación: registro solo con conclusión vigente, validación de % al convertir | RF-17, RF-18 | `mejoras` |
| Santiago Torres | Consultas y reportes por rol: vista del profesor, RA por asignatura, logro por programa, vista del estudiante | RF-20, RF-21a/b, RF-22 | `reportes` |

### Responsabilidades transversales

| Integrante | Responsabilidad |
| --- | --- |
| Jean Paul Ortiz | Setup del repositorio, entornos y pipeline CI |
| Juan José Narváez | Pruebas unitarias y de integración (cobertura ≥ 70 % con JaCoCo en autenticación, RBAC, nivel de logro y conclusiones) |
| Juan José Narváez y José Alejandro Valencia | Pruebas no funcionales (carga, tiempos de respuesta, seguridad) |
| Jean Paul Ortiz y José Alejandro Valencia | Despliegue final |
| Simón Tabares | Aceptación alfa y beta con la docente |
| Juan Camilo Bernal | Cierre y entrega final |

> Cada desarrollador documenta sus propios endpoints en Swagger; la consolidación es una tarea aparte del plan.

---

## 4. Empaquetado

### Artefacto

El proyecto se empaqueta con **Gradle** como un JAR ejecutable de Spring Boot (*fat jar*), que incluye el servidor embebido:

```bash
cd SIGRA
./gradlew clean bootJar
# → SIGRA/build/libs/SIGRA-0.0.1-SNAPSHOT.jar
```

### Estructura de paquetes

El código se organiza por **módulo de negocio** y, dentro de cada uno, por capa. Todos los módulos siguen exactamente la misma forma, de modo que cada integrante trabaja en su carpeta sin colisionar con los demás:

```
SIGRA/
├── build.gradle                  Dependencias y configuración de compilación
├── settings.gradle
├── gradlew / gradlew.bat         Gradle Wrapper
└── src/
    ├── main/
    │   ├── java/co/edu/uco/sigra/
    │   │   ├── SigraApplication.java      Punto de entrada
    │   │   ├── asignaturas/               RF-03
    │   │   ├── auditoria/                 RF-19, RF-21c
    │   │   ├── auth/                      RF-04
    │   │   ├── calificaciones/            RF-12, RF-13
    │   │   ├── estadisticas/              RF-14, RF-15, RF-16
    │   │   ├── estudiantes/               RF-08, RF-09
    │   │   ├── evaluaciones/              RF-10, RF-11
    │   │   ├── mejoras/                   RF-17, RF-18
    │   │   ├── profesores/                RF-01, RF-07
    │   │   ├── programas/                 RF-02
    │   │   ├── reportes/                  RF-20, RF-21a/b, RF-22
    │   │   ├── resultadosaprendizaje/     RF-06
    │   │   ├── semestres/                 RF-Semestre a/b
    │   │   ├── common/                    DTO, excepciones y utilidades compartidas
    │   │   ├── shared/                    Entidades y enums transversales (TipoDocumento, EstadoRegistro)
    │   │   ├── config/                    Configuración transversal
    │   │   └── security/                  RF-05: config y filtros JWT
    │   └── resources/
    │       └── application.yaml
    └── test/java/                Pruebas unitarias y de integración
```

Capas dentro de cada módulo:

| Carpeta | Contenido |
| --- | --- |
| `controller` | Endpoints REST; recibe y valida la petición |
| `dto` | Objetos de entrada y salida de la API |
| `entity` | Entidades JPA que mapean las tablas |
| `exception` | Excepciones propias del módulo |
| `mapper` | Conversión entre entidad y DTO |
| `repository` | Acceso a datos con Spring Data JPA |
| `service` / `service/impl` | Interfaz y reglas de negocio |

### Convención

Ninguna clase de `controller` accede directamente a `repository`: la ruta siempre es **controller → service → repository**. Las validaciones de negocio viven en `service/impl`, no en el controlador.

Los manejadores de excepciones de cada módulo atienden solo errores de negocio de ese módulo y NO incluyen un método para `Exception.class`; los errores comunes (validación, JSON ilegible, 403 por permisos, 500 genérico sin detalles técnicos) los atiende el manejador global en `common/exception`.

---

## 5. Arquitectura

### Vista general

```
┌──────────────────────┐        HTTPS / JSON        ┌──────────────────────────┐
│  SIGRA Frontend      │ ─────────────────────────▶ │  SIGRA Backend           │
│  Angular (SPA)       │ ◀───────────────────────── │  Spring Boot (API REST)  │
│  Admin / Profesor /  │      JWT en Authorization  │                          │
│  Estudiante          │                            └───────────┬──────────────┘
└──────────────────────┘                                        │ JDBC
                                                    ┌───────────▼──────────────┐
                                                    │  PostgreSQL              │
                                                    └──────────────────────────┘
```

SIGRA es un **sistema independiente**: no se integra con SIS, LMS ni pasarelas de pago.

### Estilo arquitectónico

- **Monolito modular por dominio.** Un solo despliegue, dividido internamente en módulos de negocio con fronteras claras. Dado el tamaño del proyecto (4 meses y ~20 usuarios), los microservicios serían sobrediseño.
- **Arquitectura en capas** dentro de cada módulo: controller → service → repository → entity.
- **API REST sin estado.** Cada petición lleva su JWT; el servidor no guarda sesión.

### Capas

| Capa | Responsabilidad | Tecnología |
| --- | --- | --- |
| Presentación | Endpoints REST, códigos HTTP, validación de entrada | Spring Web MVC, Jakarta Validation |
| Seguridad | Autenticación, filtro JWT, autorización por rol | Spring Security |
| Negocio | Reglas del dominio: rango de 5–7 RA, suma de % ≤ 100, nivel de logro, umbral del 40 % | Servicios Spring |
| Persistencia | Repositorios y mapeo objeto-relacional | Spring Data JPA / Hibernate |
| Datos | Almacenamiento con eliminación lógica y auditoría append-only | PostgreSQL |

### Decisiones transversales

- **Seguridad en dos niveles.** Ocultar una opción en la interfaz nunca basta: cada endpoint valida el rol en el backend según la Matriz RBAC (RF-05, RNF-13). Sin token o con token vencido → `401`; con rol insuficiente → `403`.
- **Nada se borra físicamente.** Programas, asignaturas, RA, profesores y matrículas se inactivan (estado `ACTIVO` / `INACTIVO`). Las calificaciones se conservan aunque el estudiante se desvincule (RF-19).
- **Auditoría append-only.** La tabla de historial no admite `UPDATE` ni `DELETE`, ni siquiera para el administrador; la restricción se impone a nivel de base de datos. Retención mínima: 5 años (RNF-16).
- **Análisis sin IA.** Estadísticas y conclusiones se calculan con agregaciones y reglas de negocio explícitas.
- **Errores sin detalles técnicos.** Los mensajes al usuario no exponen trazas, rutas internas ni consultas SQL (RNF-17).

### Stack

| Componente | Tecnología | Versión |
| --- | --- | --- |
| Lenguaje | Java | 21 (toolchain) |
| Framework | Spring Boot | 4.1.1 |
| Build | Gradle (Wrapper) | — |
| Base de datos | PostgreSQL | 15 o superior |
| ORM | Spring Data JPA / Hibernate | — |
| Seguridad | Spring Security + JWT (HS256) | — |
| Bloqueo de intentos fallidos | Persistido en PostgreSQL (`usuario.intentos_fallidos`, `usuario.fecha_bloqueo`). Redis no se usa | — |
| Validación | Spring Boot Starter Validation | — |
| Monitoreo | Spring Boot Actuator (`/actuator/health`) | — |
| Reducción de boilerplate | Lombok | — |
| Mapeo entidad ↔ DTO | MapStruct | 1.6.3 |
| Pruebas | JUnit 5 + Mockito + MockMvc | — |
| Documentación de API | OpenAPI / Swagger (springdoc) | 3.1.1 |

---

## 6. Cómo ejecutar el proyecto

### Requisitos previos

| Herramienta | Versión mínima | Verificación |
| --- | --- | --- |
| JDK | 21 | `java -version` |
| PostgreSQL | 15 | `psql --version` |
| Redis | 7 — Opcional por ahora (deshabilitado temporalmente) | `redis-cli ping` |
| Git | 2.30 | `git --version` |

No hace falta instalar Gradle: el repositorio incluye el Wrapper (`gradlew`).

### 1. Clonar el repositorio

```bash
git clone https://github.com/juancabernal/software4-SIGRA-backend.git
cd software4-SIGRA-backend/SIGRA
```

> Todos los comandos siguientes se ejecutan desde la carpeta `SIGRA/`, que es la raíz del proyecto Gradle.

### 2. Crear el esquema de la base de datos

SIGRA usa la base `postgres` y el **schema** `sigra` dentro de ella. `sigra` **no es una base de datos**: por eso la URL JDBC lleva `?currentSchema=sigra`.

Para una base vacía, ejecuta el baseline (crea el schema y todas sus tablas en una sola transacción):

```bash
psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/SIGRA_SCHEMA_CURRENT.sql
```

Hibernate **no crea ni modifica tablas**: con `ddl-auto=validate` solo comprueba que las entidades coincidan con el esquema. Si falta una tabla o una columna, el backend no arranca. El detalle del modelo (herencia `Usuario` → `Profesor`, correo institucional, deudas) está en [docs/db/SIGRA_DATABASE_CURRENT.md](docs/db/SIGRA_DATABASE_CURRENT.md).

### 3. Configurar las variables de entorno

Nunca subas credenciales al repositorio. La forma recomendada es un archivo `.env` dentro de `SIGRA/`. Spring Boot lo carga de forma nativa (`spring.config.import`) y Git lo ignora (`*.env`):

```bash
cp .env.example .env
```

En PowerShell: `Copy-Item .env.example .env`. Después completa los valores. Las variables reales del sistema (por ejemplo `$env:DB_URL`) tienen prioridad sobre el archivo.

| Variable | Descripción | Valor por defecto | Ejemplo |
| --- | --- | --- | --- |
| `DB_URL` | URL JDBC: base `postgres` y schema `sigra` (`currentSchema`). El puerto depende de tu máquina. Sin valor por defecto | — | `jdbc:postgresql://localhost:5432/postgres?currentSchema=sigra` |
| `DB_USERNAME` | Usuario de la base de datos. Sin valor por defecto | — | `postgres` |
| `DB_PASSWORD` | Contraseña de la base de datos. Sin valor por defecto | — | — |
| `SERVER_PORT` | Puerto HTTP del backend | `8080` | `8080` |
| `JWT_SECRET` | Clave de firma HS256 de al menos 32 bytes. Sin valor por defecto | — | — |
| `JWT_EXPIRATION_MS` | Vigencia del token en milisegundos (debe ser mayor que cero) | `3600000` (60 min) | `3600000` |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend, separados por coma | `http://localhost:4200` | `http://localhost:4200` |
| `JPA_DDL_AUTO` | Estrategia de Hibernate. `validate` solo comprueba el esquema; Hibernate nunca lo modifica | `validate` | `validate` |
| `JPA_SHOW_SQL` | Mostrar las consultas SQL en consola | `false` | `false` |

> **Puerto de PostgreSQL:** cada desarrollador usa el puerto de su propio servidor. Por ejemplo, en la máquina de referencia de este proyecto el servidor escucha en `localhost:5433`, así que allí `DB_URL` es `jdbc:postgresql://localhost:5433/postgres?currentSchema=sigra`. El puerto **no está fijado en el código**: se configura en `SIGRA/.env` o en las variables del sistema. El `.env.example` compartido muestra `5432`, que es el valor por defecto de PostgreSQL.

En Linux o macOS:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/postgres?currentSchema=sigra"
export DB_USERNAME=postgres
export DB_PASSWORD=cambiar_esta_clave
export SERVER_PORT=8080
export JWT_SECRET=una_clave_larga_y_aleatoria_de_al_menos_32_caracteres
export JWT_EXPIRATION_MS=3600000
```

En Windows (PowerShell):

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/postgres?currentSchema=sigra"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="cambiar_esta_clave"
$env:SERVER_PORT="8080"
$env:JWT_SECRET="una_clave_larga_y_aleatoria_de_al_menos_32_caracteres"
$env:JWT_EXPIRATION_MS="3600000"
```

### 4. Ejecutar en modo desarrollo

Linux / macOS:

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

La API queda disponible en **http://localhost:8080**.

| Recurso | URL |
| --- | --- |
| Estado del servicio | http://localhost:8080/actuator/health |
| Documentación Swagger (UI) | http://localhost:8080/swagger-ui.html |
| Contrato OpenAPI (JSON) | http://localhost:8080/v3/api-docs |

#### Autenticación (RF-04)

`POST /api/v1/auth/login` es público: no requiere token ni cabecera CSRF.

```json
{
  "correoInstitucional": "profesor.bruno@uco.net.co",
  "contrasena": "PasswordSeguro123*"
}
```

| Código | Cuándo |
| --- | --- |
| 200 | Credenciales válidas. Devuelve `token` (JWT HS256 con los claims `sub`, `correo`, `rol`, `iat`, `exp`), `tipo` (`Bearer`), `expiraEn` (segundos) y `usuario` |
| 400 | Datos inválidos: correo vacío o de otro dominio que no sea `@uco.net.co`, o contraseña vacía |
| 401 | Correo o contraseña incorrectos, o usuario inactivo. Es el mismo mensaje en todos los casos |
| 423 | Usuario bloqueado durante 15 minutos tras 5 intentos fallidos |

El correo se acepta sin distinguir mayúsculas y con espacios laterales; el servidor lo normaliza antes de buscarlo. El token vence a los 60 minutos.

### 5. Ejecutar las pruebas

```bash
./gradlew test                            # Pruebas unitarias y de capa web (sin PostgreSQL)
./gradlew integrationTest                 # Pruebas de integración (requiere PostgreSQL y SIGRA/.env)
./gradlew test --tests '*auth*'           # Solo las pruebas de un módulo (ejemplo)
./gradlew test jacocoTestReport           # Pruebas + reporte de cobertura (disponible cuando se agregue el plugin de JaCoCo)
```

Las pruebas se separan por etiqueta para saber siempre qué se ejecutó:

| Tarea | Qué incluye | Requiere PostgreSQL |
| --- | --- | --- |
| `test` | Unitarias y de capa web: servicios, entidades, JWT, BCrypt, controladores con MockMvc y la cadena de seguridad real (`SecurityConfig`) | No |
| `integrationTest` | Pruebas con `@Tag("integration")`: `SigraContextIT` levanta el contexto completo | Sí, con `.env` válido |

Un fallo de `integrationTest` por credenciales o conexión no invalida el resultado de `test`.

El reporte de cobertura quedará en `build/reports/jacoco/test/html/index.html` cuando se agregue el plugin de JaCoCo.

#### Pruebas de la API con Bruno

Además de las pruebas unitarias, el repositorio incluye una colección de pruebas de la API para [Bruno](https://www.usebruno.com) en `bruno/SIGRA/`, con datos de prueba reproducibles. Cómo cargar los datos y ejecutarla: [bruno/README.md](bruno/README.md).

### 6. Compilar y ejecutar el JAR

```bash
./gradlew clean bootJar
java -jar build/libs/SIGRA-0.0.1-SNAPSHOT.jar
```

Para apuntar a otro perfil o puerto:

```bash
java -jar build/libs/SIGRA-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod --server.port=9090
```

### 7. Conectar con el frontend

Levanta el backend en el puerto 8080 y luego el [frontend Angular](https://github.com/juancabernal/software4-SIGRA-frontend) en el 4200. El frontend apunta al backend mediante la variable `apiUrl` de sus archivos de entorno.

### Problemas frecuentes

| Síntoma | Causa probable | Solución |
| --- | --- | --- |
| `Connection refused` a PostgreSQL | El servicio no está arriba o el puerto es otro | Verifica el servicio y el valor de `DB_URL` |
| `Permission denied: ./gradlew` | El script perdió el permiso de ejecución | `chmod +x gradlew` |
| `UnsupportedClassVersionError` | JDK anterior a 21 | Instala JDK 21 y ajusta `JAVA_HOME` |
| `Could not resolve all dependencies` | Primera ejecución sin red o con proxy | Verifica la conexión y vuelve a ejecutar |
| El token se rechaza siempre | `JWT_SECRET` ausente o demasiado corto | Define una clave de 32 caracteres o más |
| `Could not resolve placeholder 'DB_URL'` (o `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`) | No existe `SIGRA/.env` ni la variable está definida en el sistema. Estas variables no tienen valor por defecto | Crea `SIGRA/.env` a partir de `.env.example` y completa los valores |
| `Schema-validation: missing table [...]` o `missing column [...]` al arrancar | La base no tiene el esquema que esperan las entidades (`ddl-auto=validate`) | Ejecuta `docs/db/SIGRA_SCHEMA_CURRENT.sql` en una base vacía, o la migración correspondiente en una base existente. Ver [SIGRA_DATABASE_CURRENT.md](docs/db/SIGRA_DATABASE_CURRENT.md) |
| `relation "usuario" does not exist` o `schema "sigra" does not exist` | `DB_URL` no apunta a `currentSchema=sigra` o el schema no fue creado | Revisa `DB_URL` y ejecuta el baseline |
| `Connection refused` en `localhost:5433` (o `5432`) | El puerto de `DB_URL` no coincide con el de tu PostgreSQL | Corrige el puerto en `DB_URL` dentro de `SIGRA/.env` |
| PowerShell no reconoce `gradlew` | En PowerShell hay que indicar la ruta del script | Ejecuta con `.\gradlew.bat` (con `.\`) |


---

## RF-04 — Inicio de sesión

**Estado: COMPLETADO (backend).** Listo para desarrollo del frontend. RF-05 (autorización por roles) no está iniciado.

| Tecnología | Uso en RF-04 |
| --- | --- |
| Spring Security | Cadena de filtros sin estado (`STATELESS`), CSRF deshabilitado para la API REST |
| BCrypt, factor 12 | Hash de contraseñas (`PasswordEncoder`). Nunca se expone el hash |
| JWT HS256 | Token de sesión firmado con `JWT_SECRET` |
| Spring Data JPA / PostgreSQL | Persistencia de `Usuario` (herencia JOINED con `Profesor`) y del contador de intentos |
| Swagger / OpenAPI | Documentación de la API |
| Bruno | Colección de pruebas de la API (`bruno/SIGRA/auth/`) |

**Endpoint:** `POST /api/v1/auth/login` (público).

| Regla | Valor |
| --- | --- |
| Vigencia del JWT | 60 minutos (`JWT_EXPIRATION_MS`, por defecto `3600000`) |
| Intentos fallidos antes de bloquear | 5 consecutivos |
| Duración del bloqueo | 15 minutos. Durante el bloqueo se responde 423 sin evaluar la contraseña |
| Persistencia del contador | Los fallos se guardan aunque la respuesta sea un error (`noRollbackFor`) |
| Mensajes de error | Amigables, sin trazas, SQL ni nombres internos de clases (RNF-17) |

Códigos de respuesta: 200 (login correcto), 400 (validación), 401 (credenciales), 423 (bloqueo temporal).

**Documentación en ejecución** (con el backend en `localhost:8080`):

- Swagger UI: http://localhost:8080/swagger-ui.html
- Contrato OpenAPI: http://localhost:8080/v3/api-docs
- Estado del servicio: http://localhost:8080/actuator/health

**Variables necesarias:** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET` (sin valor por defecto). `JWT_EXPIRATION_MS`, `JPA_DDL_AUTO` y `JPA_SHOW_SQL` tienen valor por defecto. Ver la tabla de la sección 6.

**Notas:**

- Al arrancar, Spring puede mostrar `Using generated security password`. Es un aviso inocuo: SIGRA no usa `UserDetailsService`; la autenticación real es `AuthService` + `PasswordEncoder` + JWT.
- Mientras RF-05 no esté implementado, `SecurityConfig` deja pasar el resto de rutas (`anyRequest().permitAll()`). No hay filtro JWT ni `@PreAuthorize` activos.

**Deudas que no bloquean RF-04** (ver «Pendientes conocidos» más abajo): `FAILED_ATTEMPTS_CONCURRENCY` (DEFERRED), y `PASSWORD_PROVISIONING_FLOW` (decidido, pendiente de implementar). `AUTH_RUNTIME_ROLES` quedó resuelta: `Profesor`, `Administrador` y `Estudiante` extienden `Usuario` (RF-04).

---

## Pendientes conocidos del repositorio

Estado de RF-04 (autenticación) y lo que sigue abierto:

1. **Cédula modificable: contradicción documental.** El diagrama de clases indica que `(tipoDocumento, numeroDocumento)` es una identidad no modificable, pero el criterio de aceptación de RF-01c valida la cédula al modificarla. Mientras el equipo no lo unifique, `modificarProfesor` conserva el comportamiento actual: el número de documento no cambia y el tipo de documento sí.
2. **Concurrencia de intentos fallidos (`FAILED_ATTEMPTS_CONCURRENCY: DEFERRED`).** Dos inicios de sesión incorrectos simultáneos pueden perder un incremento del contador. Es deuda técnica de seguridad: requiere bloqueo pesimista o versionado, probado contra PostgreSQL con herencia JOINED.
3. **Aprovisionamiento de contraseñas (`PASSWORD_PROVISIONING_FLOW`).** `POST /api/v1/profesores` y `POST /api/v1/estudiantes` no reciben contraseña, así que un profesor o un estudiante registrado por la API no puede iniciar sesión todavía (lo mismo aplica a los estudiantes migrados desde el diseño anterior). Decisión: el administrador asigna la contraseña mediante un endpoint propio, `PUT /api/v1/usuarios/{id}/contrasena` (RNF-10: nunca se devuelve ni se registra); pendiente de implementar. Para desarrollo se usan los seeds de Bruno.
4. **Roles en tiempo de ejecución (resuelto).** `Profesor`, `Administrador` y `Estudiante` extienden `Usuario` (RF-04), así que los tres pueden autenticarse si tienen contraseña (ver el punto 3).
5. **Alineación de esquema.** Si una base tiene `profesor_asignatura` (modelo antiguo) en lugar de `asignacion_docente`, el preflight lo marca como `ALINEACION_ASIGNACIONES_REQUERIDA`. No se renombra ninguna tabla sin revisión del módulo de asignaciones. La base de referencia no lo tiene. Ver `docs/db/RF04_schema_preflight.sql`.
6. **Unicidad de (tipoDocumento, numeroDocumento).** El UNIQUE compuesto del diagrama no puede declararse solo en `profesor`, porque `tipo_documento_id` vive en `usuario`. Se verifica en la migración y en el servicio, pero no como restricción de base de datos.
7. **JaCoCo.** El plugin de cobertura todavía no está declarado, aunque el plan de pruebas exige un 70 % mínimo.
8. **Redis.** Está desactivado temporalmente: comentado en `build.gradle` y `docker-compose.yml`.
9. **Seguridad abierta (RF-05).** `SecurityConfig` permite todas las peticiones salvo el login, y el filtro JWT no está activado. Tampoco hay `@PreAuthorize` activos hasta implementar RF-05.

---

## Documentación del proyecto

| Documento | Ubicación |
| --- | --- |
| Especificación de Requisitos de Software (IEEE 830) | Documento del equipo, versión 2.0 corregida |
| Entendimiento del problema y requisitos funcionales | `docs/requisitos_funcionales/` |
| Matriz RBAC | Apéndice 4.1 del SRS |
| Matriz de compatibilidad de pruebas | Apéndice 4.2 del SRS |
| Plan de pruebas | TestLink |

---

## Flujo de trabajo con Git

Cada integrante trabaja en su propia rama (por ejemplo `bernal` o `jean`) o en una rama `feature/RF-XX-nombre-corto`:

```bash
git checkout bernal
git pull origin develop
# ... desarrollo ...
git commit -m "feat(asignaturas): servicio de registro, modificación y activación/inactivación"
git push origin bernal
```

- **Toda integración se hace por Pull Request hacia `develop`**, revisado por al menos otro integrante.
- Nunca se abre un Pull Request de una rama personal hacia `main`.
- `develop` debe mantenerse siempre estable y compilando.
- `main` se actualiza solo desde `develop`, en las entregas.

### Convención de commits

Se usa [Conventional Commits](https://www.conventionalcommits.org/es/): `tipo(ámbito): descripción`, con los tipos `feat`, `fix`, `test`, `docs`, `chore`, `ci` y `refactor`. Ejemplos reales del repositorio:

```
feat(asignaturas): servicio de registro, modificación y activación/inactivación
fix(asignaturas): al inactivar una asignatura se inactivan en cascada sus RA activos
test(semestres): agregar pruebas unitarias de servicio y controlador
```

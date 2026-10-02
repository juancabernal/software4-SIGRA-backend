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
| José Alejandro Valencia | Control de acceso por roles: filtro JWT, roles ADMIN/PROFESOR/ESTUDIANTE, 401 sin token y 403 sin permiso | RF-05, RNF-13 | `security` |
| Juan Camilo Bernal | Gestión de asignaturas: ciclo Borrador → Activa → Inactiva, código único, activación solo con 5 a 7 RA activos | RF-03a–d | `asignaturas` |
| Jean Paul Ortiz | Gestión de profesores: cédula de 6 a 10 dígitos, correo `@uco.net.co`, cédula no modificable, inactivación lógica | RF-01a–d | `profesores` |
| Juan José Narváez | Gestión de Resultados de Aprendizaje: código único por asignatura, tope de 7 RA, mínimo de 5 activos | RF-06a–c | `resultadosaprendizaje` |
| Simón Tabares | Gestión de programas académicos: CRUD con nombre y código únicos y eliminación lógica | RF-02a–d | `programas` |
| Santiago Torres | Gestión de semestres: código único, validación de fechas y rechazo de periodos solapados | RF-Semestre a/b | `asignaturas` (submódulo semestres) |
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
| José Alejandro Valencia | Historial de auditoría: servicio `registrarEvento`, tabla sin UPDATE ni DELETE, reporte paginado para ADMIN | RF-19, RF-21c | `auditoria` |

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
    │   │   ├── asignaturas/               RF-03 y semestres
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
    │   │   ├── common/                    DTO, excepciones y utilidades compartidas
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
| Caché / bloqueo de intentos | Spring Data Redis | — |
| Validación | Spring Boot Starter Validation | — |
| Monitoreo | Spring Boot Actuator (`/actuator/health`) | — |
| Reducción de boilerplate | Lombok | — |
| Pruebas | JUnit 5 + Spring Boot Test | — |
| Documentación de API | OpenAPI / Swagger | — |

---

## 6. Cómo ejecutar el proyecto

### Requisitos previos

| Herramienta | Versión mínima | Verificación |
| --- | --- | --- |
| JDK | 21 | `java -version` |
| PostgreSQL | 15 | `psql --version` |
| Redis | 7 | `redis-cli ping` |
| Git | 2.30 | `git --version` |

No hace falta instalar Gradle: el repositorio incluye el Wrapper (`gradlew`).

### 1. Clonar el repositorio

```bash
git clone https://github.com/juancabernal/software4-SIGRA-backend.git
cd software4-SIGRA-backend/SIGRA
```

> Todos los comandos siguientes se ejecutan desde la carpeta `SIGRA/`, que es la raíz del proyecto Gradle.

### 2. Crear la base de datos

```sql
CREATE DATABASE sigra;
CREATE USER sigra_user WITH ENCRYPTED PASSWORD 'cambiar_esta_clave';
GRANT ALL PRIVILEGES ON DATABASE sigra TO sigra_user;
```

### 3. Configurar las variables de entorno

Nunca subas credenciales al repositorio. Define estas variables en tu entorno o en un archivo `.env` local (ya ignorado por Git):

| Variable | Descripción | Ejemplo |
| --- | --- | --- |
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/sigra` |
| `DB_USERNAME` | Usuario de la base de datos | `sigra_user` |
| `DB_PASSWORD` | Contraseña de la base de datos | — |
| `REDIS_HOST` | Host de Redis | `localhost` |
| `REDIS_PORT` | Puerto de Redis | `6379` |
| `JWT_SECRET` | Clave de firma HS256 (mínimo 32 caracteres) | — |
| `JWT_EXPIRATION` | Vigencia del token en milisegundos | `3600000` |

En Linux o macOS:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/sigra
export DB_USERNAME=sigra_user
export DB_PASSWORD=cambiar_esta_clave
export JWT_SECRET=una_clave_larga_y_aleatoria_de_al_menos_32_caracteres
```

En Windows (PowerShell):

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/sigra"
$env:DB_USERNAME="sigra_user"
$env:DB_PASSWORD="cambiar_esta_clave"
$env:JWT_SECRET="una_clave_larga_y_aleatoria_de_al_menos_32_caracteres"
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
| Documentación Swagger | http://localhost:8080/swagger-ui.html |

### 5. Ejecutar las pruebas

```bash
./gradlew test                  # Pruebas unitarias y de integración
./gradlew test jacocoTestReport # Pruebas + reporte de cobertura
```

El reporte de cobertura queda en `build/reports/jacoco/test/html/index.html`.

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

---

## Pendientes conocidos del repositorio

Al momento de escribir este README, el repositorio está en fase de esqueleto. Quedan por resolver:

1. **Paquete de `SigraApplication.java`.** La clase declara `package com.sigra.SIGRA;` pero el archivo vive en `src/main/java/co/edu/uco/sigra/`. Spring Boot no arrancará hasta que el paquete declarado coincida con la ruta; además, el escaneo de componentes debe partir de `co.edu.uco.sigra` para que encuentre los módulos. Lo mismo aplica a la clase de prueba, que está bajo `com/sigra/SIGRA/`.
2. **Gradle Wrapper incompleto.** Faltan `gradle/wrapper/gradle-wrapper.jar` y `gradle-wrapper.properties`. Sin ellos `./gradlew` falla; regenéralos con `gradle wrapper` y haz commit de la carpeta `gradle/`.
3. **Redis.** Redis se ejecuta como servicio externo al backend. Desde `software4-SIGRA-backend/` puede iniciarse con `docker compose up -d redis`; el backend usa `REDIS_HOST` y `REDIS_PORT`, con valores predeterminados `localhost` y `6379`.
4. **Dependencia de JWT.** `build.gradle` aún no incluye una librería JWT (por ejemplo `io.jsonwebtoken:jjwt`) ni el starter de OpenAPI para Swagger.
5. **JaCoCo.** El plugin de cobertura todavía no está declarado, aunque el plan de pruebas exige un 70 % mínimo.
6. **Inconsistencia documental pendiente.** El diagrama de clases dice que la cédula del profesor no se puede modificar una vez registrada, pero el criterio de aceptación de RF-01c valida la cédula al modificarla. Debe unificarse antes de implementar el módulo de profesores.

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

```bash
git checkout -b feature/RF-XX-nombre-corto
# ... desarrollo ...
git commit -m "RF-XX: descripción del cambio"
git push origin feature/RF-XX-nombre-corto
```

Toda rama se integra a `main` mediante Pull Request revisado por al menos otro integrante. `main` debe permanecer siempre estable y desplegable.

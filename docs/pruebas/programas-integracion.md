# Pruebas de integración — módulo Programas (RF-02)

Clase: `SIGRA/src/test/java/co/edu/uco/sigra/programas/integracion/ProgramaIntegracionIT.java`

Levanta el contexto completo de Spring Boot contra **PostgreSQL real** y llama a la API con `MockMvc`. Cada caso verifica la **respuesta HTTP** (estado y cuerpo sin detalles técnicos) y la **fila real** en `programa_academico` con `ProgramaAcademicoRepository`. La prueba **no** usa `@Transactional`: el servicio confirma de verdad, así se comprueba lo que realmente queda en la base.

**Datos de referencia.** Todos los programas de estas pruebas usan el prefijo reservado `ITG-` en el código y `ITG Programa ` en el nombre (el mismo que usa `AsignaturaIntegracionIT` para sus programas de apoyo). `@BeforeEach` y `@AfterEach` borran solo las filas con ese prefijo, en `asignatura` y luego en `programa_academico` (en ese orden, por la clave foránea).

**Seguridad.** A diferencia de `ProfesorIntegracionIT` y `AsignacionDocenteIntegracionIT`, esta clase corre con `@AutoConfigureMockMvc(addFilters = false)` y `@WithMockUser(roles = "ADMINISTRADOR")`: no ejercita la cadena real de `JwtAuthenticationFilter` ni `@PreAuthorize` contra un token real. La autenticación/autorización real de los cinco endpoints (401/403) no está cubierta todavía por una prueba de integración de este módulo; queda pendiente nivelarla al patrón de Profesores/Asignación docente.

| ID | Capacidad | Escenario | Datos de entrada | Resultado esperado (respuesta) | Resultado esperado (base) |
| --- | --- | --- | --- | --- | --- |
| IT-01 | Registrar programa | Registrar y luego rechazar nombre y código duplicados (con mayúsculas distintas) | `POST` nombre `ITG Programa Uno`, código `ITG-001`; después `POST` con el mismo nombre en mayúsculas y otro código; después `POST` con otro nombre y el mismo código en minúsculas | 201 con estado `ACTIVO`; los dos duplicados dan 409 sin detalles técnicos | Una sola fila con nombre y código normalizados; ninguno de los duplicados crea una segunda fila |
| IT-02 | Registrar programa | Entradas inválidas (9 variantes parametrizadas) | Nombre vacío, nulo o de 101 caracteres; código vacío, nulo, de 3 u 11 caracteres, con espacio o con símbolo | 400 en cada una, sin detalles técnicos | El conteo de programas no cambia |
| IT-03 | Consultar programas | Filtrar por nombre, por código y por estado contra datos reales; consultar por id existente e inexistente | Tres programas (dos `ACTIVO`, uno `INACTIVO`); `GET` con `?nombre=`, con `?codigo=`, con `?estado=INACTIVO`, con nombre+estado combinados sin resultados; `GET /{id}` existente e inexistente | 200 con cada filtro devolviendo exactamente lo esperado; combinado sin resultados da lista vacía; 200 por id existente; 404 por id inexistente, sin detalles técnicos | Solo lectura: las pruebas no modifican filas durante las consultas |
| IT-04 | Modificar nombre | Renombrar sin tocar el código; rechazar nombre duplicado y nombre inválido; id inexistente | `PUT` con nombre nuevo; `PUT` reutilizando el nombre de otro programa; `PUT` con nombre vacío; `PUT` a un id inexistente | 200 con el nombre nuevo y el mismo código; 409 por el nombre duplicado; 400 por el nombre vacío; 404 por el id inexistente (todos los errores sin detalles técnicos) | Tras el 200 la fila tiene el nombre nuevo y el código intacto; tras el 409 y el 400 la fila no cambia |
| IT-05 | Inactivar programa | Pasar a `INACTIVO`, comprobar que es idempotente, rechazar si tiene una asignatura `ACTIVA` y responder 404 para un id inexistente | `PATCH /{id}/inactivar` sobre un programa sin asignaturas; repetir sobre el mismo; `PATCH` sobre un programa con una asignatura `ACTIVA` creada por repositorio; `PATCH` a un id inexistente | 200 con estado `INACTIVO`; el segundo `PATCH` repite 200 sin error; 409 cuando hay una asignatura activa, sin detalles técnicos; 404 para el id inexistente | El primer programa queda `INACTIVO`; el que tiene la asignatura activa sigue `ACTIVO`; ninguna fila se borra |

## Seguridad y validación de capa web

`ProgramaAcademicoControllerTest` (`@WebMvcTest` en modo *standalone*, sin filtros de seguridad ni manejador de módulo) cubre el listado, la lista vacía, el filtro de estado y la traducción de un estado inválido a 400 por el manejador global. `ProgramaAcademicoCrudControllerTest` (mismo modo, con `ProgramaExceptionHandler` y `GlobalExceptionHandler` reales) cubre el registro válido, las validaciones de nombre y código, los 409 por nombre y código duplicados, el 404 al consultar, la modificación de nombre y los dos resultados de inactivar (200 e 409 por asignaturas activas). `ProgramaServiceImplTest` y `ProgramaServiceEscrituraTest` cubren la lógica del servicio (filtros en memoria, duplicados, idempotencia de inactivar) con el repositorio simulado. `ProgramaApiDocumentacionIT` verifica contra `/v3/api-docs` que los cinco endpoints documenten `bearerAuth` y sus códigos de respuesta (ver [`swagger-programas.md`](../documentacion_openapi/swagger-programas.md)).

## Ejecución

Requieren PostgreSQL con el esquema de `docs/db` y un `SIGRA/.env` válido. No se ejecutan con `./gradlew test`.

```bash
cd SIGRA
./gradlew integrationTest --tests '*ProgramaIntegracionIT*'        # solo esta
./gradlew integrationTest --tests '*Programa*'                     # programas, incluida ProgramaApiDocumentacionIT
./gradlew integrationTest                                          # todas las de integración
```

## Garantía de limpieza

Todos los datos que crea la prueba llevan el prefijo **ITG**: código `ITG-...` y nombre `ITG Programa ...`. Un `@AfterEach` (que también corre al inicio de cada caso, por si una corrida anterior se interrumpió) borra **solo** lo que cumple ese prefijo, primero las asignaturas de apoyo (por la clave foránea hacia el programa) y luego los programas. Nunca toca datos ajenos ni los programas de otras colecciones (por ejemplo `BRU-ACT`/`BRU-INA` de la colección Bruno de asignaturas).

## Bruno

La carpeta `bruno/SIGRA/programas/` contiene la secuencia manual: `01-registrar` registra un programa con un código único (generado a partir de la hora, igual que en `estudiantes`), confirma el 409 por nombre y por código duplicados, y el 400 por nombre vacío y por código con formato inválido; `02-consultar` lista todos los programas, filtra por estado, rechaza un estado inválido y consulta por id existente e inexistente; `03-modificar` renombra el programa creado en `01-registrar`, confirma el 409 al reutilizar el nombre del programa semilla `Programa Bruno Activo`, el 400 por nombre vacío y el 404 por id inexistente; `04-inactivar` lo inactiva, repite la operación para confirmar que es idempotente, confirma el 409 al intentar inactivar el programa semilla `BRU-ACT` (tiene la asignatura `BRU10` en estado `ACTIVA`, así que el 409 nunca le cambia el estado) y el 404 por id inexistente. Requiere los IDs de datos de prueba ya definidos en `environments/local.bru` (`programaActivoId`, `idInexistente`) y la sesión de administrador (`tokenAdmin`) que inicia alguna de las colecciones con `00-sesion`, por ejemplo `asignaciones-docentes`.

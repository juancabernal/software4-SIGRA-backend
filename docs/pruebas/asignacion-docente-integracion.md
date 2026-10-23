# Pruebas de integración — Asignación docente (RF-07)

Clase: `SIGRA/src/test/java/co/edu/uco/sigra/asignaturas/integracion/AsignacionDocenteIntegracionIT.java`.

El módulo asigna en forma masiva el producto cartesiano de profesores y asignaturas. Una asignación
ACTIVA existente se omite; una INACTIVA se reactiva, por lo que el par `(profesor_id, asignatura_id)`
siempre conserva una sola fila. Las pruebas usan datos con prefijo `ITG` y los eliminan al finalizar.

**Seguridad.** Igual que `ProfesorIntegracionIT`, la clase corre con `@AutoConfigureMockMvc` **sin**
`addFilters = false` ni `@WithMockUser`: la cadena de filtros real (`JwtAuthenticationFilter`) y
`@PreAuthorize` se ejecutan en cada petición. `@BeforeEach` crea por repositorio un `Administrador`
real (`itg-asigdoc-admin-...`) y adjunta `Authorization: Bearer <token>` emitido por
`JwtService.generarToken`; IT-01 a IT-07 usan ese token. IT-08 cubre el 401 (sin token y con la firma
del token alterada) y el 403 con tokens reales de un `Profesor` y de un `Estudiante` en los dos
endpoints.

| ID | Escenario | Resultado HTTP | Verificación persistente |
| --- | --- | --- | --- |
| IT-01 | Dos profesores por dos asignaturas activas | `201`, cuatro elementos en `creadas` | Cuatro filas ACTIVAS con fecha de hoy |
| IT-02 | Repetir un par activo | `201`, un elemento en `omitidas` | Sigue existiendo una sola fila |
| IT-03 | Profesor INACTIVO o asignatura no ACTIVA | `201`, pares afectados en `rechazadas` | No se crean filas para esos pares |
| IT-04 | Profesor o asignatura inexistente | `404`, sin detalles técnicos | No se crea ninguna fila |
| IT-05 | Desasignar una fila, repetir y usar un ID inexistente | `204`, `204`, `404` | La fila queda INACTIVA y no se elimina |
| IT-06 | Reasignar un par previamente inactivo | `201`, un elemento en `creadas` | Reactiva la misma fila, sin duplicarla |
| IT-07 | Entradas inválidas (9 variantes parametrizadas, con BD y seguridad reales) | `400` en cada una, sin detalles técnicos | El conteo de asignaciones no cambia |
| IT-08a | Sin token o con token alterado, en los dos endpoints | `401` en las cuatro llamadas, sin detalles técnicos | El conteo de asignaciones no cambia |
| IT-08b | Un `PROFESOR` real intenta usar los dos endpoints | `403` en las dos, sin detalles técnicos | Igual que IT-08a |
| IT-08c | Un `ESTUDIANTE` real intenta usar los dos endpoints | `403` en las dos, sin detalles técnicos | Igual que IT-08a |

IT-07 son las 9 variantes: listas vacías, `profesorIds` vacío, `asignaturaIds` vacío, elemento nulo
en cada lista, cada lista ausente del cuerpo, un UUID mal formado y el cuerpo vacío.

## Seguridad y validación de capa web

`AsignacionDocenteSecurityWebTest` ejecuta la cadena JWT y `@PreAuthorize` real con dependencias
simuladas (sin BD): comprueba `401` sin token, `403` para PROFESOR/ESTUDIANTE y autorización
ADMINISTRADOR en ambos endpoints. `AsignacionDocenteControllerTest` (`@WebMvcTest`, sin filtros de
seguridad) cubre la deserialización, las validaciones (listas vacías, elementos nulos, JSON mal
formado) y la traducción de las tres excepciones de dominio a `404`. `ContratoErrorAsignacionDocenteTest`
cubre el contrato transversal de error (`415`, `406`, `409` por integridad) y que
`AsignacionDocenteExceptionHandler` conserve la precedencia sobre el advice global.
`AsignacionDocenteServiceImplTest` cubre la normalización de IDs repetidos. La única prueba que
combina **seguridad real end-to-end con persistencia real** es `AsignacionDocenteIntegracionIT`
(IT-08a/b/c): las demás pruebas de seguridad usan dependencias simuladas, sin tocar la base de datos.

## Ejecución

```bash
cd SIGRA
./gradlew test --tests '*AsignacionDocente*Test'
./gradlew integrationTest --tests '*AsignacionDocenteIntegracionIT*'
```

La segunda orden requiere una base con el esquema actual completo. Antes de ejecutarla, aplicar
las migraciones pendientes de `docs/db`, incluida
`RF07_asignacion_docente_unicidad_migration.sql`.

## Garantía de limpieza

Todos los datos que crea la prueba llevan el prefijo **ITG**: correos `itg-asigdoc-...@uco.net.co`,
programas `ITG Programa AsigDoc ...`, asignaturas `ITG-ASD-...` y tipo de documento `ITG Tipo
AsigDoc`. Un `@AfterEach` (que también corre al inicio de cada caso, por si una corrida anterior se
interrumpió) borra **solo** lo que cumple ese prefijo, en orden seguro: primero las asignaciones
docentes (tienen FK hacia profesor y asignatura), luego las asignaturas, luego los programas de
apoyo y por último los usuarios propios — profesor, administrador y estudiante, cada uno consultado
por su subtipo concreto (`ProfesorRepository`, `EstudianteRepository` y una consulta JPQL sobre
`Administrador`, que no tiene repositorio propio) y nunca por `UsuarioRepository.findAll()`:
`Usuario` es abstracta (herencia `JOINED`), y una fila de `usuario` sin fila en `profesor`,
`administrador` o `estudiante` impediría cargarla. Un usuario se considera de la prueba si su correo
empieza por `itg-asigdoc-` **o** si usa el tipo de documento `ITG Tipo AsigDoc`; el segundo criterio
evita que un registro con otro correo deje referenciado el tipo y rompa la limpieza de las corridas
siguientes. Finalmente se borra el tipo de documento. Nunca toca datos ajenos.

## Bruno

La carpeta `bruno/SIGRA/asignaciones-docentes/` contiene la secuencia manual: `00-sesion` inicia
sesión como administrador, profesor y estudiante; `01-asignar` crea una asignación, confirma la
omisión del duplicado, el rechazo por profesor/asignatura inexistente o inactiva y el error de
validación; `02-desasignar` desasigna, prueba el id inexistente y reasigna el mismo par para
confirmar la reactivación; `03-seguridad` cubre sin token, token alterado, rol PROFESOR y rol
ESTUDIANTE. Requiere los IDs de datos de prueba ya definidos en `environments/local.bru`.

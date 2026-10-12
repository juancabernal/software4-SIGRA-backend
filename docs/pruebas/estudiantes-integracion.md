# Pruebas de integración — módulo Estudiantes

Clase: `SIGRA/src/test/java/co/edu/uco/sigra/estudiantes/integracion/EstudianteIntegracionIT.java`

Levantan el contexto completo de Spring Boot contra **PostgreSQL real** y llaman a la API con `MockMvc`. Cada caso verifica la **respuesta HTTP** (estado y cuerpo sin detalles técnicos) y la **fila real** en `usuario`/`estudiante`/`matricula` con `EstudianteRepository`, `MatriculaRepository` y `UsuarioRepository`. La prueba **no** usa `@Transactional`: el servicio confirma de verdad, así se comprueba que las restricciones únicas — `(tipo_documento_id, numero_documento)` y `correo_institucional` en `usuario`, y `(estudiante_id, asignatura_id, semestre_id)` en `matricula` — existen de verdad en la base y no solo en el código de servicio.

**Datos de referencia.** `@BeforeEach` crea directamente por repositorio (sin pasar por los endpoints de otros módulos, para no acoplar esta prueba a su comportamiento): un `TipoDocumento` propio, una `Asignatura` ya `ACTIVA` (código `ITE-ASIG-1`, activada con `Asignatura.activar(5)` sin crear ningún `ResultadoAprendizaje` real) con su `ProgramaAcademico` de apoyo, y dos `Semestre` ya `ACTIVO` (`ITE-SEM-1` y `ITE-SEM-2`, el segundo solo para probar repitencia en IT-05).

**Seguridad.** Los `@PreAuthorize` de `EstudianteController` y `MatriculaController` siguen comentados hasta RF-05 y `SecurityConfig` usa `permitAll()`, así que estas pruebas no cubren roles. Cuando se active RF-05 para este módulo hay que añadir un caso de 401/403, igual que ya lo advierte `semestres-integracion.md` para el suyo.

| ID | Capacidad | Escenario | Datos de entrada | Resultado esperado (respuesta) | Resultado esperado (base) |
| --- | --- | --- | --- | --- | --- |
| IT-01 | `gestion-estudiantes` | Registrar y rechazar duplicados: documento repetido (incluso de uno ya inactivo) y correo repetido | `POST` con documento `990000001`; repetir el mismo documento; `DELETE` ese estudiante y repetir el documento otra vez; `POST` con documento `990000002`; `POST` con documento `990000003` y el correo del anterior | 201 `ACTIVO`; 409 "...ya está registrado en el sistema."; 204; 409 igual (el inactivo no libera el documento); 201; 409 "Ya existe un estudiante registrado con el correo..." | Una sola fila por documento en todo el flujo; el correo también se valida contra `UsuarioRepository` (todos los subtipos de `Usuario`, no solo `Estudiante`) |
| IT-02 | `gestion-estudiantes` | Validación de formato en el borde (parametrizada, 8 entradas) | Documento con letras, de 5 dígitos, de 11 dígitos; correo de otro dominio; y cada uno de `tipoDocumentoId`, `numeroDocumento`, `nombreCompleto`, `correoInstitucional` ausente | 400 en los 8 casos, cuerpo sin detalles técnicos | No se crea ninguna fila (`estudianteRepository.count()` sin cambios en cada caso) |
| IT-03 | `gestion-estudiantes` | Modificar: nombre/correo válidos, documento no modificable, correo de otro estudiante, id inexistente | `PUT` cambiando nombre y correo; `PUT` cambiando además `numeroDocumento`; `PUT` asignando el correo de un segundo estudiante registrado; `PUT` a un UUID aleatorio | 200 con los datos actualizados; 400 "El tipo y el número de documento del estudiante no se pueden modificar después del registro."; 409 "Ya existe un estudiante registrado con el correo..."; 404 "No existe un estudiante con id..." | La fila solo cambia en la modificación válida; el documento nunca cambia; ningún rechazo sobrescribe el correo |
| IT-04 | `gestion-estudiantes` | Inactivación lógica: sigue visible con estado `INACTIVO` y su matrícula no se toca | `POST` estudiante; `POST` matrícula (con la asignatura/semestre del `@BeforeEach`); `DELETE` estudiante; `GET /{id}`; `GET ?filtro=<documento>`; `DELETE` a un id inexistente | 204; 200 con estado `INACTIVO` y los mismos datos en ambas consultas; 404 en el id inexistente | La fila de estudiante queda `INACTIVO` sin perder documento/correo; la matrícula del estudiante sigue existiendo y en estado `ACTIVO` |
| IT-05 | `matricula-estudiantes` | Matricular y desvincular de extremo a extremo: terna única, repitencia, las tres validaciones de estado, desvinculación y reactivación | `POST` matrícula; repetir la misma terna; la misma asignatura en el segundo semestre (repitencia); con un estudiante inactivo, una asignatura `BORRADOR` y un semestre `INACTIVO` (variantes creadas para el caso); `DELETE` la matrícula original; repetir el `DELETE`; volver a matricular la terna original | 201; 409 "...ya está matriculado en la asignatura..."; 201 (repitencia, terna distinta); 400 en los tres casos de estado, uno por cada regla (`ReglaDeMatriculaException`); 204; 409 "...ya no está activa."; 200 (reactivación, no 201) | Una sola fila por terna en todo el flujo (lo garantiza también `uk_matricula_terna`); la reactivación final reutiliza el mismo id de matrícula, sin crear una fila nueva |

## Cómo ejecutarlas

Requieren PostgreSQL con el esquema de `docs/db` y un `SIGRA/.env` válido. No se ejecutan con `./gradlew test`.

```bash
cd SIGRA
./gradlew integrationTest --tests '*EstudianteIntegracionIT*'   # solo estas
./gradlew integrationTest                                       # todas las de integración
```

## Garantía de limpieza

Todos los datos que crea esta prueba llevan un prefijo reservado, distinto de los que ya usan `semestres` (años `219X`) y `asignaturas` (`ITG-`), para que las tres clases puedan ejecutarse en cualquier orden sin pisarse:

- `numeroDocumento` de estudiante: rango numérico `990000xxx` (el patrón `\d{6,10}` de `EstudianteRequestDTO` no admite letras, por eso el prefijo es numérico en vez de alfabético).
- `correoInstitucional`: prefijo `ite-` (p. ej. `ite-001@uco.net.co`).
- Código de la asignatura de apoyo: prefijo `ITE-ASIG-`.
- Código de los semestres de apoyo: prefijo `ITE-SEM-`.
- Nombre del `TipoDocumento` propio: `"ITE Tipo"`; nombre del `ProgramaAcademico` de apoyo: prefijo `"ITE Programa"`.

Un `@AfterEach` (que también corre al inicio de cada caso, en `@BeforeEach`, por si una corrida anterior se interrumpió) borra **solo** las filas con esos prefijos, en el orden que respeta las llaves foráneas: primero las matrículas de los estudiantes propios (usando `MatriculaRepository.buscarPorEstudiante`, que hace `join fetch` y evita inicializar relaciones `LAZY` fuera de una transacción), luego los estudiantes, luego la asignatura y su programa de apoyo, y por último los semestres y el tipo de documento.

Esa limpieza de matrículas recorre los estudiantes propios en vez de filtrar por el código de la asignatura o del semestre en `Matricula`: en esta clase toda matrícula que se crea referencia siempre a uno de sus propios estudiantes, así que ese recorrido basta y es más simple que el de `AsignaturaIntegracionIT` (que sí necesita filtrar por asignatura porque sus matrículas de prueba pueden venir de estudiantes ajenos a esa clase).

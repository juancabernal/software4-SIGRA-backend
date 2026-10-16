# Pruebas del módulo de matrícula — RF-08, RF-09, RF-09a

> Actualizado para la integración definitiva de `main` y `develop` (2026-10-16). La autorización
> ya no usa interceptores ni un interruptor de desarrollo: se aplica mediante Spring Security,
> `@PreAuthorize`, `CurrentUser` y `SecurityAuthorizationService`. Las referencias históricas a
> `ControlAccesoMatriculaInterceptorTest`, `MatriculaControllerRolesTest` y sus anotaciones
> antiguas quedan sustituidas por las pruebas JWT/RBAC de `SecurityRf05IntegrationIT` y las
> pruebas unitarias del servicio.

A diferencia de `estudiantes-integracion.md` (que documenta una sola clase `*IntegracionIT`), este
archivo documenta la cobertura de pruebas de **todo el módulo de matrícula** del cambio
`matricula-desvinculacion`, repartida en seis lugares:

1. `SIGRA/src/test/java/co/edu/uco/sigra/estudiantes/service/MatriculaServiceImplTest.java` — unitaria,
   Mockito, sin contexto de Spring.
2. `SIGRA/src/test/java/co/edu/uco/sigra/estudiantes/controller/MatriculaControllerTest.java` —
   `@WebMvcTest`, servicio mockeado.
3. `SIGRA/src/test/java/co/edu/uco/sigra/auth/integration/SecurityRf05IntegrationIT.java` —
   contexto completo con JWT, roles, propiedad docente y alcance de matrícula.
4. La guardia arquitectónica `PrivateEndpointAuthorizationArchitectureTest` — verifica que cada
   mapping privado del API tenga autorización explícita.
5. `SIGRA/src/test/java/co/edu/uco/sigra/estudiantes/integracion/EstudianteIntegracionIT.java`, casos
   **IT-05** a **IT-08** — contexto completo contra PostgreSQL real.
6. La colección Bruno: `bruno/SIGRA/estudiantes/04-matricular/`, `05-desvincular-consultar/`,
   `06-registrar-con-matricula/` y `07-confirmacion-desvincular/`.

Los rótulos **PN-35** (matricular) y **PN-36** (desvincular) son los que `proposal.md` deriva del
requisito, tal como aparecen en los `@DisplayName` reales de abajo (ver la nota en `proposal.md` sobre
por qué no hay un plan de pruebas previo con esos códigos).

## 1. `MatriculaServiceImplTest` (unitaria, Mockito)

Clase anotada `@DisplayName("Servicio de matrículas")`. Usa un `Clock.fixed` inyectado como
`relojMatriculas`, así que las fechas de los casos son deterministas.

### Clase anidada "Matricular"

| Caso (`@DisplayName`) | Qué verifica |
| --- | --- |
| PN-35 Con terna nueva y los tres activos, crea la matrícula ACTIVA sin reactivarla | Camino feliz: 201 lógico, `ResultadoMatricula.reactivada() == false` |
| Con un estudiante inexistente lanza excepción sin consultar asignatura, semestre ni la terna | Corte temprano: no se consulta nada más tras el 404 |
| Con una asignatura inexistente lanza excepción sin consultar el semestre ni la terna | Ídem, segundo paso |
| Con un semestre inexistente lanza excepción sin consultar la terna | Ídem, tercer paso |
| Con el estudiante INACTIVO lanza excepción sin consultar la terna | Regla de estado del estudiante (`EstadoRegistro`, no `EstadoMatricula`) |
| Con la asignatura en BORRADOR lanza excepción sin consultar la terna | Regla de estado de la asignatura |
| PN-35 Con la asignatura INACTIVA lanza excepción sin consultar la terna | Ídem, el otro estado no admitido |
| PN-35 Con el semestre cuya fecha de fin ya pasó lanza excepción aunque la marca almacenada diga ACTIVO | **D3**: vigencia por fechas, no por el `estado` guardado |
| PN-35 Con el semestre cuya fecha de inicio es posterior a hoy lanza excepción | El otro borde de "no vigente" |
| Con la fecha de inicio del semestre igual a hoy sí permite matricular | Borde inclusivo inferior |
| Con la fecha de fin del semestre igual a hoy sí permite matricular | Borde inclusivo superior |
| PN-35 Con la terna ya matriculada y ACTIVA lanza excepción y no guarda | Unicidad lógica antes de la de base de datos |
| PN-35 Con el mismo estudiante y asignatura ya matriculados en otro semestre, crea una matrícula nueva e independiente | Repitencia: dos filas, no una compartida |
| Con la terna retirada (DESVINCULADA) existente, reactiva la misma matrícula en lugar de crear una nueva | Rematrícula: mismo id, no un segundo registro |
| Ninguna de las rutas de matricular invoca métodos de borrado en ningún repositorio | Verificación negativa transversal |
| Al crear la matrícula le queda la fecha del reloj, no una fecha del cliente | **D2** |
| Al reactivar una matrícula retirada, su fecha pasa a ser la del día de la rematrícula | **D2**, con el reloj movido a otro día (la única forma de probarlo de verdad) |
| La matrícula reactivada conserva su identificador original aunque su fecha cambie | Id estable a través de la reactivación |

### Clase anidada "Desvincular"

| Caso (`@DisplayName`) | Qué verifica |
| --- | --- |
| Con una matrícula ACTIVA la pasa a DESVINCULADA | Camino feliz |
| PN-36 Con una matrícula inexistente lanza excepción sin consultar las calificaciones | Orden: existencia antes que confirmación |
| PN-36 Con una matrícula ya DESVINCULADA lanza excepción y no guarda ni consulta las calificaciones | Orden: estado antes que confirmación |

Sub-anidada **"PN-36 Confirmación adicional para desvincular con calificaciones registradas"** (puerto
`CalificacionesDeMatricula` mockeado):

| Caso (`@DisplayName`) | Qué verifica |
| --- | --- |
| PN-36 Con 1 calificación registrada y sin confirmar, rechaza indicando la cantidad y no guarda | 409 lógico + cantidad en el mensaje |
| PN-36 Con varias calificaciones registradas y sin confirmar, el mensaje usa la cantidad real, no un literal fijo | El número no está "quemado" en el mensaje |
| PN-36 Con calificaciones registradas y confirmando, desvincula la matrícula | Camino feliz con confirmación |
| Sin calificaciones registradas y confirmando de todos modos, desvincula sin error | `confirmar=true` nunca rompe el camino sin calificaciones |
| PN-36 Con calificaciones registradas y confirmando, no invoca ningún método de borrado | La contraparte verificable de "las calificaciones sobreviven" |
| Tras desvincular con calificaciones y confirmando, el objeto guardado conserva el mismo id y la misma fecha de matrícula | La desvinculación no reescribe identidad ni fecha |
| Consulta las calificaciones exactamente una vez, tanto al rechazar sin confirmar como al desvincular confirmando | No hay doble conteo ni reintentos ocultos |
| Sin confirmar, reintentar la desvinculación dos veces seguidas rechaza ambas veces por igual, sin memoria del intento anterior | La confirmación es un acto del cliente, el sistema no la infiere |

### Clases anidadas "Listar matriculados" y "Listar asignaturas de estudiante"

| Caso (`@DisplayName`) | Qué verifica |
| --- | --- |
| Con una asignatura inexistente lanza excepción sin consultar el semestre | Orden de validación en el listado |
| Con un semestre inexistente lanza excepción | Ídem |
| Con incluirInactivas en false delega la consulta con el estado ACTIVA | Filtro por defecto |
| Con incluirInactivas en true delega la consulta con estado nulo | Filtro explícito, incluye desvinculadas |
| Sin matriculados devuelve lista vacía sin lanzar excepción | 200 con `[]`, no 404 |
| Con un estudiante inexistente lanza excepción sin consultar el historial | Historial por estudiante, validación previa |
| Con historial de varias matrículas devuelve la lista tal cual la entrega el repositorio | Sin transformación oculta |
| Sin matrículas devuelve lista vacía | 200 con `[]` |

### Clase anidada "Registrar y matricular (alta con estudiante nuevo)" — RF-09a

| Caso (`@DisplayName`) | Qué verifica |
| --- | --- |
| RF-09a Con datos válidos, registra al estudiante y lo matricula en una sola operación, sin reactivar nada | Camino feliz, **D6** |
| Con el documento ya registrado, propaga la excepción del registro sin envolverla y no consulta nada de la matrícula | Delegación pura en `EstudianteService.registrar`, sin tocar la rama de matrícula |
| Con el correo ya registrado, propaga la excepción del registro sin envolverla y no consulta nada de la matrícula | Ídem, la otra unicidad |
| Tarea 4.8: el mensaje de documento duplicado llega idéntico, letra por letra, al que lanza el registro normal | Garantía contra divergencia entre los dos caminos de alta |
| PN-35 Con la asignatura no activa, registra al estudiante pero rechaza la matrícula y no la guarda | El estudiante sí se registra; la matrícula no — la mitad atómica que un mock puede observar |
| PN-35 Con el semestre no vigente, registra al estudiante pero rechaza la matrícula y no la guarda | Ídem |
| Con una asignatura inexistente, registra al estudiante pero lanza excepción al matricular | Ídem, referencia inexistente |
| Con un semestre inexistente, registra al estudiante pero lanza excepción al matricular | Ídem |

> Con mocks **no** se puede comprobar el rollback real (que el estudiante quede sin persistir en la
> base cuando la matrícula falla): estos casos solo comprueban que, en la secuencia de llamadas, la
> matrícula fallida no se guarda. La atomicidad real contra PostgreSQL es **IT-06** (sección 5).

## 2. `MatriculaControllerTest` (`@WebMvcTest`)

`@DisplayName("MatriculaController: deserialización, validación y códigos HTTP (RF-08, RF-09)")`.
Servicio mockeado; cubre deserialización, validación de DTO y mapeo de excepciones a código HTTP.

### `POST /api/v1/matriculas`

| Caso | Qué verifica |
| --- | --- |
| Cuerpo válido y matrícula nueva devuelve 201 con la matrícula creada | Incluye `fechaMatricula` y `estado: ACTIVA` en el cuerpo |
| Cuerpo válido y matrícula reactivada devuelve 200, no 201 | El controlador distingue por `ResultadoMatricula.reactivada()` |
| Una fecha enviada en el cuerpo se ignora: la matrícula se crea con la del sistema | Spec: "La fecha no la decide el cliente" |
| Campo faltante (semestreId) devuelve 400 con el campo señalado en detalles | Validación de DTO |
| Terna ya matriculada y activa devuelve 409 sin detalles | Mapeo de `MatriculaDuplicadaException` |
| Estudiante inactivo / Asignatura no activa / Semestre no vigente devuelve 400 sin detalles | Mapeo de `ReglaDeMatriculaException`, mensaje actualizado a "no vigente" |
| Estudiante / Asignatura / Semestre inexistente devuelve 404 sin detalles | Mapeo de `EstudianteNoEncontradoException` / `ReferenciaNoEncontradaException` |

### `POST /api/v1/matriculas/con-estudiante` — RF-09a

| Caso | Qué verifica |
| --- | --- |
| Cuerpo válido devuelve 201 con el estudiante y la matrícula creados | Forma de `EstudianteConMatriculaResponseDTO` |
| Documento inválido anidado en estudiante devuelve 400 con el campo señalado en detalles | Cascada de `@Valid` sobre `EstudianteRequestDTO` anidado |
| Correo con dominio inválido anidado en estudiante devuelve 400 con el campo señalado en detalles | Ídem |
| Estudiante ausente del cuerpo devuelve 400 con el contenedor señalado en detalles | `@NotNull` sobre el contenedor, sin cascada porque no hay objeto |
| asignaturaId / semestreId ausente devuelve 400 con el campo señalado en detalles | Validación de los campos propios del DTO compuesto |
| Documento / Correo ya registrado devuelve 409 sin detalles | Delegación en `EstudianteService.registrar` |
| Asignatura / Semestre inexistente devuelve 404 sin detalles | Delegación en la lógica de `matricular` |
| Asignatura no activa / Semestre no vigente devuelve 400 sin detalles | Ídem |

### `DELETE /api/v1/matriculas/{id}`

| Caso | Qué verifica |
| --- | --- |
| Caso feliz devuelve 204 sin cuerpo | Desvinculación sin calificaciones |
| Matrícula ya inactiva devuelve 409 sin detalles | `MatriculaYaInactivaException` |
| Matrícula inexistente devuelve 404 sin detalles | `MatriculaNoEncontradaException` |
| Matrícula con calificaciones y sin confirmar devuelve 409 pidiendo confirmación | `ConfirmacionRequeridaException`, con el servicio mockeado para simular calificaciones |
| Matrícula con calificaciones y confirmar=false explícito se comporta igual que sin el parámetro | El valor explícito `false` no cambia nada frente al valor por defecto |
| Matrícula con calificaciones y confirmar=true devuelve 204 | Camino feliz con confirmación |
| Matrícula ya inactiva con confirmar=true sigue devolviendo 409, no 204 | Spec: "La confirmación no exime de las demás reglas" |
| Matrícula inexistente con confirmar=true sigue devolviendo 404 | Spec: "La confirmación no sustituye a la existencia de la matrícula" |
| confirmar con un valor no booleano devuelve un error 4xx, no 500 | Entrada malformada en el `@RequestParam` |

### `GET /api/v1/matriculas?asignaturaId=&semestreId=&incluirInactivas=`

| Caso | Qué verifica |
| --- | --- |
| Sin incluirInactivas devuelve 200 y llama al servicio con incluirInactivas=false | Valor por defecto |
| Con incluirInactivas=true llama al servicio con ese valor | Parámetro explícito |
| Lista vacía devuelve 200 con un arreglo vacío, no 404 | Spec: "Asignatura sin matriculados" |

## 3. Seguridad de matrícula y regresiones RBAC

La autorización se prueba en la infraestructura común de Spring Security. Se cubren ausencia o
invalidez del JWT (401), rol insuficiente (403), asignatura fuera del alcance del profesor,
matrícula ajena, estudiante propio y confirmación de desvinculación. La prueba arquitectónica
evita que un endpoint nuevo dependa únicamente de `anyRequest().authenticated()`.

## 4. `EstudianteIntegracionIT` — casos de matrícula (contexto completo + PostgreSQL real)

Ya documentada en `estudiantes-integracion.md` para IT-01 a IT-05 (que incluye la matrícula básica:
terna única, repitencia, las tres validaciones de estado, desvinculación y reactivación). Los casos
nuevos de este cambio, que ese archivo todavía no lista porque está fuera del alcance de esta tarea
actualizarlo, son:

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| IT-06 | Registrar y matricular en un paso: si la matrícula falla (semestre no vigente, o asignatura inexistente), no queda ni el estudiante ni su usuario | Comprueba el **rollback real** del `@Transactional`: ni `estudianteRepository` ni `usuarioRepository` tienen la fila tras el fallo. Es el único escenario del spec que un mock no puede demostrar (design.md, nota de la tarea 4.10) |
| IT-07 | Registrar y matricular en un paso: camino feliz de punta a punta | 201 con el estudiante y la matrícula; el estudiante creado se puede consultar por separado; aparece en el listado de matriculados con su `fechaMatricula` real |
| IT-08 | Registrar y matricular en un paso: documento duplicado da el mismo 409 que el registro normal | Mismo mensaje, letra por letra, que el registro por separado; no queda una segunda fila ni una matrícula huérfana |

## 5. Colección Bruno (`bruno/SIGRA/estudiantes/`)

| Carpeta | Casos | Qué cubre |
| --- | --- | --- |
| `04-matricular/` | 9 | Camino feliz con `fechaMatricula` y `estado: ACTIVA`; terna duplicada; repitencia en otro semestre; asignatura en borrador; semestre no vigente (mensaje actualizado); estudiante inactivo; estudiante/asignatura/semestre inexistente |
| `05-desvincular-consultar/` | 6 | Retirar; retiro repetido; rematrícula reactiva (con `estado: ACTIVA` y la fecha recalculada); listado sin inactivas / con inactivas (literales `ACTIVA`/`DESVINCULADA` y `fechaMatricula` en cada fila); asignaturas de un estudiante (con `fechaMatricula` y `estado`) |
| `06-registrar-con-matricula/` (nueva, RF-09a) | 3 | Alta y matrícula válidas (201); documento ya registrado (409); documento con formato inválido (400, `detalles` nombra el campo anidado) |
| `07-confirmacion-desvincular/` (nueva) | 4 | Desvincular con calificaciones sin confirmar (409 — **no ejecutable hoy**, ver más abajo); desvincular con calificaciones confirmando (204, sí ejecutable) |

Todas las peticiones de error verifican el contrato transversal (`timestamp`, `status`, `error`,
`mensaje`, sin `Exception` ni `at co.edu`).

### Verificación realizada contra el backend real

Se ejecutó la colección completa (`bru run estudiantes -r`) contra un backend local (commit actual,
construido con `./gradlew bootRun`) y PostgreSQL con la migración
`docs/db/RF08_matricula_fecha_y_estado_migration.sql` ya aplicada. Resultado: **38/41 peticiones,
69/72 tests y 40/41 asserts en verde.** Las 3 excepciones:

- Dos asserts de `05-desvincular-consultar` (`05-listado-con-inactivas` y `06-asignaturas-de-estudiante`)
  fallaron **solo** por una limitación del entorno de verificación (una fila ajena a la colección,
  descrita abajo, impidió recrear `BRUEST-A1`/`BRUEST-A2` con sus ids fijos, y la corrida tuvo que
  usar semestres de reemplazo con otro código); no son defectos de las peticiones.
- La petición `07-confirmacion-desvincular/02` falló exactamente como su `docs` documenta: hoy
  responde 204, no 409, porque `CalificacionesDeMatriculaPorDefecto` siempre cuenta 0.

## Qué queda sin verificar en producción

1. **La confirmación por calificaciones nunca se dispara en producción todavía.**
   `CalificacionesDeMatricula` solo tiene publicado el bean por defecto
   (`CalificacionesDeMatriculaPorDefecto`), que **siempre** devuelve `0` (design.md, D4). Como la
   entidad `Calificacion` no existe —el módulo `calificaciones`, de Simón Tabares, sigue vacío—,
   ninguna matrícula puede tener calificaciones reales, así que `MatriculaServiceImpl.desvincular`
   nunca entra a la rama que exige confirmación. La regla **está** implementada y probada hoy, pero
   solo contra el puerto simulado con Mockito: la clase anidada "PN-36 Confirmación adicional..." de
   `MatriculaServiceImplTest`, los casos de `confirmar` en `MatriculaControllerTest`, y la petición
   `07-confirmacion-desvincular/02-desvincular-con-calificaciones-sin-confirmar.bru` de la colección
   Bruno (que queda deliberadamente en rojo con un `docs` que lo explica). El 409 real solo aparecerá
   cuando `calificaciones` publique su propio bean de `CalificacionesDeMatricula` — y ese día, según
   D4, el cambio no debería requerir tocar una sola línea de `estudiantes`.

2. **El control de acceso ADMIN/PROFESOR está activo por defecto.** La cadena de Spring Security exige
   JWT y los endpoints de matrícula declaran `@PreAuthorize`; además, el servicio comprueba la
   asignatura autorizada y conserva el alcance de recurso. No existe una propiedad que desactive
   permisos en producción. La cobertura equivalente está en `SecurityRf05IntegrationIT`, la guardia
   arquitectónica y `MatriculaServiceImplTest`.

## Nota sobre el entorno de verificación

Al ejecutar la colección completa se encontró una fila de datos ajena a la colección Bruno
(`sigra.asignatura`, código `MAT-12`, bajo el mismo programa `BRU-EST` que usa el seed de este
módulo) que impidió que `seed-estudiantes.sql` completara su limpieza idempotente
(`DELETE FROM sigra.programa_academico WHERE codigo = 'BRU-EST'` falla por la FK de esa fila ajena).
No es un defecto de este seed ni de las peticiones: es contaminación de un entorno local compartido
con otros proyectos. Se reporta para que quien administre esa base la limpie; el seed en sí es
correcto (se validó su sintaxis y sus fechas relativas con consultas de solo lectura, y se corrió
con éxito la parte de inserción equivalente contra datos nuevos sin ese conflicto).

Aparte de esa fila, la verificación también encontró y corrigió un problema real en
`seed-estudiantes.sql`: los semestres `BRUEST-A1`/`BRUEST-A2` usaban años fijos lejanos (2080/2081)
que, antes de este cambio, daban igual porque la vigencia se leía del `estado` guardado. Con la
vigencia por fechas (**D3**, commit 2), esos años fijos dejan de ser vigentes apenas pasa el momento
en que se escribieron, y de hecho ya estaban recalculados a `INACTIVO` en la base verificada. El seed
ahora calcula esas fechas en relación con `CURRENT_DATE`, para que sigan vigentes sin mantenimiento.

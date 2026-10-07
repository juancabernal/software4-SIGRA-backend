# Pruebas de integración — módulo Profesores

Clase: `SIGRA/src/test/java/co/edu/uco/sigra/profesores/integracion/ProfesorIntegracionIT.java`

Levantan el contexto completo de Spring Boot contra **PostgreSQL real** y llaman a la API con `MockMvc`. Cada caso verifica la **respuesta HTTP** (estado y cuerpo sin detalles técnicos) y la **fila real** en `usuario`/`profesor`/`asignacion_docente` con `ProfesorRepository` y `AsignacionDocenteRepository`. La prueba **no** usa `@Transactional`: el servicio confirma de verdad, así se comprueba lo que realmente queda en la base.

**Datos de referencia.** `@BeforeEach` crea por repositorio un `TipoDocumento` propio (`ITP Tipo`). Los casos que necesitan asignaciones (IT-04 e IT-05) crean directamente por repositorio un `ProgramaAcademico`, una o dos `Asignatura` y sus `AsignacionDocente`, sin pasar por los endpoints de otros módulos para no acoplarse a su comportamiento.

**Seguridad.** Los `@PreAuthorize` de `ProfesorController` siguen comentados hasta RF-05 y `SecurityConfig` usa `permitAll()`, así que estas pruebas no cubren roles. Cuando se active RF-05 para este módulo hay que añadir un caso de 401/403, igual que ya lo advierten `semestres-integracion.md` y `estudiantes-integracion.md`.

| ID | Capacidad | Escenario | Datos de entrada | Resultado esperado (respuesta) | Resultado esperado (base) |
| --- | --- | --- | --- | --- | --- |
| IT-01 | Registrar profesor | Registrar con correo sucio y luego rechazar documento y correo duplicados | `POST` documento `990000001`, nombre `ITP Profesor Uno`, correo `"  ITP-Uno@UCO.NET.CO "`; después `POST` con el mismo documento y otro correo; después `POST` con documento nuevo y correo `ITP-UNO@UCO.NET.CO` | 201 con correo `itp-uno@uco.net.co`, estado `ACTIVO` y `tieneAsignacionesActivas` falso; los dos duplicados dan 409 sin detalles técnicos | Una sola fila con documento `990000001`, correo normalizado y `ACTIVO`; el documento duplicado no crea una segunda fila; el correo duplicado no crea la fila `990000002` |
| IT-02 | Registrar profesor | Entradas inválidas (9 variantes parametrizadas) | Número con letras, de 5 dígitos, de 11 dígitos; correo de dominio externo, correo sin formato; `tipoDocumentoId` nulo; número de documento nulo; nombre vacío; correo nulo | 400 en cada una, sin detalles técnicos | El conteo de profesores no cambia |
| IT-03 | Modificar profesor | Modificar nombre y correo, proteger el documento y rechazar correo ajeno | `PUT` con nombre `ITP Profesor Modificado` y correo `"  ITP-020-NUEVO@UCO.NET.CO "`; `PUT` cambiando el documento a `990000021`; con un segundo profesor creado, `PUT` al primero usando el correo del segundo en mayúsculas | 200 con nombre y correo (normalizado) nuevos y el mismo documento; 400 al cambiar el documento; 409 al usar el correo del otro | Tras el 200 la fila tiene los valores nuevos; tras el 400 y el 409 la fila del primero no cambia y el segundo conserva su correo |
| IT-04 | Inactivar profesor | Rechazar si tiene asignaciones activas y permitir cuando ya no las tiene | Profesor con una `AsignacionDocente` `ACTIVO`; `DELETE`; luego se pone la asignación en `INACTIVO` a mano y se repite `DELETE`; `DELETE` de un id inexistente | 400; luego 204; 404 para el inexistente (todos los errores sin detalles técnicos) | Tras el 400 el profesor sigue `ACTIVO` y la asignación intacta; tras el 204 el profesor queda `INACTIVO` y la asignación sigue existiendo (no se borra) |
| IT-05 | Consultar profesores y asignaturas | Listar, filtrar por documento y por nombre, y consultar asignaturas por estado | Dos profesores; `GET` sin filtro, con `?filtro=990000040` y con `?filtro=Consulta Uno`; un profesor con una asignación `ACTIVO` y otra `INACTIVO`; `GET /{id}/asignaturas` sin estado, con `?estado=ACTIVO` y con `?estado=INACTIVO`; profesor sin asignaciones; id inexistente | 200 con ambos profesores en la lista; los filtros devuelven exactamente 1; asignaturas: 2 sin estado, 1 con `ACTIVO` (solo `ITP-ASIG-1`), 1 con `INACTIVO` (solo `ITP-ASIG-2`); lista vacía para el profesor sin asignaciones; 404 para el inexistente | Solo lectura: las pruebas no modifican filas durante las consultas |

## Cómo ejecutarlas

Requieren PostgreSQL con el esquema de `docs/db` y un `SIGRA/.env` válido. No se ejecutan con `./gradlew test`.

```bash
cd SIGRA
./gradlew integrationTest --tests '*ProfesorIntegracionIT*'   # solo estas
./gradlew integrationTest                                      # todas las de integración
```

## Garantía de limpieza

Todos los datos que crea la prueba llevan el prefijo **ITP**: correos `itp-...@uco.net.co`, programas `ITP Programa ...`, asignaturas `ITP-ASIG-...` y tipo de documento `ITP Tipo`. Un `@AfterEach` (que también corre al inicio de cada caso, por si una corrida anterior se interrumpió) borra **solo** lo que cumple ese prefijo, en orden seguro: asignaciones docentes, asignaturas, programas, usuarios/profesores y tipo de documento. Un usuario se considera de la prueba si su correo empieza por `itp-` **o** si usa el tipo de documento `ITP Tipo`; el segundo criterio evita que un registro con otro correo deje referenciado el tipo de documento y rompa la limpieza de las corridas siguientes. Nunca toca datos ajenos.
# Pruebas de integración — módulo Semestres

Clase: `SIGRA/src/test/java/co/edu/uco/sigra/semestres/integracion/SemestreIntegracionIT.java`

Levantan el contexto completo de Spring Boot contra **PostgreSQL real** y llaman a la API con `MockMvc`. Cada caso verifica la **respuesta HTTP** (estado y cuerpo sin detalles técnicos) y la **fila real** en `sigra.semestre` con `SemestreRepository`. La prueba **no** usa `@Transactional`: el servicio confirma de verdad.

**Fecha controlada.** El reloj del módulo (bean `relojSemestres`) se reemplaza con `@MockitoBean`, así "hoy" es una fecha fija del año 2190 (por defecto 2190-01-10) y cada caso puede moverla. Con eso se prueba el cálculo del estado sin depender de la fecha en que se ejecuten.

**Seguridad.** Los `@PreAuthorize` del módulo siguen comentados hasta RF-05 y `SecurityConfig` usa `permitAll()`, así que estas pruebas no cubren roles. Cuando se active RF-05 hay que añadir un caso de 401/403.

| ID | Requisito | Escenario | Datos de entrada (hoy = 2190-01-10) | Resultado esperado (respuesta) | Resultado esperado (base) |
| --- | --- | --- | --- | --- | --- |
| IT-01 | RF Semestre: crear y consultar | Registrar uno en curso, uno futuro y un código repetido | `POST 2190-1` 2190-01-10 → 2190-06-10; `POST 2190-2` 2190-07-20 → 2190-12-05; `GET /2190-2`; `POST 2190-1` otra vez con fechas que no se cruzan | 201 `ACTIVO`; 201 `INACTIVO`; 200 con los datos; 409 sin detalles técnicos | Filas con código, fechas y estado correctos; solo 2 filas reservadas; el original no cambia |
| IT-02 | RF Semestre: validaciones | 13 entradas inválidas (parametrizada) | Código `2190-3`, `219-1`, `AAAA-1`, vacío o nulo; fechas nulas; fin igual o anterior al inicio; inicio en el pasado; año del código ≠ año de inicio; fecha inexistente `2190-13-45`; formato `01/02/2190` | 400 en cada una, sin detalles técnicos | No se guarda ninguna fila |
| IT-03 | RF Semestre: sin cruces | Cruces contra un semestre base 2190-02-01 → 2190-06-30 (parametrizada, 6 casos) | Termina el día que empieza el base; empieza el día que termina el base; cruce parcial; contenido; contiene; mismas fechas. Después, uno contiguo 2190-07-01 → 2190-11-30 | 409 en los 6 cruces; 201 el contiguo | El que se cruza no se guarda; el base no cambia; el contiguo sí se guarda |
| IT-04 | RF Semestre: extender fechaFin | Extender, acortar, repetir, cruzar, inexistente y semestre terminado | `2190-1` (01-10 → 05-31) y `2190-2` (07-01 → 11-30); `PATCH` a 06-15, 06-01, 06-15, 07-01, `{}`, `2199-2`; hoy = 2190-06-16 y `PATCH 2190-1` a 06-25; `PATCH 2190-2` a 12-15 | 200; 400; 400; 409; 400; 404; 400; 200 `INACTIVO` | Solo cambian las dos extensiones válidas; fechaInicio intacta; los rechazos no alteran fechaFin |
| IT-05 | RF Semestre: estado por fechas | El estado se recalcula al consultar y se guarda, con extremos incluidos | `2190-2` (07-01 → 11-30); `GET /2190-2` con hoy = 06-30, 07-01, 11-30 y 12-01; `GET /2199-1` | `INACTIVO`, `ACTIVO`, `ACTIVO`, `INACTIVO`; 404 | La columna `estado` queda igual que la respuesta en cada paso (sin proceso programado) |

## Cómo ejecutarlas

Requieren PostgreSQL con el esquema de `docs/db` y un `SIGRA/.env` válido. No se ejecutan con `./gradlew test`.

```bash
cd SIGRA
./gradlew integrationTest --tests '*SemestreIntegracionIT*'   # solo estas
./gradlew integrationTest                                     # todas las de integración
```

## Garantía de limpieza

Todos los semestres que crea la prueba usan códigos de los **años reservados 2190–2199** (`219X-1` / `219X-2`). Ningún dato real ni seed del equipo usa ese rango (los seeds usan 1980, 1990, 2075, 2080, 2081 y 2090–2099), así que no se cruzan con semestres existentes. Un `@AfterEach` (que también corre al inicio de cada caso, por si una corrida anterior se interrumpió) borra **solo** los semestres con esos códigos. Las pruebas no crean matrículas, así que no hay claves foráneas que lo impidan.

La prueba **no** llama a `GET /api/v1/semestres` (listar): ese endpoint recalcula y guarda el estado de todos los semestres de la base, y con el reloj fijado en 2190 dejaría inactivos los semestres reales de tu base local. Solo consulta por código, que toca únicamente la fila consultada.

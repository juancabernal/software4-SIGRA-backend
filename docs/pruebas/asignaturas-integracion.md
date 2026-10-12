# Pruebas de integración — módulo Asignaturas

Clase: `SIGRA/src/test/java/co/edu/uco/sigra/asignaturas/integracion/AsignaturaIntegracionIT.java`

Levantan el contexto completo de Spring Boot contra **PostgreSQL real**, con `MockMvc` y el control por rol encendido (`sigra.seguridad.roles.habilitado=true`). Cada caso verifica la **respuesta HTTP** (estado y cuerpo sin detalles técnicos) y el **estado real en la base** con los repositorios. La prueba **no** usa `@Transactional`: el servicio confirma de verdad, así que el rollback de IT-04b es real. Los casos 1 a 4 usan el token JWT real de un administrador de prueba (`JwtService.generarToken`); IT-05 usa tokens reales de un profesor y un estudiante creados por la prueba.

| ID | Requisito | Escenario | Datos de entrada | Resultado esperado (respuesta) | Resultado esperado (base) |
| --- | --- | --- | --- | --- | --- |
| IT-01 | RF-03a | Registrar con datos sucios y luego duplicado | `POST` código `" itg-101 "`, nombre `"  Cálculo   Integral  "`; después `POST` con `"Itg-101"` | 201 con código `ITG-101`, nombre `Cálculo Integral`, estado `BORRADOR`, `cantidadRa` 0 y el programa enviado; el duplicado da 409 | Una sola fila `ITG-101`, normalizada, en `BORRADOR`, del programa correcto y sin RA |
| IT-02 | RF-03a | Entradas inválidas (10 variantes parametrizadas) | Código `AB`, de 21 caracteres, `AB C`, `-AB`, `A--B`; nombre `AB`, de 101 caracteres, `<script>x</script>`, con carácter de control; `programaId` nulo | 400 en cada una; el mensaje no repite el valor enviado ni expone detalles técnicos | El conteo de asignaturas no cambia |
| IT-03 | RF-03c | Activar según la cantidad de RA activos | `PATCH /{id}/activar` con 4 RA activos; con 5 activos + 2 inactivos; otra asignatura con 8 activos | 400; 200 `ACTIVA` con `cantidadRa` 5; 400 | Sigue `BORRADOR`; queda `ACTIVA`; sigue `BORRADOR` |
| IT-04a | RF-03d | Inactivar y reactivar en cascada | Asignatura `ACTIVA` con 5 RA activos, 1 RA inactivado a mano, una matrícula `ACTIVA` y una asignación docente; `PATCH /inactivar` y luego `/activar` | 200 `INACTIVA`; luego 200 `ACTIVA` con `cantidadRa` 5 | Los 5 RA quedan `INACTIVO` con `inactivado_con_asignatura`; el manual no queda marcado; matrícula y asignación intactas. Al reactivar vuelven solo esos 5 y el manual sigue `INACTIVO` |
| IT-04b | RF-03d | Reactivar con RA insuficientes (rollback) | Asignatura `INACTIVA` con solo 4 RA marcados; `PATCH /activar` | 400 | Verificado después de la respuesta: sigue `INACTIVA` y sus 4 RA siguen `INACTIVO` y marcados |
| IT-05 | RF-03b | Roles y alcance con tokens reales | Sin token; estudiante y profesor en `POST`; profesor asignado a A y B (no a C); estudiante matriculado solo en A; administrador en `/mias` | 401; 403; 403; lista del profesor = A y B; detalle de A 200 y de C 403; `/mias` profesor = A y B; `/mias` estudiante = A; administrador en `/mias` 403 | Los `POST` rechazados no crean filas |

## Cómo ejecutarlas

Requieren PostgreSQL con el esquema de `docs/db` y un `SIGRA/.env` válido. No se ejecutan con `./gradlew test`.

```bash
cd SIGRA
./gradlew integrationTest --tests '*AsignaturaIntegracionIT*'   # solo estas
./gradlew integrationTest                                       # todas las de integración
```

## Garantía de limpieza

Todos los datos que crea la prueba llevan el prefijo **ITG**: códigos de asignatura `ITG-...`, nombres `ITG ...`, correos `it-...@uco.net.co`, programas `ITG Programa ...`, tipo de documento `ITG Tipo` y semestres `ITG-S...`. Un `@AfterEach` (que también corre al inicio de cada caso, por si una corrida anterior se interrumpió) borra **solo** lo que cumple ese prefijo, en orden seguro: matrículas, asignaciones docentes, RA, asignaturas, programas, usuarios, semestres y tipo de documento. Nunca toca datos ajenos.

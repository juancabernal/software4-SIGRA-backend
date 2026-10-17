# Pruebas de integración — Nivel de logro y estadísticas (RF-14 / RF-15)

Clase: `SIGRA/src/test/java/co/edu/uco/sigra/estadisticas/integracion/NivelLogroIntegracionIT.java`

Levantan el contexto completo de Spring Boot contra **PostgreSQL real** y llaman a la API con `MockMvc`. Cada caso verifica la **respuesta HTTP** y las **filas reales** en `sigra.nivel_logro_estudiante` y `sigra.conclusion`. No usan `@Transactional`: el servicio confirma de verdad.

**Calificaciones de prueba.** Los módulos de evaluaciones y calificaciones todavía no existen, así que las notas se cargan en `sigra.registro_calificacion_prueba` (nota 0.0–5.0 y porcentaje de la evaluación), que es la tabla que lee `FuenteCalificacionesPruebaImpl`.

**Seguridad.** Corren con la seguridad real del backend (JWT + `@PreAuthorize`) y tokens emitidos por `JwtService`; el alcance del PROFESOR lo aplica el servicio con el `UsuarioPrincipal` autenticado.

| ID | Requisito | Escenario | Datos de entrada | Resultado esperado (respuesta) | Resultado esperado (base) |
| --- | --- | --- | --- | --- | --- |
| IE-01 | RF-14/15: promedio ponderado | Recalcular un RA con dos estudiantes | Ana: 2.0 (20 %), 4.0 (30 %), 5.0 (50 %). Beto: 3.0 (10 %), 3.0 (10 %), 4.5 (20 %), porcentajes que no suman 100 | 200; `evaluados` = 2; `promedioNota` = 3.93 | Ana 4.10 → `ALTO`; Beto 3.75 → `MEDIO` (normalizado) |
| IE-02 | RF-14/15: umbral de 3 y estadísticas | GET estadísticas con 4 matriculados | Uno con 3 notas de 1.5, uno con 3 notas de 4.8, uno con solo 2 notas y uno sin notas | 200; `totalEstudiantes` 4, `evaluados` 2, `sinDatosSuficientes` 1, `sinEvaluar` 1; distribución en orden BAJO, MEDIO, ALTO, SOBRESALIENTE = 1, 0, 0, 1 | El de 2 notas tiene fila con `cantidad_calificaciones` = 2 y nivel y promedio en NULL |
| IE-03 | RF-14/15 → RF-16 | El recálculo dispara la conclusión automática | 5 estudiantes, 3 en Bajo o Medio (60 %); se recalcula dos veces | 1.ª: `conclusion.generada` = true, `porcentajeInsuficiente` = 60; 2.ª: `generada` = false | Exactamente 1 conclusión VIGENTE con el mensaje de RF-16 |
| IE-04 | Alcance por rol | Consultar y recalcular con distintos usuarios | Profesor asignado a la asignatura, profesor ajeno, estudiante y sin token | 200; 403 (GET y POST); 403; 401 | — |
| IE-05 | Validaciones | RA o semestre inexistentes | UUID aleatorios | 404 sin detalles técnicos (GET y POST) | — |

## Cómo ejecutarlas

Requieren PostgreSQL con el esquema de `docs/db/SIGRA_SCHEMA_CURRENT.sql` (incluye `nivel_logro_estudiante` y `registro_calificacion_prueba`), la migración `docs/db/RF08_matricula_fecha_y_estado_migration.sql` y un `SIGRA/.env` válido.

```bash
cd SIGRA
./gradlew integrationTest --tests '*NivelLogroIntegracionIT*'   # solo estas
```

## Garantía de limpieza

Todos los datos propios llevan el prefijo `ITE-` (asignaturas, semestres), `ITE Programa` o correos `ite-…@uco.net.co`. Un `@AfterEach`, que también corre al inicio de cada caso por si una corrida anterior se interrumpió, borra **solo** esas filas y sus dependientes (niveles, calificaciones de prueba, conclusiones, matrículas y asignaciones), en orden de claves foráneas.

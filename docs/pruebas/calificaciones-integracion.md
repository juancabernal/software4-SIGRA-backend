# Pruebas de integración — módulo Calificaciones (RF-12, RF-13)

Clase: `SIGRA/src/test/java/co/edu/uco/sigra/calificaciones/integracion/CalificacionIntegracionIT.java`

Levantan el contexto completo de Spring Boot contra **PostgreSQL real** y llaman a la API con `MockMvc`, seguridad real (filtro JWT y @PreAuthorize) y tokens JWT reales emitidos por `JwtService` para usuarios de la base. No usan `@Transactional`: el servicio confirma de verdad.

**Datos de referencia.** Cada caso crea por repositorio: tipo de documento `ITN Tipo`; un semestre que empieza ayer (así es el «semestre activo» que elige evaluaciones aunque la base tenga otro vigente) y uno anterior que no contiene hoy; un programa, la asignatura `ITN-101` y su RA; un profesor con asignación docente ACTIVA; las evaluaciones «Parcial 1» (fecha tentativa dentro de 10 días) y «Quiz 1» (fecha tentativa ayer); y las matrículas ACTIVAS de Ana y Beto.

| ID    | Capacidad                | Escenario                                                                                                                                                             | Resultado esperado (respuesta)                                                                                           | Resultado esperado (base)                                    |
|-------|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| IT-01 | Registrar y planilla     | Nota a tiempo y nota después de la fecha tentativa; planilla con una matrícula desvinculada sin nota, Consulta de las evaluaciones del RA en el semestre              | 201 con `fueraDeFecha` falso y 201 con `fueraDeFecha` verdadero; planilla con 2 filas ordenadas por nombre, 1 calificada | La calificación guarda nota, observación y la marca          |
| IT-02 | Reglas de registro       | Nota 5.5, observación de 5 caracteres, matrícula desvinculada, matrícula de otro semestre, evaluación inactiva, evaluación inexistente; luego nota válida y duplicada | 400 en las cinco reglas, 404 para la evaluación inexistente, 201 y luego 409                                             | Ninguna fila por los rechazos; una sola calificación de Ana  |
| IT-03 | Modificar y trazabilidad | `PUT` 3.5 → 4.0; `PUT` repetido con los mismos valores; `GET /historial`                                                                                              | 200 y 200; historial `REGISTRO`, `MODIFICACION` con nota y observación anteriores y nuevas, usuario y rol                | La nota queda en 4.0; el `PUT` sin cambios no agrega eventos |
| IT-04 | Seguridad                | Los cuatro endpoints sin token y con tokens de ESTUDIANTE, ADMINISTRADOR y de un PROFESOR sin asignación                                                              | 401 sin token, 403 en los demás, sin detalles técnicos                                                                   | Ninguna calificación nueva ni modificada                     |
| IT-05 | Matrícula desvinculada   | Con una nota registrada, la matrícula pasa a `INACTIVO`; planilla y `PUT` de la nota                                                                                  | La fila aparece con `matriculaActiva` falso y la nota se cuenta; el `PUT` da 400                                         | La nota se conserva sin cambios                              |
## Cómo ejecutarlas

Requieren PostgreSQL con el esquema de `docs/db` (en una base existente, aplicar antes `docs/db/RF12_calificacion_migration.sql`) y un `SIGRA/.env` válido.

```bash
cd SIGRA
./gradlew integrationTest --tests '*CalificacionIntegracionIT*'
```

## Garantía de limpieza

Todo lo que crea la prueba lleva el prefijo **ITN**: asignatura `ITN-...`, programa `ITN Programa ...`, semestres `ITN-S...`, correos `itn-...@uco.net.co` y tipo de documento `ITN Tipo`. `@AfterEach` (que también corre al inicio de cada caso) borra **solo** esos datos con SQL, en orden seguro: historial, calificaciones, matrículas, evaluaciones, asignaciones, RA, asignatura, programa, subtipos de usuario, usuarios, semestres y tipo de documento. Se usa SQL porque el repositorio del historial es de solo inserción y no expone borrado.
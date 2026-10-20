-- =============================================================================
-- SIGRA — Migración: matricula gana fecha_matricula y cambia el dominio de estado
-- Archivo: docs/db/RF08_matricula_fecha_y_estado_migration.sql
-- Motor: PostgreSQL 15+
--
-- Para quién: toda base que ya tenga sigra.matricula, venga de
-- docs/db/SIGRA_SCHEMA_CURRENT.sql o de docs/db/RF08_matricula_migration.sql
-- (los dos la dejan idéntica). Corresponde al cambio OpenSpec
-- `matricula-desvinculacion`, grupo 1.
--
-- Qué hace, y por qué es OBLIGATORIA y no opcional:
--   1. Añade matricula.fecha_matricula (DATE, NOT NULL). La entidad Matricula la
--      mapea `nullable = false`. Con ddl-auto=update Hibernate crea la columna,
--      pero NO rellena las filas que ya existen, así que la dejaría nula contra
--      un campo no nulo.
--   2. Cambia el dominio de matricula.estado de ('ACTIVO','INACTIVO') a
--      ('ACTIVA','DESVINCULADA'). El estado de una matrícula dejó de ser el
--      vocabulario compartido de «registro dado de baja»
--      (common.enums.EstadoRegistro) y pasó a ser el propio del módulo
--      (estudiantes.entity.EstadoMatricula): una matrícula desvinculada conserva
--      sus calificaciones vivas y es historial académico. Hibernate NO traduce
--      los datos y NO toca el CHECK, y con el CHECK viejo intacto el primer
--      INSERT con 'ACTIVA' falla.
--
-- CAMBIO INCOMPATIBLE: hay que correrla ANTES de arrancar el backend de este
-- commit, y el backend anterior no funciona contra el esquema migrado sin
-- aplicar el bloque de reversión del final.
--
-- Es IDEMPOTENTE: se puede ejecutar varias veces. No borra filas y no
-- sobreescribe una fecha_matricula ya poblada (ver el guardado IS NULL del
-- paso 2). Va entera en una transacción: si algo falla, no queda a medias.
--
-- Uso (puerto, usuario y base según tu máquina; en el entorno local la base es imexcol):
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d imexcol -v ON_ERROR_STOP=1 -f docs/db/RF08_matricula_fecha_y_estado_migration.sql
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- Paso 1 — Añadir la columna, todavía aceptando nulos.
-- Nullable en este punto a propósito: las filas que ya existen no tienen valor y
-- se rellenan en el paso 2. Ponerla NOT NULL aquí fallaría.
-- -----------------------------------------------------------------------------
ALTER TABLE sigra.matricula
    ADD COLUMN IF NOT EXISTS fecha_matricula DATE;

-- -----------------------------------------------------------------------------
-- Paso 2 — Rellenar las filas existentes con un valor determinista.
--
-- El valor es la fecha_inicio del semestre de cada matrícula, no un CURRENT_DATE
-- plano para todas: es la fecha más temprana en que ese estudiante pudo haber
-- quedado matriculado en ese periodo, se deriva del propio dato en lugar de
-- inventar un día arbitrario, y mantiene el orden relativo de un historial
-- académico repartido en varios semestres.
--
-- El `fecha_matricula IS NULL` es lo que hace idempotente este paso: en una
-- segunda ejecución no pisa las fechas reales que el backend ya haya escrito.
-- -----------------------------------------------------------------------------
UPDATE sigra.matricula m
   SET fecha_matricula = s.fecha_inicio
  FROM sigra.semestre s
 WHERE m.semestre_id = s.id
   AND m.fecha_matricula IS NULL;

-- Red de seguridad antes de exigir NOT NULL. Debería afectar 0 filas: el UPDATE
-- de arriba es un join por la FK fk_matricula_semestre y semestre.fecha_inicio
-- es NOT NULL, así que no hay matrícula que pueda quedarse sin fecha. Está
-- igualmente porque si quedara una sola fila nula el paso 3 abortaría la
-- migración entera.
UPDATE sigra.matricula
   SET fecha_matricula = CURRENT_DATE
 WHERE fecha_matricula IS NULL;

-- -----------------------------------------------------------------------------
-- Paso 3 — Ahora sí, no nula. Repetirlo sobre una columna que ya es NOT NULL no
-- da error: es un no-op.
-- -----------------------------------------------------------------------------
ALTER TABLE sigra.matricula
    ALTER COLUMN fecha_matricula SET NOT NULL;

-- -----------------------------------------------------------------------------
-- Paso 4 — Reemplazar chk_matricula_estado por el dominio nuevo.
--
-- ANTES de traducir los datos del paso 5, porque la traducción viola el CHECK
-- viejo: un UPDATE a 'ACTIVA' contra CHECK (estado IN ('ACTIVO','INACTIVO'))
-- falla.
--
-- Y el CHECK nuevo se añade NOT VALID, que es imprescindible y no un adorno:
-- ADD CONSTRAINT valida de inmediato las filas que ya están, y en este punto
-- siguen diciendo 'ACTIVO'/'INACTIVO', así que un ADD normal fallaría aquí.
-- NOT VALID omite esa comprobación retroactiva pero SÍ se aplica a toda
-- escritura posterior, incluido el UPDATE del paso 5 — que escribe los valores
-- nuevos y por tanto lo cumple. El paso 6 lo valida cuando ya no queda ninguna
-- fila con los valores viejos.
--
-- PostgreSQL no admite ADD CONSTRAINT IF NOT EXISTS; el DROP ... IF EXISTS
-- previo es lo que hace idempotente el par.
-- -----------------------------------------------------------------------------
ALTER TABLE sigra.matricula
    DROP CONSTRAINT IF EXISTS chk_matricula_estado;

ALTER TABLE sigra.matricula
    ADD CONSTRAINT chk_matricula_estado CHECK (estado IN ('ACTIVA', 'DESVINCULADA')) NOT VALID;

-- El DEFAULT de la columna pasa de 'ACTIVO' a 'ACTIVA'.
--
-- Si se dejara en 'ACTIVO', el CHECK nuevo lo volvería inservible: cualquier
-- INSERT que omitiera la columna fallaría por violar la restricción, y el
-- mensaje de error apuntaría al CHECK en lugar de al default obsoleto, que es
-- lo que de verdad estaría mal.
--
-- Se consideró quitar el DEFAULT por completo —la entidad ya inicializa el campo
-- en ACTIVA y Hibernate siempre envía la columna, así que la aplicación nunca lo
-- usa— y se descartó: el default sí lo usan los INSERT escritos a mano, que en
-- este proyecto son frecuentes (los seed de bruno/SIGRA/seed/ y los scripts de
-- docs/db/). Quitarlo los rompería sin ganar nada. 'ACTIVA' es además el mismo
-- valor inicial que declara la entidad, así que las dos fuentes concuerdan.
--
-- Un DEFAULT no se valida contra el CHECK en tiempo de ALTER, solo al insertar,
-- de modo que esta sentencia es válida en cualquier punto del script.
ALTER TABLE sigra.matricula
    ALTER COLUMN estado SET DEFAULT 'ACTIVA';

-- -----------------------------------------------------------------------------
-- Paso 5 — Traducir los datos. Idempotente: en una segunda ejecución ninguna
-- fila coincide y los dos UPDATE afectan 0 filas.
-- -----------------------------------------------------------------------------
UPDATE sigra.matricula
   SET estado = 'ACTIVA'
 WHERE estado = 'ACTIVO';

UPDATE sigra.matricula
   SET estado = 'DESVINCULADA'
 WHERE estado = 'INACTIVO';

-- -----------------------------------------------------------------------------
-- Paso 6 — Validar el CHECK ahora que ninguna fila tiene los valores viejos.
-- Deja la restricción como una restricción normal y no «no validada», de modo
-- que el esquema migrado sea indistinguible de uno creado desde cero. Sobre una
-- restricción ya válida es un no-op.
-- -----------------------------------------------------------------------------
ALTER TABLE sigra.matricula
    VALIDATE CONSTRAINT chk_matricula_estado;

COMMIT;

-- =============================================================================
-- VERIFICACIÓN — correr después del COMMIT y antes de arrancar la aplicación.
-- Las dos consultas deben devolver 0. Si alguna no da 0, la migración quedó a
-- medias y el backend va a fallar al leer o al escribir matrículas.
-- =============================================================================

-- 1) No debe quedar ninguna fila con el vocabulario viejo de estado:
-- SELECT count(*) AS filas_con_estado_viejo
--   FROM sigra.matricula
--  WHERE estado IN ('ACTIVO', 'INACTIVO');

-- 2) No debe quedar ninguna fila sin fecha de matrícula:
-- SELECT count(*) AS filas_sin_fecha_matricula
--   FROM sigra.matricula
--  WHERE fecha_matricula IS NULL;

-- Opcional, para ver el resultado de un vistazo (3 filas en el entorno local:
-- 2 que eran 'ACTIVO' y 1 que era 'INACTIVO'):
-- SELECT id, semestre_id, fecha_matricula, estado
--   FROM sigra.matricula
--  ORDER BY fecha_matricula;

-- =============================================================================
-- REVERSIÓN — descomentar y ejecutar SOLO si hay que volver al backend anterior.
-- Escrito de antemano para no tener que redactarlo con prisa durante un rollback.
-- Deja sigra.matricula tal como la dejaban SIGRA_SCHEMA_CURRENT.sql y
-- RF08_matricula_migration.sql, salvo la columna fecha_matricula (ver la nota).
-- =============================================================================

-- BEGIN;
--
-- -- Reponer el CHECK viejo, también NOT VALID y por el mismo motivo que en el
-- -- paso 4 pero al revés: aquí las filas todavía dicen 'ACTIVA'/'DESVINCULADA'.
-- ALTER TABLE sigra.matricula
--     DROP CONSTRAINT IF EXISTS chk_matricula_estado;
--
-- ALTER TABLE sigra.matricula
--     ADD CONSTRAINT chk_matricula_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')) NOT VALID;
--
-- ALTER TABLE sigra.matricula
--     ALTER COLUMN estado SET DEFAULT 'ACTIVO';
--
-- -- Traducción inversa.
-- UPDATE sigra.matricula SET estado = 'ACTIVO'   WHERE estado = 'ACTIVA';
-- UPDATE sigra.matricula SET estado = 'INACTIVO' WHERE estado = 'DESVINCULADA';
--
-- ALTER TABLE sigra.matricula
--     VALIDATE CONSTRAINT chk_matricula_estado;
--
-- -- fecha_matricula se puede quedar: el backend anterior no conoce la columna y
-- -- la ignora al leer. Pero hay que soltarle el NOT NULL, porque ese backend
-- -- tampoco la envía al insertar y, sin valor y sin default, un INSERT nuevo
-- -- violaría la restricción. Dejarla nullable conserva las fechas ya calculadas
-- -- para cuando se vuelva a aplicar esta migración.
-- ALTER TABLE sigra.matricula
--     ALTER COLUMN fecha_matricula DROP NOT NULL;
--
-- COMMIT;
--
-- -- Solo si se quiere borrar también el dato (irreversible: las fechas se pierden):
-- -- ALTER TABLE sigra.matricula DROP COLUMN IF EXISTS fecha_matricula;

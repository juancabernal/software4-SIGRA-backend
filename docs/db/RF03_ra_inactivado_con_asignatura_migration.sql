-- =============================================================================
-- SIGRA — Migración: resultado_aprendizaje.inactivado_con_asignatura (reactivar asignaturas)
-- Archivo: docs/db/RF03_ra_inactivado_con_asignatura_migration.sql
-- Motor: PostgreSQL 15+
--
-- Por qué: una asignatura INACTIVA ahora puede reactivarse (INACTIVA -> ACTIVA). Al inactivarla,
-- sus RA activos pasan a inactivos; para devolver a ACTIVO SOLO esos (y no los RA que alguien
-- inactivó a mano antes), cada RA recuerda si fue inactivado junto con su asignatura.
-- Con ddl-auto=validate, sin esta columna el backend NO arranca.
--
-- Es IDEMPOTENTE y no borra datos. Una base creada desde cero con SIGRA_SCHEMA_CURRENT.sql
-- actualizado NO la necesita.
--
-- Mejor esfuerzo para datos existentes: los RA inactivos de asignaturas que YA están INACTIVAS
-- se marcan como inactivados con la asignatura (antes no se podía distinguir). Si al reactivar
-- una de ellas los RA restaurados no suman entre 5 y 7, la reactivación se rechaza con un
-- mensaje claro y no cambia nada.
--
-- Uso (puerto, usuario y base según tu máquina):
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d <BASE> -v ON_ERROR_STOP=1 -f docs/db/RF03_ra_inactivado_con_asignatura_migration.sql
-- =============================================================================

BEGIN;

ALTER TABLE sigra.resultado_aprendizaje
    ADD COLUMN IF NOT EXISTS inactivado_con_asignatura BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE sigra.resultado_aprendizaje r
   SET inactivado_con_asignatura = TRUE
  FROM sigra.asignatura a
 WHERE a.id = r.asignatura_id
   AND a.estado = 'INACTIVA'
   AND r.estado = 'INACTIVO'
   AND r.inactivado_con_asignatura = FALSE;

COMMIT;

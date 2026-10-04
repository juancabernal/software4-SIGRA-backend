-- =============================================================================
-- Datos de prueba para la coleccion Bruno de RF-Semestre (semestres).
-- Es IDEMPOTENTE: borra los semestres de prueba y los vuelve a crear. Ejecutalo
-- ANTES de cada corrida completa de la coleccion, porque las pruebas crean el
-- semestre 2098-1 y extienden la fecha de fin de 2090-1.
--
-- Los semestres de prueba usan anios lejanos (1990, 2090, 2091, 2098) para no
-- cruzarse con semestres reales: la regla de negocio no permite rangos que se
-- crucen, asi que un semestre real en esos anios haria fallar el seed o las pruebas.
--
-- 1990-1 se inserta directamente porque la API no permite crear semestres con
-- fecha de inicio en el pasado; sirve para probar "no modificar un semestre que
-- ya termino".
--
-- Si mas adelante otras tablas referencian semestre (matricula, evaluaciones...),
-- agrega aqui el DELETE correspondiente ANTES de borrar semestre.
-- =============================================================================
BEGIN;

DELETE FROM semestre WHERE codigo IN ('1990-1', '2090-1', '2090-2', '2091-1', '2097-1', '2098-1', '2099-1');

INSERT INTO semestre (id, codigo, fecha_inicio, fecha_fin, estado) VALUES
  ('00000000-0000-4000-c000-000000000001', '1990-1', '1990-01-20', '1990-06-10', 'INACTIVO'),
  ('00000000-0000-4000-c000-000000000002', '2090-1', '2090-01-20', '2090-06-10', 'INACTIVO'),
  ('00000000-0000-4000-c000-000000000003', '2091-1', '2091-01-20', '2091-06-10', 'INACTIVO');

COMMIT;

-- Resumen
SELECT codigo, fecha_inicio, fecha_fin, estado
  FROM semestre
 WHERE codigo IN ('1990-1', '2090-1', '2091-1')
 ORDER BY fecha_inicio;

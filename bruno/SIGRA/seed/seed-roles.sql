-- =============================================================================
-- Datos de prueba: un Administrador y un Estudiante que pueden iniciar sesión (RF-04).
-- Complementa seed-auth.sql (que solo crea profesores). IDEMPOTENTE: borra y recrea
-- SOLO estas dos cuentas de prueba. Todas las referencias llevan el esquema sigra.
--
-- Contraseña de ambas: PasswordSeguro123*  (el mismo hash BCrypt que usa seed-auth.sql).
--   admin.bruno@uco.net.co       -> rol ADMINISTRADOR
--   estudiante.bruno@uco.net.co  -> rol ESTUDIANTE
--
-- Requisito: el esquema debe tener las tablas sigra.administrador y sigra.estudiante como
-- subtipos (SIGRA_SCHEMA_CURRENT.sql actualizado o RF04_administrador_y_estudiante_subtipos_migration.sql).
-- =============================================================================
BEGIN;

DELETE FROM sigra.administrador WHERE id = '00000000-0000-4000-e000-000000000010';
DELETE FROM sigra.estudiante    WHERE id = '00000000-0000-4000-e000-000000000011';
DELETE FROM sigra.usuario       WHERE id IN ('00000000-0000-4000-e000-000000000010',
                                             '00000000-0000-4000-e000-000000000011');

-- Cualquier tipo de documento sirve para estas cuentas; se crea uno si no existe ninguno.
INSERT INTO sigra.tipo_documento (id, nombre)
VALUES ('00000000-0000-4000-d000-000000000001', 'Cédula de Ciudadanía')
ON CONFLICT DO NOTHING;

DO $$
DECLARE
    v_tipo_doc_id UUID;
    v_hash VARCHAR := '$2a$12$0xeKtqgxyBKM4Ks0ZWgxJu0ooSpm0OFeZ7hILuTE4henvC4dtDmjW';
BEGIN
    SELECT COALESCE(
               (SELECT id FROM sigra.tipo_documento WHERE nombre = 'Cédula de Ciudadanía' LIMIT 1),
               (SELECT id FROM sigra.tipo_documento ORDER BY nombre LIMIT 1))
      INTO v_tipo_doc_id;

    INSERT INTO sigra.usuario (id, tipo_documento_id, nombre_completo, correo_institucional,
                               password_hash, estado, intentos_fallidos, fecha_bloqueo)
    VALUES ('00000000-0000-4000-e000-000000000010', v_tipo_doc_id, 'Administrador Bruno',
            'admin.bruno@uco.net.co', v_hash, 'ACTIVO', 0, NULL);
    INSERT INTO sigra.administrador (id) VALUES ('00000000-0000-4000-e000-000000000010');

    INSERT INTO sigra.usuario (id, tipo_documento_id, nombre_completo, correo_institucional,
                               password_hash, estado, intentos_fallidos, fecha_bloqueo)
    VALUES ('00000000-0000-4000-e000-000000000011', v_tipo_doc_id, 'Estudiante Bruno',
            'estudiante.bruno@uco.net.co', v_hash, 'ACTIVO', 0, NULL);
    INSERT INTO sigra.estudiante (id, numero_documento)
    VALUES ('00000000-0000-4000-e000-000000000011', '1122334455');
END $$;

COMMIT;

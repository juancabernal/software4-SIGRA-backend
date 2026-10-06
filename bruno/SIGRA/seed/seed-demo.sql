-- =============================================================================
-- Datos de DEMOSTRACIÓN para el catálogo académico (RF-03): programas, asignaturas y RA
-- con casos realistas para probar la pantalla Materias a mano. NO es para Bruno (Bruno usa
-- seed-asignaturas.sql, con códigos BRU...); no se mezclan: este script no toca esos datos.
--
-- Qué deja (13 asignaturas, 4 programas):
--   ACTIVAS (5 a 7 RA activos): MAT-301, ALG-201, EST-401, FIS-102, PRG-101 (7 activos y 1 inactivo)
--   BORRADOR listas para activar (5 a 7 RA): EST-101, BDA-301, RES-201
--   BORRADOR que NO se pueden activar: ARQ-401 (3 RA), CAL-202 (0 RA), MKT-301 (8 RA, más de 7)
--   INACTIVAS que se pueden reactivar: CON-101 (5 RA), TOP-201 (6 RA)
--   Un programa inactivo (TEC-SIS) para ver la marca "(inactivo)" en los filtros.
--
-- IDEMPOTENTE: borra y recrea SOLO las filas de este script (identificadas por su id fijo)
-- y deja intactos tus otros datos. Si ya registraste a mano una asignatura con alguno de
-- estos códigos (por ejemplo MAT-301) o un programa con alguno de estos nombres, el script se
-- detiene con un error de duplicado y no cambia nada: renómbrala o bórrala antes.
-- Requiere el esquema con la columna resultado_aprendizaje.inactivado_con_asignatura.
-- Contraseñas: este script no crea usuarios (usa seed-auth.sql y seed-roles.sql).
--
-- Uso (puerto y base según tu .env):
--   psql -U postgres -h localhost -p <PUERTO> -d <BASE> -v ON_ERROR_STOP=1 -f bruno/SIGRA/seed/seed-demo.sql
-- =============================================================================
SET client_encoding TO 'UTF8';
BEGIN;

DELETE FROM sigra.resultado_aprendizaje WHERE asignatura_id::text LIKE '00000000-0000-4000-b0d0-%';
DELETE FROM sigra.asignatura            WHERE id::text             LIKE '00000000-0000-4000-b0d0-%';
DELETE FROM sigra.programa_academico    WHERE id::text             LIKE '00000000-0000-4000-a0d0-%';

INSERT INTO sigra.programa_academico (id, codigo, nombre, estado) VALUES
  ('00000000-0000-4000-a0d0-000000000001', 'ING-SIS', 'Ingeniería de Sistemas', 'ACTIVO'),
  ('00000000-0000-4000-a0d0-000000000002', 'ING-CIV', 'Ingeniería Civil', 'ACTIVO'),
  ('00000000-0000-4000-a0d0-000000000003', 'ADM-EMP', 'Administración de Empresas', 'ACTIVO'),
  ('00000000-0000-4000-a0d0-000000000004', 'TEC-SIS', 'Tecnología en Sistemas', 'INACTIVO');

INSERT INTO sigra.asignatura (id, codigo, nombre, programa_id, estado) VALUES
  ('00000000-0000-4000-b0d0-000000000001', 'MAT-301', 'Cálculo Diferencial', '00000000-0000-4000-a0d0-000000000001', 'ACTIVA'),
  ('00000000-0000-4000-b0d0-000000000002', 'ALG-201', 'Álgebra Lineal', '00000000-0000-4000-a0d0-000000000001', 'ACTIVA'),
  ('00000000-0000-4000-b0d0-000000000003', 'EST-401', 'Estadística Inferencial', '00000000-0000-4000-a0d0-000000000003', 'ACTIVA'),
  ('00000000-0000-4000-b0d0-000000000004', 'FIS-102', 'Física Mecánica', '00000000-0000-4000-a0d0-000000000002', 'ACTIVA'),
  ('00000000-0000-4000-b0d0-000000000005', 'PRG-101', 'Programación I', '00000000-0000-4000-a0d0-000000000001', 'ACTIVA'),
  ('00000000-0000-4000-b0d0-000000000006', 'EST-101', 'Estadística Básica', '00000000-0000-4000-a0d0-000000000003', 'BORRADOR'),
  ('00000000-0000-4000-b0d0-000000000007', 'BDA-301', 'Bases de Datos', '00000000-0000-4000-a0d0-000000000001', 'BORRADOR'),
  ('00000000-0000-4000-b0d0-000000000008', 'RES-201', 'Resistencia de Materiales', '00000000-0000-4000-a0d0-000000000002', 'BORRADOR'),
  ('00000000-0000-4000-b0d0-000000000009', 'ARQ-401', 'Arquitectura de Software', '00000000-0000-4000-a0d0-000000000001', 'BORRADOR'),
  ('00000000-0000-4000-b0d0-000000000010', 'CAL-202', 'Cálculo Integral', '00000000-0000-4000-a0d0-000000000001', 'BORRADOR'),
  ('00000000-0000-4000-b0d0-000000000011', 'MKT-301', 'Mercadeo Estratégico', '00000000-0000-4000-a0d0-000000000003', 'BORRADOR'),
  ('00000000-0000-4000-b0d0-000000000012', 'CON-101', 'Contabilidad General', '00000000-0000-4000-a0d0-000000000003', 'INACTIVA'),
  ('00000000-0000-4000-b0d0-000000000013', 'TOP-201', 'Topografía', '00000000-0000-4000-a0d0-000000000002', 'INACTIVA');

-- Resultados de aprendizaje. Los RA de las asignaturas INACTIVAS quedan marcados como inactivados
-- junto con la asignatura (inactivado_con_asignatura = true): son los que se restauran al reactivarla.
INSERT INTO sigra.resultado_aprendizaje (id, asignatura_id, codigo, descripcion, estado, inactivado_con_asignatura) VALUES
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000001', 'RA-01', 'Calcula límites y analiza la continuidad de funciones reales de una variable.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000001', 'RA-02', 'Deriva funciones algebraicas y trascendentes aplicando las reglas de derivación.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000001', 'RA-03', 'Aplica la derivada al análisis de la variación de funciones y a problemas de optimización.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000001', 'RA-04', 'Interpreta geométrica y físicamente la derivada en situaciones de ingeniería.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000001', 'RA-05', 'Utiliza software matemático para verificar resultados de cálculo diferencial.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000002', 'RA-01', 'Resuelve sistemas de ecuaciones lineales mediante eliminación gaussiana.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000002', 'RA-02', 'Opera con matrices y calcula determinantes e inversas.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000002', 'RA-03', 'Analiza espacios vectoriales, subespacios, bases y dimensión.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000002', 'RA-04', 'Aplica transformaciones lineales y su representación matricial.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000002', 'RA-05', 'Calcula valores y vectores propios y los usa en la diagonalización.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000002', 'RA-06', 'Modela problemas de ingeniería con herramientas del álgebra lineal.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000003', 'RA-01', 'Estima parámetros poblacionales mediante intervalos de confianza.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000003', 'RA-02', 'Formula y contrasta hipótesis estadísticas con pruebas paramétricas.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000003', 'RA-03', 'Aplica pruebas no paramétricas cuando no se cumplen los supuestos.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000003', 'RA-04', 'Construye e interpreta modelos de regresión lineal simple y múltiple.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000003', 'RA-05', 'Realiza análisis de varianza para comparar varios grupos.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000003', 'RA-06', 'Comunica los resultados de un análisis estadístico con claridad y rigor.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000004', 'RA-01', 'Describe el movimiento de partículas en una y dos dimensiones.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000004', 'RA-02', 'Aplica las leyes de Newton a problemas de dinámica.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000004', 'RA-03', 'Utiliza los conceptos de trabajo y energía para resolver problemas.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000004', 'RA-04', 'Analiza choques con la conservación del momento lineal.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000004', 'RA-05', 'Resuelve problemas de equilibrio estático y de rotación de cuerpos rígidos.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000005', 'RA-01', 'Analiza un problema y lo descompone en pasos algorítmicos.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000005', 'RA-02', 'Construye programas con estructuras de control secuenciales, condicionales y repetitivas.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000005', 'RA-03', 'Define y utiliza funciones para modularizar el código.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000005', 'RA-04', 'Maneja arreglos y colecciones de datos en la solución de problemas.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000005', 'RA-05', 'Aplica buenas prácticas de nomenclatura, estilo y documentación del código.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000005', 'RA-06', 'Depura y prueba programas para verificar su correcto funcionamiento.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000005', 'RA-07', 'Utiliza control de versiones para gestionar el código fuente.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000005', 'RA-08', 'Usa recursividad en problemas sencillos.', 'INACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000006', 'RA-01', 'Organiza y resume datos con tablas de frecuencia y medidas descriptivas.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000006', 'RA-02', 'Representa datos mediante gráficos estadísticos adecuados.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000006', 'RA-03', 'Calcula probabilidades básicas de eventos simples y compuestos.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000006', 'RA-04', 'Aplica distribuciones de probabilidad discretas y la distribución normal.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000006', 'RA-05', 'Interpreta resultados estadísticos en el contexto de la administración.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000007', 'RA-01', 'Diseña modelos entidad-relación a partir de requisitos del negocio.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000007', 'RA-02', 'Normaliza esquemas relacionales hasta la tercera forma normal.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000007', 'RA-03', 'Consulta y manipula datos con SQL.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000007', 'RA-04', 'Implementa restricciones de integridad y transacciones.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000007', 'RA-05', 'Optimiza consultas con el uso de índices.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000007', 'RA-06', 'Documenta el diseño físico de una base de datos.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000008', 'RA-01', 'Calcula esfuerzos y deformaciones en elementos sometidos a carga axial.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000008', 'RA-02', 'Analiza elementos sometidos a torsión.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000008', 'RA-03', 'Dibuja diagramas de fuerza cortante y momento flector.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000008', 'RA-04', 'Determina esfuerzos de flexión en vigas.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000008', 'RA-05', 'Calcula esfuerzos cortantes en vigas y secciones.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000008', 'RA-06', 'Evalúa la deflexión de vigas con métodos de integración.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000008', 'RA-07', 'Analiza el pandeo de columnas esbeltas.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000009', 'RA-01', 'Identifica los atributos de calidad que condicionan una arquitectura.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000009', 'RA-02', 'Selecciona patrones arquitectónicos según el contexto del sistema.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000009', 'RA-03', 'Documenta una arquitectura con vistas y diagramas.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000011', 'RA-01', 'Analiza el entorno y el mercado objetivo de una organización.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000011', 'RA-02', 'Segmenta mercados y define el posicionamiento de un producto.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000011', 'RA-03', 'Formula estrategias de producto, precio, plaza y promoción.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000011', 'RA-04', 'Diseña un plan de marketing con objetivos medibles.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000011', 'RA-05', 'Evalúa la competencia con herramientas de análisis estratégico.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000011', 'RA-06', 'Aplica técnicas de investigación de mercados.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000011', 'RA-07', 'Mide el desempeño de campañas con indicadores clave.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000011', 'RA-08', 'Propone estrategias de fidelización de clientes.', 'ACTIVO', false),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000012', 'RA-01', 'Reconoce los elementos de los estados financieros.', 'INACTIVO', true),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000012', 'RA-02', 'Registra operaciones en el libro diario y el libro mayor.', 'INACTIVO', true),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000012', 'RA-03', 'Elabora el balance de prueba.', 'INACTIVO', true),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000012', 'RA-04', 'Prepara ajustes y estados financieros básicos.', 'INACTIVO', true),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000012', 'RA-05', 'Aplica la normativa contable vigente en la elaboración de informes.', 'INACTIVO', true),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000013', 'RA-01', 'Realiza mediciones de distancias y ángulos en campo.', 'INACTIVO', true),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000013', 'RA-02', 'Calcula coordenadas y áreas de un terreno.', 'INACTIVO', true),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000013', 'RA-03', 'Elabora planos topográficos a escala.', 'INACTIVO', true),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000013', 'RA-04', 'Utiliza equipos de medición como estación total y nivel.', 'INACTIVO', true),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000013', 'RA-05', 'Realiza nivelaciones geométricas y trigonométricas.', 'INACTIVO', true),
  (gen_random_uuid(), '00000000-0000-4000-b0d0-000000000013', 'RA-06', 'Interpreta curvas de nivel y perfiles del terreno.', 'INACTIVO', true);

COMMIT;

-- Resumen
SELECT a.codigo, a.nombre, p.nombre AS programa, a.estado,
       COUNT(r.id) FILTER (WHERE r.estado = 'ACTIVO')   AS ra_activos,
       COUNT(r.id) FILTER (WHERE r.estado = 'INACTIVO') AS ra_inactivos
  FROM sigra.asignatura a
  JOIN sigra.programa_academico p ON p.id = a.programa_id
  LEFT JOIN sigra.resultado_aprendizaje r ON r.asignatura_id = a.id
 WHERE a.id::text LIKE '00000000-0000-4000-b0d0-%'
 GROUP BY a.codigo, a.nombre, p.nombre, a.estado
 ORDER BY a.estado, a.codigo;

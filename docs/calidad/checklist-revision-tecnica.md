# Checklist de revisión técnica (filtros F2 y F3)

Lo usa el revisor técnico en cada Pull Request. Los umbrales marcados como *(propuesta)* no vienen del manual operativo: hay que acordarlos con el equipo.

## 1. Antes de leer el código

Si algo de esta lista falla, el PR se devuelve **sin revisar el código** y la tarjeta pasa a «En correcciones».

- [ ] El PR enlaza un Issue o requerimiento (`Closes #n`) y la descripción está completa.
- [ ] La rama es solo para este cambio y apunta a la rama de integración correcta.
- [ ] El CI está en verde (compilación y pruebas unitarias).
- [ ] Hay evidencia de pruebas unitarias en el PR.
- [ ] El tamaño es revisable: hasta unas 400 líneas cambiadas y sin mezclar temas *(propuesta)*.
- [ ] No hay archivos que no pertenecen al cambio (`.env`, `build/`, archivos del IDE).

## 2. Revisión del contenido

Marca ✔ (cumple), ✘ (incumple, con observación) o N/A.

| # | Criterio | Qué miro en SIGRA |
| --- | --- | --- |
| 1 | **Alcance y trazabilidad** | El PR hace lo que pide el Issue y nada más. Los commits mencionan el Issue. |
| 2 | **Diseño y arquitectura** | Respeta las capas controller → service → repository. Los controladores no contienen reglas de negocio. Las entidades no salen por la API: se usan DTO y mapper. No hay dependencias circulares entre módulos. |
| 3 | **Estándares y legibilidad** | Nombres claros y coherentes con el módulo. Métodos cortos con una sola responsabilidad. Sin código muerto o comentado, sin `System.out`, sin TODO sin Issue. |
| 4 | **Mantenibilidad** | Sin duplicación. Constantes en vez de textos o números «mágicos». Consultas eficientes: evitar `findAll()` seguido de un filtro en memoria y una consulta por cada elemento de una lista. `@Transactional` donde se modifican datos. |
| 5 | **Errores y excepciones** | Excepciones de dominio con su handler. Las respuestas de error usan el cuerpo estándar (`ErrorResponseBuilder`). Mensajes en español sin trazas, SQL ni nombres de clases (RNF-17). Códigos HTTP correctos (400, 401, 403, 404, 409). No se tragan excepciones. |
| 6 | **Seguridad técnica** | Todo endpoint nuevo lleva `@PreAuthorize` con el rol correcto según la matriz RF-05. Las entradas se validan con Jakarta Validation y `@Valid`. Sin SQL concatenado. Sin secretos ni credenciales. Nunca salen hashes ni contraseñas en respuestas o logs. Dependencias nuevas justificadas y con versión fija. |
| 7 | **Pruebas unitarias** | Cubren el camino feliz, los errores y los límites. Una prueba falla si se revierte el cambio. Sin `@Disabled` sin Issue. No dependen de datos de la máquina de quien la escribió. |
| 8 | **Base de datos** | Los cambios de esquema traen script en `docs/db` y actualizan `SIGRA_SCHEMA_CURRENT.sql`. El esquema no se cambia con `ddl-auto`. |
| 9 | **Documentación** | Swagger/OpenAPI, README o `docs/` actualizados. Reglas de negocio no obvias explicadas en un comentario. |
| 10 | **Requisitos no funcionales** | Si el cambio toca seguridad, rendimiento, disponibilidad, compatibilidad o mantenibilidad, consulta al especialista RNF antes de aprobar. |
| 11 | **Uso de IA** | Si hubo IA, el PR referencia el registro. El autor entiende y probó lo que incorporó. |

## 3. Decisión

Clasifica cada observación:

- **[bloqueante]** debe corregirse antes de integrar.
- **[sugerencia]** mejora opcional; no impide aprobar.
- **[pregunta]** necesito entender una decisión.

| Decisión | Cuándo |
| --- | --- |
| **Aprobar** | Cumple todo y no hay bloqueantes. Las sugerencias pueden quedar como comentarios. |
| **Solicitar cambios** | Hay al menos un bloqueante. La tarjeta pasa a «En correcciones». |

Todo rechazo usa este formato (el manual exige criterio, evidencia y corrección):

```
Criterio incumplido: <cuál de la tabla y por qué importa>
Evidencia: <archivo y línea, resultado del CI o de una prueba>
Corrección requerida: <qué debe cambiar para aprobarse>
```

Al aprobar: integrar tú mismo (nadie más), mover la tarjeta a «En pruebas» y anotar la revisión en el registro.

## 4. Reglas de la revisión

- No se aprueba con el CI en rojo.
- El CI complementa tu revisión, no la reemplaza.
- Si el autor sube cambios después de tu revisión, vuelves a revisar (la aprobación anterior se descarta).
- Tiempo de respuesta acordado con el equipo: *(propuesta)* 24 horas.
- Dentro de las 24 horas, revisa primero los PR más antiguos y los que bloquean a otros.
- Si el defecto es de comportamiento y no de calidad de código, se registra como error (BUG-xx) en vez de rechazar por eso.

## 5. Plantilla de nota de diseño (filtro F2)

Un cambio relevante necesita esta nota antes del código; la revisan el revisor técnico y el especialista RNF.

```
Issue / requerimiento:
Tipo de mantenimiento:
Responsable:

Problema o necesidad:
Solución propuesta (qué cambia, en qué módulos):
Alternativas consideradas y por qué se descartaron:
Impacto en el producto existente (módulos, datos, API, pruebas):
Riesgos y cómo se mitigan:
Requisitos no funcionales afectados y cómo se verificarán:
Cómo se probará (unitarias, integración, funcionales):

Revisión técnica: Aprobado / Requiere ajustes   Fecha:
Revisión RNF:      Aprobado / Requiere ajustes   Fecha:
```

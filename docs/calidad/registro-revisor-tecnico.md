# Registros del revisor técnico

Plantillas para llevar tus registros. Los campos de las secciones 2, 3 y 4 son los del manual operativo (sección 9); la sección 1 es una bitácora propia para sustentar el indicador de Pull Requests aprobados en primera revisión.

> Las filas de ejemplo son solo para mostrar el formato: bórralas y llena con datos reales. El manual pide que los indicadores reflejen resultados reales.

## 1. Bitácora de revisiones de Pull Requests

Una fila por cada ronda de revisión (un PR con dos rondas ocupa dos filas).

| Fecha | PR | Issue | Autor | Ronda | Resultado | Criterio incumplido / observaciones | Tiempo (h) | Enlace a la review |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| EJEMPLO | #12 | #14 | Nombre del autor | 1 | Cambios solicitados | Criterio 5: el mensaje de error expone el nombre de una clase | 0,5 | (enlace) |
| EJEMPLO | #12 | #14 | Nombre del autor | 2 | Aprobado | Sin observaciones | 0,25 | (enlace) |

**Indicador «% de PR aprobados en primera revisión»** = PR cuya **ronda 1** terminó en *Aprobado* ÷ PR que ya se cerraron (aprobados e integrados), expresado en porcentaje. No cuentes los PR que siguen abiertos.

## 2. Registro de actividades

Una fila por actividad, todos los días. La evidencia es el enlace a la review, al PR o al documento.

| Fecha | Integrante | ID de actividad | Actividad | Tipo | Tiempo | Resultado | Evidencia | Estado |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| EJEMPLO | Tu nombre | ISSUE-14 | Revisión técnica del PR #12 | Análisis | 1,5 horas | Review con observaciones | (enlace al PR) | Terminado |

El manual define estos tipos: análisis, diseño, desarrollo, pruebas o documentación. «Revisión» no aparece: pregúntale al Scrum Master cuál usar (lo razonable es «Análisis») y regístralo para ser consistente.

## 3. Registro de uso de IA

Se diligencia también cuando se descarta la respuesta. No pegues credenciales, secretos, datos personales ni código confidencial en herramientas no autorizadas.

| Campo | Qué registrar |
| --- | --- |
| Integrante y fecha | |
| Herramienta | |
| Propósito | |
| Prompt | Completo o el extracto pertinente |
| Resultado | Resumen o referencia |
| Aplicación | Qué se incorporó (archivo, código o decisión) |
| Verificación | Cómo lo comprobaste (prueba, documentación o revisión) |
| Errores | Inconsistencias que encontraste |
| Decisión | Aceptado, modificado o descartado, con justificación |

## 4. Registro de errores

Para defectos que detectes en código ya integrado o durante la revisión.

| Campo | Qué registrar |
| --- | --- |
| ID y fecha | BUG-xx y fecha de detección |
| Reportado por | Revisor técnico |
| Funcionalidad | Componente afectado |
| Descripción | Qué ocurrió y qué se esperaba |
| Severidad | Crítica, alta, media o baja |
| Responsable | Quién corrige |
| Causa raíz | Origen identificado |
| Solución | Corrección aplicada |
| Evidencia | Resultado de la nueva prueba |
| Estado | Abierto, corregido, en verificación o cerrado |

Recuerda: los defectos críticos y altos bloquean la entrega; los medios y bajos necesitan una decisión de tratamiento antes del cierre.

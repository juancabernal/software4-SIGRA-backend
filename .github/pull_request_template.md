## Descripción
<!-- Describe brevemente los cambios realizados en este Pull Request. -->

## Tipo de Cambio
- [ ] Nueva funcionalidad (cambio que añade un endpoint o módulo)
- [ ] Corrección de errores (cambio que resuelve un bug)
- [ ] Refactorización (mejora de código sin alterar la funcionalidad)
- [ ] Documentación OpenAPI / Swagger

## Checklist de Code Review (Obligatorio antes de fusionar a desarrollo)
- [ ] **Arquitectura y Diseño:** Se respeta la Arquitectura Adoptada y la separación limpia de capas (Controlador, Servicio, Repositorio, DTOs).
- [ ] **Documentación OpenAPI/Swagger:** Los nuevos endpoints cuentan con sus anotaciones de seguridad (`@SecurityRequirement`) y códigos de respuesta documentados (`200`, `400`, `401`, `403`, `404` según aplique).
- [ ] **Seguridad y Permisos:** Los endpoints protegidos incluyen las anotaciones de autorización correctas (`@PreAuthorize`).
- [ ] **Pruebas y Cobertura ($\ge 70\%$):** Las pruebas unitarias y de integración pasan satisfactoriamente, y el pipeline de CI verifica el umbral de cobertura con JaCoCo.
- [ ] **Calidad de Código:** No hay código comentado innecesario, se usan nombres descriptivos y se siguen las convenciones del proyecto (Clean Code).
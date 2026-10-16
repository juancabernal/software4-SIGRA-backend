# Contrato de autenticación para Angular

## Login

POST /api/v1/auth/login recibe:

    {
      "correoInstitucional": "usuario@uco.net.co",
      "contrasena": "<contraseña>"
    }

Restricciones: `correoInstitucional` máximo 254 caracteres; `contrasena` máximo 72 bytes UTF-8. El backend rechaza el exceso, no lo trunca. El cuerpo real de `POST /api/v1/auth/login` tiene un límite configurable de 16 KiB (se verifica aunque `Content-Length` falte o el transporte sea chunked); las cargas académicas no usan este límite.

Respuesta 200: token, tipo Bearer, expiraEn en segundos y usuario con id, nombreCompleto, correoInstitucional y rol.

Enviar en cada ruta privada: Authorization: Bearer <token>. No enviar JWT en query string ni confiar en un UUID enviado por Angular para representar al usuario actual.

## Errores

- 400: DTO ausente, vacío, tipos JSON inesperados o correo inválido.
- 401: token ausente, inválido, expirado, manipulado, usuario inactivo o identidad inconsistente.
- 403: token válido pero rol o alcance insuficiente.
- 404: recurso inexistente, con cuerpo de error estándar y sin detalles técnicos.
- 413: cuerpo real del login por encima del límite configurado.
- 500: fallo inesperado del servidor; el cuerpo es genérico y no incluye detalles técnicos.
- 503: indisponibilidad temporal del repositorio durante autenticación; el mensaje es genérico y no contiene clases, SQL ni credenciales.
- 401: el quinto intento con contraseña incorrecta también se rechaza como credencial inválida y deja el bloqueo de 15 minutos activo.
- 423: los intentos posteriores durante esos 15 minutos se rechazan como usuario temporalmente bloqueado; la contraseña correcta tampoco permite entrar durante el bloqueo.

El frontend debe tratar expiraEn como duración máxima y retirar el token local al cerrar sesión o recibir 401. El logout del frontend no revoca el JWT en el servidor antes de su expiración.

Todas las respuestas de error incluyen `timestamp` con zona, `status`, `error` derivado del estado y `mensaje`; `detalles` solo aparece cuando hay errores de campo. El filtro de correlación se registra antes de CORS y el backend devuelve `X-Request-Id` también en 401/403/413/500/503 y en rechazos CORS; desde un origen permitido Angular puede leerlo porque CORS lo expone. No registrar ni reenviar ese identificador como secreto: no sustituye al JWT.

La revocación inmediata de JWT no existe: cerrar sesión en Angular elimina el token local, pero un token emitido sigue siendo válido hasta su expiración o hasta que cambie la identidad persistida. El bloqueo de login es por usuario; el rate limiting distribuido por IP/cuenta debe complementarse en gateway/WAF.

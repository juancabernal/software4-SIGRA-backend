# Ciclo de vida JWT

- Algoritmo único: HS256.
- Secreto externo JWT_SECRET, mínimo 32 bytes.
- Duración por defecto: JWT_EXPIRATION_MS=3600000, expiraEn=3600.
- Claims: sub UUID, correo, rol, iat y exp.
- Se validan firma, algoritmo, expiración, claims, UUID, rol permitido, usuario ACTIVO y coincidencia persistida de rol y correo.
- El contexto se limpia al comienzo de cada solicitud; la API es stateless.
- Cambiar rol, correo o estado invalida la autorización del token en la siguiente solicitud.
- No existe endpoint de revocación: logout elimina el token local; cambiar JWT_SECRET invalida tokens anteriores.

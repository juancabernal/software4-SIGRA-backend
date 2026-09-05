# TestLink para SIGRA — Guía de instalación con Docker

Esta carpeta contiene todo lo necesario para levantar **TestLink** (herramienta de diseño y gestión de casos de prueba) en tu propia máquina, usando Docker. Ya está resuelto todo lo que nos dio problemas la primera vez — sigue esta guía tal cual y no deberías tener ningún error.

## Requisitos previos

1. **Docker Desktop** instalado y **abierto** (verifica que el ícono de la ballena 🐳 en la barra de tareas esté fijo, no parpadeando).
   - Descarga: https://www.docker.com/products/docker-desktop/
2. Windows con **WSL2** habilitado (Docker Desktop te lo pide automáticamente en la instalación si no lo tienes).
3. Conexión a internet (se descargan imágenes y se clona código durante la construcción).

## Paso 1 — Clonar el repositorio y cambiar a la rama `testlink`

```bash
git clone https://github.com/juancabernal/software4-SIGRA.git
cd software4-SIGRA
git checkout testlink
cd testlink-sigra
```

## Paso 2 — Construir la imagen

```bash
docker-compose build --no-cache
```

⏱️ **Esto tarda entre 2 y 4 minutos.** Está construyendo TestLink desde su código fuente oficial (no una imagen ya armada), por eso toma un poco más que una descarga normal. Es normal, no se quedó pegado.

## Paso 3 — Levantar los contenedores

```bash
docker-compose up -d
```

Verifica que ambos contenedores quedaron corriendo:

```bash
docker ps
```

Deberías ver `testlink-sigra-mariadb-1` y `testlink-sigra-testlink-1`, ambos con estado `Up`.

⚠️ **Si el puerto 8090 ya está en uso en tu máquina** (por otro proyecto tuyo), verás un error de "port is already allocated". En ese caso, abre `docker-compose.yml`, busca la línea `- '8090:80'` y cambia el `8090` por otro número libre (ej. `8091`), luego repite `docker-compose up -d`.

## Paso 4 — Entrar al instalador web

Abre en tu navegador:

```
http://localhost:8090
```

Vas a ver la pantalla de instalación de TestLink. Sigue estos pasos **exactamente en este orden**:

1. Marca la casilla **"New installation"** → continúa.
2. En la pantalla de verificación de requisitos, todo debería salir en verde. Dale clic a continuar.
3. **Database Configuration** — completa así:
   ```
   Database Type: MySQL/MariaDB (5.6+ / 10.+)
   Database host: mariadb          ← NO pongas "localhost", debe ser "mariadb"
   Database name: testlink
   Table prefix: (déjalo vacío)
   Database admin login: root
   Database admin password: testlink
   ```
4. Más abajo, define el usuario propio de TestLink (no el root):
   ```
   Usuario: testlink
   Contraseña: testlink
   ```
5. Continúa — debería mostrarte un mensaje final: **"Installation was successful!"**

## Paso 5 — Iniciar sesión

```
Usuario: admin
Contraseña: admin
```

**Cambia esa contraseña de inmediato** desde tu perfil, una vez dentro.

## Errores que YA están resueltos en estos archivos (no deberías verlos)

Si por alguna razón te aparece alguno de estos, es señal de que estás usando una versión vieja de los archivos — asegúrate de tener el `Dockerfile` y `docker-compose.yml` más recientes de esta rama:

| Error | Causa | Ya resuelto en este archivo |
|---|---|---|
| `pull access denied for bitnami/testlink` | Bitnami eliminó su catálogo gratuito en 2025 | Construimos la imagen desde el código fuente oficial de GitHub, no dependemos de imágenes de terceros |
| `404 Not Found icu-devtools` | El repositorio de seguridad de Debian bullseye ya no está activo | El Dockerfile redirige apt a `archive.debian.org` y omite el repo de seguridad |
| `Unable to read current working directory` | Se borraba la carpeta donde la terminal estaba parada | El Dockerfile ya hace `cd /` antes de borrar `/var/www/html` |
| `/var/testlink/logs/ directory Failed` | TestLink espera esas carpetas en una ruta específica | El Dockerfile ya las crea con los permisos correctos |
| `templates_c directory is writable — Failed` | Permisos del código clonado | El Dockerfile ya aplica `chown` a todo `/var/www/html` |
| `Access denied for user 'testlink'@'IP...'` al hacer login | El instalador crea el usuario solo para `'localhost'`, pero en Docker cada contenedor tiene su propia IP | Ver sección "Si no puedes iniciar sesión" abajo |

### Si no puedes iniciar sesión (usuario/contraseña "incorrectos")

Corre esto una sola vez, después de instalar:

```bash
docker exec -it testlink-sigra-mariadb-1 mysql -uroot -ptestlink
```

Y dentro del prompt de MySQL:

```sql
CREATE USER 'testlink'@'%' IDENTIFIED BY 'testlink';
GRANT ALL PRIVILEGES ON testlink.* TO 'testlink'@'%';
FLUSH PRIVILEGES;
EXIT;
```

Luego recarga la página de login e intenta de nuevo.

## Comandos útiles

```bash
# Ver logs si algo falla
docker-compose logs -f testlink

# Detener sin borrar datos
docker-compose stop

# Volver a levantar
docker-compose start

# Apagar y eliminar contenedores (los datos en los volumes persisten)
docker-compose down

# Apagar y BORRAR TODO incluidos los datos (para empezar 100% de cero)
docker-compose down -v
```

## ¿Podemos usar todos la misma instancia de TestLink en vez de que cada uno tenga la suya?

**El repositorio de GitHub sea público no tiene nada que ver con esto** — es una confusión común. Que el código esté público en GitHub solo significa que cualquiera puede *ver los archivos*; no expone el contenedor de Docker que corre en la máquina de alguien. Cada quien que siga esta guía en su propia laptop tendrá su **propia instancia local**, separada de las demás, sin importar que el repo sea público.

Para que todo el equipo trabaje sobre **una sola instancia compartida**, hay dos caminos:

### Opción A — Si están en la misma red (mismo WiFi/salón)
Quien levante el contenedor comparte su **IP local** (se ve con `ipconfig` en Windows, busca "Dirección IPv4"), y los demás acceden a:
```
http://<IP-de-esa-máquina>:8090
```
en vez de `localhost`. Limitación: solo funciona mientras esa máquina esté prendida y conectada a esa red — no sirve para trabajar cada quien desde su casa en momentos distintos.

### Opción B — Instancia centralizada en la nube (recomendada si van a trabajar en distintos momentos/lugares)
Levantar estos mismos `Dockerfile` y `docker-compose.yml` en una **máquina virtual en la nube** (ej. Azure, dado que ya están en el ecosistema Microsoft y probablemente tengan créditos de estudiante) que esté prendida todo el tiempo. Ahí todos entrarían a la misma URL pública, sin depender de la laptop de nadie en particular.

**Recomendación práctica:** para esta fase (diseño de casos de prueba), no es estrictamente necesario compartir una sola instancia — cada quien puede diseñar sus casos en su instancia local siguiendo esta guía, y más adelante consolidar todo exportando/importando (TestLink permite exportar Test Suites a XML e importarlas en otra instancia) en la máquina de quien vaya a quedar como el repositorio "oficial" del equipo.

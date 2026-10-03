# Pruebas de la API con Bruno

Esta carpeta contiene la colección de pruebas de la API REST de SIGRA para [Bruno](https://www.usebruno.com), empezando por **RF-03 (Gestión de asignaturas)**.

## ¿Qué es Bruno y por qué lo usamos?

Bruno es un cliente de APIs, parecido a Postman, con una diferencia importante para un equipo: **cada petición es un archivo de texto (`.bru`) que vive en el repositorio**. Eso significa que:

- La colección se versiona en Git junto al código: se revisa en los Pull Request y se ve en el historial qué prueba cambió y por qué.
- No hace falta cuenta, inicio de sesión ni sincronización en la nube.
- Todo el equipo ejecuta exactamente las mismas pruebas, con los mismos datos.

## Contenido

```
bruno/
├── README.md                       Este archivo
└── SIGRA/                          La colección (ábrela desde Bruno)
    ├── bruno.json                  Definición de la colección
    ├── environments/local.bru      Variables del entorno local (URL e ids de prueba)
    ├── seed/seed-asignaturas.sql   Datos de prueba reproducibles
    └── asignaturas/                Pruebas de RF-03, en orden
        ├── 00-salud/               El backend responde (1)
        ├── 01-registrar/           RF-03a: registro y validaciones (14)
        ├── 02-consultar/           RF-03b: consulta con filtros (17)
        ├── 03-obtener-modificar/   RF-03c: consulta por id y modificación (8)
        ├── 04-activar/             RF-03d: activación con 5 a 7 RA activos (10)
        ├── 05-inactivar/           RF-03d: inactivación y cascada de RA (6)
        └── 06-cors/                CORS para el frontend en http://localhost:4200 (2)
```

Cada carpeta tiene un `folder.bru` con su `seq`, que fija el orden de ejecución, y cada petición lleva un prefijo numérico (`01-...`, `02-...`) con su `seq` dentro de la carpeta. **El orden importa**: algunas pruebas cambian el estado de las asignaturas de prueba (por ejemplo, `04-activar` deja `BRU05` en ACTIVA y `05-inactivar` la inactiva después).

## Instalación

1. Descarga e instala Bruno desde https://www.usebruno.com/downloads.
2. (Opcional) Para ejecutar la colección desde la terminal, instala el CLI:

   ```bash
   npm i -g @usebruno/cli
   ```

## Requisitos

- El backend corriendo en `http://localhost:8080` (ver el README de la raíz).
- PostgreSQL con la base de datos `sigra` creada. Las tablas las crea el backend al arrancar (`ddl-auto: update`), así que arranca el backend al menos una vez antes de cargar los datos de prueba.

## Cómo ejecutar las pruebas

### 1. Cargar los datos de prueba

Desde la **raíz del repositorio**, en PowerShell (ajusta la ruta de `psql.exe` a tu versión de PostgreSQL):

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -p 5432 -d sigra -f bruno/SIGRA/seed/seed-asignaturas.sql
```

El script crea dos programas (uno activo y uno inactivo), diez asignaturas con el prefijo `BRU` y sus resultados de aprendizaje, y al final muestra un resumen con los RA activos de cada una. Es **idempotente**: borra los datos `BRU` anteriores y los vuelve a crear.

> **Vuelve a ejecutar el seed antes de cada corrida completa de la colección.** Las pruebas cambian el estado de algunas asignaturas y, sin el seed, la segunda corrida fallaría (por ejemplo, `BRU05` ya no estaría en BORRADOR).

### 2. Arrancar el backend

```powershell
cd SIGRA
.\gradlew.bat bootRun
```

### 3. Ejecutar la colección

**Desde Bruno:**

1. *Open Collection* → selecciona la carpeta `bruno/SIGRA`.
2. Arriba a la derecha, elige el entorno **`local`**.
3. Clic derecho sobre la colección → **Run** → *Run Collection*.

**Desde la terminal** (con el CLI instalado), estando en `bruno/SIGRA`:

```bash
bru run . -r --env local
```

`-r` hace que se ejecuten también las subcarpetas.

## Qué se verifica

- Los códigos HTTP de cada caso (201, 200, 400, 404, 409).
- Los mensajes reales del backend (por ejemplo, «al menos 5» o «máximo 7» al activar).
- En todas las respuestas de error, que el cuerpo tenga `timestamp`, `status`, `error` y `mensaje`, y que **no** exponga trazas ni detalles técnicos (RNF-17).
- Que la consulta cuente solo los RA **activos** (la asignatura `BRU4I` tiene 4 activos y 3 inactivos, y cuenta 4).
- Que la búsqueda no distinga mayúsculas ni tildes (`programacion bruno` encuentra «Programación Bruno»).
- Que inactivar no borra la asignatura y deja sus RA activos en INACTIVO (cascada).
- Que CORS permite `http://localhost:4200` y rechaza otros orígenes. La segunda prueba de CORS no revisa el formato de error, porque ese 403 lo genera Spring antes de llegar al manejador de errores del módulo.

### Por qué no hay una prueba con JSON roto

Bruno no puede enviar un JSON sintácticamente inválido: si el cuerpo no es JSON válido, lo envía como una cadena de texto JSON. Por eso el caso de «cuerpo ilegible» se prueba con JSON **válido** que el backend no puede interpretar: un arreglo (`[1, 2, 3]`) en lugar de un objeto, y un `programaId` que no es un UUID. En ambos casos el backend responde 400 con «El cuerpo de la petición no es válido.».

### Comprobar la cascada en la base de datos

Después de ejecutar la colección, los RA de `BRU10` (inactivada en `05-inactivar`) deben haber quedado todos en INACTIVO:

```sql
SELECT codigo, estado FROM resultado_aprendizaje WHERE asignatura_id = '00000000-0000-4000-b000-000000000007';
```

## Cómo agregar las pruebas de otro módulo

1. Crea una carpeta bajo `bruno/SIGRA/` con el nombre del módulo (por ejemplo, `profesores/`) y su `folder.bru`:

   ```
   meta {
     name: Profesores (RF-01)
     seq: 2
   }
   ```

   El `seq` define en qué orden se ejecuta respecto a los demás módulos (`asignaturas` tiene `seq: 1`).
2. Dentro, organiza las peticiones en subcarpetas numeradas, cada una con su `folder.bru` y su `seq`, igual que en `asignaturas/`.
3. Si necesitas datos de prueba, agrega un script en `seed/` (idempotente y con un prefijo propio, como `BRU` aquí) y sus ids fijos en `environments/local.bru`.
4. Ejecuta `bru run . -r --env local` para comprobar que todo se lee y corre en orden.

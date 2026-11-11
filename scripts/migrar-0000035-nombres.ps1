<#
.SYNOPSIS
  Migración automática 0000035 (nombre partido en sigra.usuario).

.DESCRIPTION
  1) Lee credenciales de SIGRA/.env
  2) Si el repo tiene cambios locales, hace git stash
  3) Lista usuarios actuales y el split previsto (SQL)
  4) Aplica RF35_usuario_nombres_migration.sql (sin pasos manuales en el SQL Editor)
  5) Lista usuarios ya migrados (SQL)
  6) Levanta el backend, hace login + GET estudiantes/profesores y muestra nombres
  7) Apaga el backend y recupera el stash

.PARAMETER DryRun
  Solo lista/valida; NO ejecuta la migración ni toca datos.

.PARAMETER SkipApi
  Migra y verifica por SQL, pero no levanta el backend ni llama a la API.

.EXAMPLE
  pwsh -File scripts/migrar-0000035-nombres.ps1

.EXAMPLE
  pwsh -File scripts/migrar-0000035-nombres.ps1 -DryRun
#>
[CmdletBinding()]
param(
    [switch]$DryRun,
    [switch]$SkipApi
)

$ErrorActionPreference = 'Stop'

$BackendRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$SigraDir = Join-Path $BackendRoot 'SIGRA'
$EnvFile = Join-Path $SigraDir '.env'
$DocsDb = Join-Path $BackendRoot 'docs\db'
$SqlList = Join-Path $DocsDb 'RF35_usuario_nombres_listar_validacion.sql'
$SqlMigrate = Join-Path $DocsDb 'RF35_usuario_nombres_migration.sql'
$SqlPost = Join-Path $DocsDb 'RF35_usuario_nombres_post_listar.sql'

function Write-Step([string]$msg) {
    Write-Host ""
    Write-Host "=== $msg ===" -ForegroundColor Cyan
}

function Import-DotEnv([string]$path) {
    if (-not (Test-Path $path)) {
        throw "No existe $path. Copia .env.example a SIGRA/.env con la URL de Supabase."
    }
    $map = @{}
    Get-Content $path | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq '' -or $line.StartsWith('#')) { return }
        $i = $line.IndexOf('=')
        if ($i -lt 1) { return }
        $k = $line.Substring(0, $i).Trim()
        $v = $line.Substring($i + 1).Trim()
        $map[$k] = $v
    }
    return $map
}

function Find-Psql {
    $cmd = Get-Command psql -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $candidates = @(
        'C:\Program Files\PostgreSQL\18\bin\psql.exe',
        'C:\Program Files\PostgreSQL\17\bin\psql.exe',
        'C:\Program Files\PostgreSQL\16\bin\psql.exe',
        'C:\Program Files\PostgreSQL\15\bin\psql.exe'
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return $c }
    }
    throw "No se encontró psql. Instala PostgreSQL client o agrega psql al PATH."
}

function Parse-JdbcUrl([string]$jdbc) {
    # jdbc:postgresql://host:port/database?sslmode=require&currentSchema=sigra
    if ($jdbc -notmatch '^jdbc:postgresql://([^:/?]+):?(\d+)?/([^?]+)') {
        throw "DB_URL no reconocida: $jdbc"
    }
    return [pscustomobject]@{
        Host = $Matches[1]
        Port = if ($Matches[2]) { $Matches[2] } else { '5432' }
        Database = $Matches[3]
    }
}

function Invoke-PsqlFile {
    param(
        [string]$Psql,
        [hashtable]$EnvMap,
        [object]$Db,
        [string]$File,
        [string]$Label
    )
    if (-not (Test-Path $File)) { throw "Falta archivo SQL: $File" }
    Write-Host "Ejecutando $Label ..." -ForegroundColor Yellow
    $env:PGPASSWORD = $EnvMap['DB_PASSWORD']
    $env:PGSSLMODE = 'require'
    & $Psql -h $Db.Host -p $Db.Port -U $EnvMap['DB_USERNAME'] -d $Db.Database `
        -v ON_ERROR_STOP=1 -f $File
    if ($LASTEXITCODE -ne 0) {
        throw "psql falló ($Label) con código $LASTEXITCODE"
    }
}

function Invoke-PsqlQuery {
    param(
        [string]$Psql,
        [hashtable]$EnvMap,
        [object]$Db,
        [string]$Sql
    )
    $env:PGPASSWORD = $EnvMap['DB_PASSWORD']
    $env:PGSSLMODE = 'require'
    & $Psql -h $Db.Host -p $Db.Port -U $EnvMap['DB_USERNAME'] -d $Db.Database `
        -v ON_ERROR_STOP=1 -c $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "psql query falló con código $LASTEXITCODE"
    }
}

# --- Post-list SQL (siempre regenerado junto al script) ---
@"
-- Generado por migrar-0000035-nombres.ps1: listado post-migración
SELECT
    id,
    correo_institucional,
    primer_nombre,
    segundo_nombre,
    primer_apellido,
    segundo_apellido,
    concat_ws(' ', primer_nombre, segundo_nombre, primer_apellido, segundo_apellido) AS nombre_derivado,
    CASE
        WHEN segundo_nombre IS NOT NULL AND btrim(segundo_nombre) <> '' THEN 'nombre + segundo + 2 apellidos'
        ELSE 'nombre + 2 apellidos (sin segundo nombre)'
    END AS forma,
    (primer_nombre = 'S/D' OR primer_apellido = 'S/D' OR segundo_apellido = 'S/D') AS usa_sd
FROM sigra.usuario
ORDER BY usa_sd DESC, primer_apellido, primer_nombre;

SELECT
    count(*) AS total,
    count(*) FILTER (WHERE segundo_nombre IS NOT NULL AND btrim(segundo_nombre) <> '') AS con_segundo_nombre,
    count(*) FILTER (WHERE primer_nombre = 'S/D' OR primer_apellido = 'S/D' OR segundo_apellido = 'S/D') AS con_sd
FROM sigra.usuario;
"@ | Set-Content -Path $SqlPost -Encoding UTF8

Write-Step '0000035 — migración automática de nombres'
Write-Host "Backend root: $BackendRoot"
if ($DryRun) { Write-Host 'Modo DryRun: NO se aplicará la migración.' -ForegroundColor Yellow }

$envMap = Import-DotEnv $EnvFile
$db = Parse-JdbcUrl $envMap['DB_URL']
$psql = Find-Psql
Write-Host "DB host=$($db.Host) db=$($db.Database) user=$($envMap['DB_USERNAME'])"
Write-Host "psql: $psql"

# --- Stash si hay cambios locales ---
$stashed = $false
$stashMsg = "auto-stash 0000035 $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Push-Location $BackendRoot
try {
    $status = git status --porcelain 2>$null
    if ($status) {
        Write-Step 'Guardando cambios locales (git stash)'
        git stash push -u -m $stashMsg
        if ($LASTEXITCODE -ne 0) { throw 'git stash falló' }
        $stashed = $true
        Write-Host "Stash creado: $stashMsg"
    } else {
        Write-Host 'Working tree limpio: no hace falta stash.'
    }
} finally {
    Pop-Location
}

$backendProc = $null
try {
    Write-Step '1) Listado / validación PREVIA (cómo quedarían los datos)'
    Invoke-PsqlFile -Psql $psql -EnvMap $envMap -Db $db -File $SqlList -Label 'listar_validacion'

    if ($DryRun) {
        Write-Step 'DryRun terminado — no se migró nada'
        return
    }

    Write-Step '2) Aplicando migración RF35 (automática)'
    Invoke-PsqlFile -Psql $psql -EnvMap $envMap -Db $db -File $SqlMigrate -Label 'migration'

    Write-Step '3) Listado POST-migración (todos los usuarios)'
    Invoke-PsqlFile -Psql $psql -EnvMap $envMap -Db $db -File $SqlPost -Label 'post_listar'

    if ($SkipApi) {
        Write-Host 'SkipApi: no se levanta backend.' -ForegroundColor Yellow
        return
    }

    Write-Step '4) Levantando backend para validar API'
    $bootEnv = @{
        DB_URL = $envMap['DB_URL']
        DB_USERNAME = $envMap['DB_USERNAME']
        DB_PASSWORD = $envMap['DB_PASSWORD']
        JWT_SECRET = $envMap['JWT_SECRET']
        JWT_EXPIRATION_MS = $(if ($envMap['JWT_EXPIRATION_MS']) { $envMap['JWT_EXPIRATION_MS'] } else { '3600000' })
        SERVER_PORT = $(if ($envMap['SERVER_PORT']) { $envMap['SERVER_PORT'] } else { '8080' })
        JPA_DDL_AUTO = 'validate'
        JPA_SHOW_SQL = 'false'
        CORS_ALLOWED_ORIGINS = $(if ($envMap['CORS_ALLOWED_ORIGINS']) { $envMap['CORS_ALLOWED_ORIGINS'] } else { 'http://localhost:4200' })
    }
    foreach ($k in $bootEnv.Keys) { Set-Item -Path "Env:$k" -Value $bootEnv[$k] }

    $logFile = Join-Path $SigraDir 'migracion-0000035-backend.log'
    if (Test-Path $logFile) { Remove-Item $logFile -Force }
    $backendProc = Start-Process -FilePath '.\gradlew.bat' -ArgumentList @('bootRun', '--no-daemon') `
        -WorkingDirectory $SigraDir -PassThru -NoNewWindow `
        -RedirectStandardOutput $logFile -RedirectStandardError $logFile

    $port = $bootEnv['SERVER_PORT']
    $healthy = $false
    for ($i = 1; $i -le 90; $i++) {
        Start-Sleep -Seconds 2
        try {
            $h = Invoke-RestMethod -Uri "http://127.0.0.1:$port/actuator/health" -TimeoutSec 3
            if ($h.status -eq 'UP') { $healthy = $true; break }
        } catch {
            if ($backendProc.HasExited) {
                Get-Content $logFile -Tail 80
                throw "El backend terminó antes de quedar UP (exit=$($backendProc.ExitCode)). Ver $logFile"
            }
        }
    }
    if (-not $healthy) {
        Get-Content $logFile -Tail 80
        throw "Timeout esperando health en :$port"
    }
    Write-Host "Backend UP en :$port"

    Write-Step '5) Login + listado API (estudiantes y profesores)'
    $loginBody = @{
        correoInstitucional = 'admin.bruno@uco.net.co'
        contrasena = 'PasswordSeguro123*'
    } | ConvertTo-Json
    try {
        $login = Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:$port/api/v1/auth/login" `
            -ContentType 'application/json' -Body $loginBody
    } catch {
        Write-Host 'Login admin.bruno falló; ¿seeds cargados en Supabase? Se omite validación API.' -ForegroundColor Yellow
        Write-Host $_.Exception.Message
        return
    }
    $token = $login.token
    $headers = @{ Authorization = "Bearer $token" }

    $estudiantes = Invoke-RestMethod -Uri "http://127.0.0.1:$port/api/v1/estudiantes" -Headers $headers
    $profesores = Invoke-RestMethod -Uri "http://127.0.0.1:$port/api/v1/profesores" -Headers $headers

    Write-Host ""
    Write-Host "Estudiantes ($($estudiantes.Count)):" -ForegroundColor Green
    $estudiantes | ForEach-Object {
        [pscustomobject]@{
            primerNombre = $_.primerNombre
            segundoNombre = $_.segundoNombre
            primerApellido = $_.primerApellido
            segundoApellido = $_.segundoApellido
            nombreCompleto = $_.nombreCompleto
            correo = $_.correoInstitucional
        }
    } | Format-Table -AutoSize | Out-String | Write-Host

    Write-Host "Profesores ($($profesores.Count)):" -ForegroundColor Green
    $profesores | ForEach-Object {
        [pscustomobject]@{
            primerNombre = $_.primerNombre
            segundoNombre = $_.segundoNombre
            primerApellido = $_.primerApellido
            segundoApellido = $_.segundoApellido
            nombreCompleto = $_.nombreCompleto
            correo = $_.correoInstitucional
        }
    } | Format-Table -AutoSize | Out-String | Write-Host

    $malosEst = @($estudiantes | Where-Object {
        -not $_.primerNombre -or -not $_.primerApellido -or -not $_.segundoApellido
    })
    $malosProf = @($profesores | Where-Object {
        -not $_.primerNombre -or -not $_.primerApellido -or -not $_.segundoApellido
    })
    if ($malosEst.Count -gt 0 -or $malosProf.Count -gt 0) {
        throw "Validación API: faltan partes obligatorias (est=$($malosEst.Count) prof=$($malosProf.Count))"
    }

    $conSegundo = @($estudiantes + $profesores | Where-Object { $_.segundoNombre })
    Write-Host "OK API: todos tienen primerNombre + 2 apellidos." -ForegroundColor Green
    Write-Host "Con segundoNombre: $($conSegundo.Count)"

    Write-Step 'Migración 0000035 completada'
}
finally {
    if ($backendProc -and -not $backendProc.HasExited) {
        Write-Step 'Deteniendo backend'
        try { Stop-Process -Id $backendProc.Id -Force -ErrorAction SilentlyContinue } catch {}
        Get-Process -Name 'java' -ErrorAction SilentlyContinue |
            Where-Object { $_.Path -like '*SIGRA*' -or $_.CommandLine -like '*bootRun*' } |
            Stop-Process -Force -ErrorAction SilentlyContinue
    }
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue

    if ($stashed) {
        Write-Step 'Recuperando cambios del stash'
        Push-Location $BackendRoot
        try {
            git stash pop
            if ($LASTEXITCODE -ne 0) {
                Write-Host "stash pop tuvo conflictos o falló. Revisa: git stash list (mensaje: $stashMsg)" -ForegroundColor Yellow
            } else {
                Write-Host 'Stash recuperado.'
            }
        } finally {
            Pop-Location
        }
    }
}

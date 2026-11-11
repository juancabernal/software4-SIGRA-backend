# Lista nombre_completo actuales en Supabase (solo lectura). Lee SIGRA/.env.
$ErrorActionPreference = 'Stop'
$SigraDir = Resolve-Path (Join-Path $PSScriptRoot '..\SIGRA')
$EnvFile = Join-Path $SigraDir '.env'
$map = @{}
Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq '' -or $line.StartsWith('#')) { return }
    $i = $line.IndexOf('=')
    if ($i -lt 1) { return }
    $map[$line.Substring(0, $i).Trim()] = $line.Substring($i + 1).Trim()
}
if ($map['DB_URL'] -notmatch '^jdbc:postgresql://([^:/?]+):?(\d+)?/([^?]+)') {
    throw "DB_URL inválida"
}
$hostName = $Matches[1]
$port = if ($Matches[2]) { $Matches[2] } else { '5432' }
$dbName = $Matches[3]
$psqlCmd = Get-Command psql -ErrorAction SilentlyContinue
$psql = if ($psqlCmd) { $psqlCmd.Source } else { $null }
if (-not $psql) {
    foreach ($c in @(
        'C:\Program Files\PostgreSQL\18\bin\psql.exe',
        'C:\Program Files\PostgreSQL\17\bin\psql.exe',
        'C:\Program Files\PostgreSQL\16\bin\psql.exe'
    )) { if (Test-Path $c) { $psql = $c; break } }
}
if (-not $psql) { throw 'psql no encontrado' }

$sql = @'
SELECT
  correo_institucional,
  nombre_completo,
  cardinality(regexp_split_to_array(btrim(nombre_completo), '\s+')) AS palabras,
  regexp_split_to_array(btrim(nombre_completo), '\s+') AS tokens
FROM sigra.usuario
ORDER BY palabras, nombre_completo;

SELECT
  cardinality(regexp_split_to_array(btrim(nombre_completo), '\s+')) AS palabras,
  count(*) AS usuarios
FROM sigra.usuario
GROUP BY 1
ORDER BY 1;
'@

$env:PGPASSWORD = $map['DB_PASSWORD']
$env:PGSSLMODE = 'require'
& $psql -h $hostName -p $port -U $map['DB_USERNAME'] -d $dbName -v ON_ERROR_STOP=1 -c $sql
$code = $LASTEXITCODE
Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
exit $code

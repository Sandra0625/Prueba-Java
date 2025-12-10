<#
  Script: run-create-db.ps1
  Propósito: Ejecutar el SQL de creación de base de datos usando el cliente MySQL de XAMPP.
  Uso:
    .\run-create-db.ps1 -DbName bankinc_db -DbUser bankinc_user -DbPass 'secret'

  El script pedirá la contraseña de root si es necesaria.
#>

param(
    [string]$DbName = 'bankinc_db',
    [string]$DbUser = 'bankinc_user',
    [string]$DbPass = 'change_me_password'
)

$mysqlClient = 'C:\xampp\mysql\bin\mysql.exe'
$sqlFile = Join-Path $PSScriptRoot 'create-db.sql'

if (-not (Test-Path $mysqlClient)) {
    Write-Error "No se encontró el cliente MySQL en '$mysqlClient'. Asegúrate de que XAMPP está instalado en C:\xampp."
    exit 1
}

if (-not (Test-Path $sqlFile)) {
    Write-Error "No se encontró el archivo SQL '$sqlFile'."
    exit 1
}

# Pedir contraseña root (vacío si no tiene)
$rootPass = Read-Host 'Contraseña de root de MySQL (deja vacío si no tiene)' -AsSecureString
$plainRoot = ''
try {
    if ($rootPass.Length -gt 0) {
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($rootPass)
        $plainRoot = [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
} catch {
    $plainRoot = ''
}

# Reemplazar placeholders en el SQL y guardar temporal
$tmpSql = Join-Path $PSScriptRoot ("create-db-temp-{0}.sql" -f (Get-Date -Format yyyyMMddHHmmss))
Get-Content $sqlFile | ForEach-Object {
    $_ -replace 'bankinc_db', $DbName -replace 'bankinc_user', $DbUser -replace 'change_me_password', $DbPass
} | Set-Content $tmpSql

if ($plainRoot -eq '') {
    & $mysqlClient -u root < $tmpSql
} else {
    & $mysqlClient -u root -p$plainRoot < $tmpSql
}

Remove-Item $tmpSql -ErrorAction SilentlyContinue

Write-Host "Operación completada. Comprueba conectividad: mysql -u $DbUser -p -D $DbName" -ForegroundColor Green

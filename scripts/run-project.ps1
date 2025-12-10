<#
  Script: run-project.ps1
  Propósito: Construir y ejecutar la aplicación Spring Boot desde la raíz del proyecto.

  Uso ejemplo (sin privilegios):
    .\scripts\update-maven-path.ps1 -Scope User
    .\scripts\run-project.ps1 -DbUser tu_usuario -DbPass tu_contraseña

  Parámetros:
    -DbUser: nombre de usuario de la BD MySQL (opcional si ya está en application.properties)
    -DbPass: contraseña de la BD MySQL (opcional)
    -SkipTests: si se deben omitir los tests (default: $true)
#>

param(
    [string]$DbUser = $null,
    [string]$DbPass = $null,
    [bool]$SkipTests = $true
)

function Check-Command {
    param([string]$cmd)
    $c = Get-Command $cmd -ErrorAction SilentlyContinue
    return $null -ne $c
}

Write-Host "1) Verificando Java y Maven..."
java -version 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Warning "java no está en PATH o no responde. Verifica JAVA_HOME y PATH."
}

if (-not (Check-Command mvn)) {
    Write-Warning "mvn no encontrado en PATH. Ejecuta: .\scripts\update-maven-path.ps1 -Scope User  (o -Scope Machine como admin)"
    exit 1
}

Write-Host "2) Compilando el proyecto (Maven)..."
$skipArg = $SkipTests ? '-DskipTests=true' : ''
mvn $skipArg clean package
if ($LASTEXITCODE -ne 0) {
    Write-Error "La compilación falló. Revisa los errores y vuelve a intentar."
    exit 2
}

Write-Host "3) Ejecutando la aplicación..."
# Encontrar el jar generado
$jar = Get-ChildItem -Path target -Filter '*.jar' | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
if (-not $jar) {
    Write-Error "No se encontró el archivo JAR en target/. Comprueba la compilación"
    exit 3
}

$javaArgs = @()
if ($DbUser) { $javaArgs += "-Dspring.datasource.username=$DbUser" }
if ($DbPass) { $javaArgs += "-Dspring.datasource.password=$DbPass" }

Write-Host "Ejecutando: java $($javaArgs -join ' ') -jar $($jar.FullName)"
Start-Process -FilePath 'java' -ArgumentList @($javaArgs + "-jar", $jar.FullName) -NoNewWindow

Write-Host "La aplicación se está ejecutando (proceso lanzado). Revisa logs en la terminal o en target/" -ForegroundColor Green

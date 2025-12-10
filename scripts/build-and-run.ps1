<#
  scripts/build-and-run.ps1
  Ejecuta mvn clean package y arranca el JAR, guardando logs en el directorio del proyecto.
  Uso: abrir PowerShell en la raíz del proyecto y ejecutar:
    .\scripts\build-and-run.ps1

  Nota: este script debe ejecutarse en la máquina local del desarrollador. No lo ejecuto remotamente.
#>

param(
    [string]$MvnArgs = '-DskipTests=true clean package',
    [string]$JarName = 'prueba-0.0.1-SNAPSHOT.jar'
)

Set-Location -Path (Split-Path -Parent $MyInvocation.MyCommand.Definition)
Set-Location -Path ..

Write-Host "Ejecutando: mvn $MvnArgs"

# Compilar y guardar log (ejecutar mvn con argumentos separados para PowerShell)
$mvnCmd = 'mvn'
$mvnArgsArray = $MvnArgs -split ' '
& $mvnCmd @mvnArgsArray 2>&1 | Tee-Object -FilePath mvn-build.log
$mvnExit = $LASTEXITCODE

if ($mvnExit -ne 0) {
    Write-Error "Maven falló con código $mvnExit. Revisa 'mvn-build.log'. Mostrando últimas 120 líneas:"
    Get-Content .\mvn-build.log -Tail 120
    exit $mvnExit
}

Write-Host "Compilación exitosa. Empaquetando jar..."

$jarPath = Join-Path -Path (Join-Path -Path (Get-Location) -ChildPath 'target') -ChildPath $JarName
if (-not (Test-Path $jarPath)) {
    Write-Error "No se encontró el JAR en '$jarPath'. Lista del directorio target:"; Get-ChildItem -Path .\target
    exit 2
}

Write-Host "Arrancando $jarPath (logs en app-start.log)"

# Arrancar la aplicación y capturar salida (usa Tee-Object para registrar stdout+stderr)
Write-Host "Arrancando java -jar $jarPath (capturando salida en app-start.log)"
& java -jar $jarPath 2>&1 | Tee-Object -FilePath app-start.log

Write-Host "Ejecución finalizada. Revisa 'app-start.log' para la salida de la aplicación."

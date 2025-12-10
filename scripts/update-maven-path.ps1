<#
  Script: update-maven-path.ps1
  Propósito: Añadir MAVEN_HOME y la ruta bin de Maven al PATH.
  Uso:
    - Sin privilegios (usuario): ejecutar en PowerShell normal
        .\scripts\update-maven-path.ps1 -Scope User
    - Con privilegios (sistema): ejecutar PowerShell como Administrador
        .\scripts\update-maven-path.ps1 -Scope Machine

  Ajusta $MAVEN_HOME_DIR si tu instalación está en otra ruta.
#>

param(
    [ValidateSet('User','Machine')]
    [string]$Scope = 'User'
)

$MAVEN_HOME_DIR = 'C:\Program Files\Apache\maven-mvnd-1.0.3-windows-amd64\mvn'
$MAVEN_BIN = Join-Path $MAVEN_HOME_DIR 'bin'

if (-not (Test-Path $MAVEN_BIN)) {
    Write-Error "No se encontró '$MAVEN_BIN'. Ajusta la variable `\$MAVEN_HOME_DIR` en el script o comprueba la ruta de instalación."
    exit 1
}

function Add-ToMachinePath {
    param([string]$bin)
    $old = [Environment]::GetEnvironmentVariable('Path','Machine')
    if ($old -notlike "*${bin}*") {
        [Environment]::SetEnvironmentVariable('Path', $old + ';' + $bin, 'Machine')
        Write-Host "Agregada ruta al PATH de sistema: $bin"
    } else {
        Write-Host "La ruta ya existe en PATH de sistema: $bin"
    }
}

function Add-ToUserPath {
    param([string]$bin)
    $current = [Environment]::GetEnvironmentVariable('Path','User')
    if ($current -notlike "*${bin}*") {
        # setx actualiza la variable de usuario
        $new = "$bin;$current"
        setx Path $new | Out-Null
        Write-Host "Agregada ruta al PATH de usuario: $bin"
    } else {
        Write-Host "La ruta ya existe en PATH de usuario: $bin"
    }
}

if ($Scope -eq 'Machine') {
    $isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
    if (-not $isAdmin) {
        Write-Error "Se requieren privilegios de Administrador para modificar el PATH del sistema. Abre PowerShell como Administrador o usa -Scope User."
        exit 2
    }
    [Environment]::SetEnvironmentVariable('MAVEN_HOME', $MAVEN_HOME_DIR, 'Machine')
    Add-ToMachinePath -bin $MAVEN_BIN
    Write-Host "MAVEN_HOME establecido en nivel Machine: $MAVEN_HOME_DIR"
    Write-Host "Cierra y abre la terminal (o reinicia el sistema) para que los cambios surtan efecto en todas las sesiones."
} else {
    # User scope
    [Environment]::SetEnvironmentVariable('MAVEN_HOME', $MAVEN_HOME_DIR, 'User')
    Add-ToUserPath -bin $MAVEN_BIN
    Write-Host "MAVEN_HOME establecido en nivel User: $MAVEN_HOME_DIR"
    Write-Host "Cierra y abre la terminal o VS Code para que los cambios surtan efecto en la nueva sesión."
}

Write-Host "Comprueba con: mvn -v" -ForegroundColor Green

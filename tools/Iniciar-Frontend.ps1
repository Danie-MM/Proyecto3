# Inicia Angular con las versiones del equipo, sin cambiar las instalaciones globales.
# La ruta del proyecto se obtiene desde este archivo; no depende de donde se abra la terminal.
$ErrorActionPreference = 'Stop'
$frontendDirectory = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../frontend'))
$portableNodeDirectory = Join-Path $env:LOCALAPPDATA 'PetMatchCR/tools/node-v20.11.1-win-x64'
$portableNodeExecutable = Join-Path $portableNodeDirectory 'node.exe'

# Primero se busca la copia local preparada para PetMatchCR. Si no existe, se revisa PATH.
# No descargamos ni instalamos Node automaticamente: una version incorrecta se explica y se detiene.
if (Test-Path -LiteralPath $portableNodeExecutable -PathType Leaf) {
    $nodeExecutable = $portableNodeExecutable
} else {
    $nodeCommand = Get-Command node.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $nodeCommand) {
        throw 'Falta Node 20.11.1. Prepara esa version en PATH o en %LOCALAPPDATA%/PetMatchCR/tools/node-v20.11.1-win-x64 y vuelve a iniciar la tarea.'
    }
    $nodeExecutable = $nodeCommand.Source
}

# npm y Angular deben heredar el mismo Node. El cambio en PATH dura solo esta ejecucion.
$nodeDirectory = Split-Path -Parent $nodeExecutable
$npmExecutable = Join-Path $nodeDirectory 'npm.cmd'
$previousPath = $env:Path
$locationChanged = $false
try {
    $env:Path = $nodeDirectory + [System.IO.Path]::PathSeparator + $previousPath
    $nodeVersion = & $nodeExecutable --version
    if ($LASTEXITCODE -ne 0 -or $nodeVersion -ne 'v20.11.1') {
        throw "Se necesita Node 20.11.1; se encontro '$nodeVersion'. El script no cambia tu Node global. Consulta docs/EJECUTAR_EN_MI_PC.md."
    }
    if (-not (Test-Path -LiteralPath $npmExecutable -PathType Leaf)) {
        throw 'No se encontro npm.cmd junto a Node. Usa la distribucion completa de Node 20.11.1, que incluye npm 10.2.4.'
    }
    $npmVersion = & $npmExecutable --version
    if ($LASTEXITCODE -ne 0 -or $npmVersion -ne '10.2.4') {
        throw "Se necesita npm 10.2.4; se encontro '$npmVersion'. No se instalaran paquetes con una version diferente."
    }

    Push-Location -LiteralPath $frontendDirectory
    $locationChanged = $true
    if (-not (Test-Path -LiteralPath 'package-lock.json' -PathType Leaf)) {
        throw 'Falta frontend/package-lock.json. Recupera ese archivo del repositorio antes de instalar las dependencias.'
    }

    # node_modules se instala en la computadora y no se guarda en Git.
    # npm ci usa las versiones exactas del archivo compartido package-lock.json.
    $requiredDependencies = @(
        'node_modules/@angular/core/package.json',
        'node_modules/@angular/cli/bin/ng.js',
        'node_modules/typescript/package.json',
        'node_modules/.bin/ng.cmd'
    )
    $missingDependencies = @($requiredDependencies | Where-Object { -not (Test-Path -LiteralPath $_ -PathType Leaf) })
    if ($missingDependencies.Count -gt 0) {
        Write-Host 'Preparando las dependencias locales con npm ci...'
        & $npmExecutable ci
        if ($LASTEXITCODE -ne 0) {
            throw 'No se pudieron preparar las dependencias. Revisa el error anterior; Angular no se ha iniciado.'
        }
    }

    # Este proxy manda /api al backend de IntelliJ. npm start conserva el proxy de Docker.
    Write-Host "Node $nodeVersion / npm $npmVersion"
    Write-Host 'Frontend: http://127.0.0.1:4200/mascotas'
    Write-Host 'Backend esperado: http://127.0.0.1:8083. Para detener Angular usa Ctrl+C.'
    & $npmExecutable run start:local
    if ($LASTEXITCODE -ne 0) {
        throw 'Angular se detuvo con un error. Revisa el mensaje anterior en esta terminal.'
    }
} finally {
    if ($locationChanged) { Pop-Location }
    $env:Path = $previousPath
}

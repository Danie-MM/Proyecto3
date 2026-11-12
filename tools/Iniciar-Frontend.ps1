# Inicia Angular con las versiones del equipo, sin cambiar las instalaciones globales.
# La ruta del proyecto se obtiene desde este archivo; no depende de donde se abra la terminal.
$ErrorActionPreference = 'Stop'
$launcher = Join-Path $PSScriptRoot 'Iniciar-Frontend.cjs'
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

# Una sola implementacion comprueba Node/npm, package-lock y dependencias, y configura el proxy local.
# El modo por defecto es start:local e incluye --prebundle=false; build y test tambien se pueden pasar.
# Node recibe los argumentos por separado. No modificamos PATH ni la politica de PowerShell.
& $nodeExecutable $launcher @args
exit $LASTEXITCODE

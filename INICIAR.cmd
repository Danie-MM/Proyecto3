@echo off
setlocal
cd /d "%~dp0"
set "PETMATCHCR_INTERACTIVO=1"
if /I "%~1"=="--no-interactivo" set "PETMATCHCR_INTERACTIVO=0"

echo PetMatchCR - preparar y encender el proyecto
echo.
where docker >nul 2>&1
if errorlevel 1 (
    echo Falta Docker Desktop. Instalalo y abri Docker antes de continuar.
    goto error
)
docker info >nul 2>&1
if errorlevel 1 (
    echo Docker no responde. Abri Docker Desktop y espera Engine running.
    goto error
)
docker info --format "{{.OSType}}" | findstr /i /c:"linux" >nul
if errorlevel 1 (
    echo Este proyecto necesita contenedores Linux en Docker Desktop.
    goto error
)
docker compose version >nul 2>&1
if errorlevel 1 (
    echo Falta Docker Compose. Revisa la instalacion de Docker Desktop.
    goto error
)

if not exist ".env" (
    if exist "backend\.env" (
        copy /y "backend\.env" ".env" >nul
    ) else (
        copy /y ".env.example" ".env" >nul
    )
    if errorlevel 1 goto error
)
docker compose config --quiet
if errorlevel 1 goto error

echo La primera vez se descargan herramientas y dependencias. Puede tardar varios minutos.
docker compose up --build --detach --wait --wait-timeout 240
if errorlevel 1 goto error

echo.
docker compose ps
echo.
echo Listo: http://localhost:4200/comprobar-entorno
echo Podes cerrar esta ventana. El proyecto seguira funcionando en Docker.
echo Para detenerlo sin borrar datos, usa DETENER.cmd.
if "%PETMATCHCR_INTERACTIVO%"=="1" start "" "http://localhost:4200/comprobar-entorno"
if "%PETMATCHCR_INTERACTIVO%"=="1" pause
exit /b 0

:error
echo.
echo No se pudo completar el arranque. Revisa el mensaje anterior y COMENZAR.md.
echo Si Docker funciona, consulta: docker compose logs --tail 80
echo No borres volumenes para solucionar el error.
if "%PETMATCHCR_INTERACTIVO%"=="1" pause
exit /b 1

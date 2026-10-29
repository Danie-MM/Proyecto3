@echo off
setlocal
cd /d "%~dp0"
echo Deteniendo PetMatchCR sin borrar la base de datos...
docker compose down
if errorlevel 1 (
    echo No se pudo detener. Revisa que Docker Desktop este abierto.
    if /I not "%~1"=="--no-interactivo" pause
    exit /b 1
)
echo Listo. Los datos se conservan para el proximo inicio.
if /I not "%~1"=="--no-interactivo" pause
exit /b 0

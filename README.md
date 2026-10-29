# PetMatchCR

Base de desarrollo con Angular, Spring Boot y PostgreSQL. La pantalla de prueba conecta las tres partes; los módulos del proyecto se construirán sobre esta base.

## Descargar y encender

1. Abrí **Docker Desktop** y esperá a que el motor esté funcionando con contenedores Linux.
2. Cloná este repositorio o [descargá el ZIP](https://github.com/Danie-MM/Proyecto3/archive/refs/heads/main.zip) y extraelo.
3. Abrí una terminal de **PowerShell en la carpeta donde está `compose.yaml`** y ejecutá:

```powershell
if (-not (Test-Path .env)) {
    if (Test-Path backend/.env) { Copy-Item backend/.env .env }
    else { Copy-Item .env.example .env }
}
docker compose up --build -d --wait --wait-timeout 240
```

La primera parte prepara la configuración solo si falta y conserva la del backend anterior si existe. Docker descarga las herramientas, compila y enciende el proyecto. No necesitás instalar Java, Node, Angular ni PostgreSQL por separado.

Abrí <http://localhost:4200/comprobar-entorno>. Guardá un texto y consultá su ID para comprobar la conexión. La primera construcción puede tardar varios minutos.

En macOS/Linux, copiá `.env.example` a `.env` solo si este último no existe; si tenés `backend/.env`, reutilizalo. Después ejecutá el mismo comando `docker compose up` de arriba.

## Programar y detener

- **Frontend:** abrí `frontend/` en Visual Studio Code y trabajá en `src/app/`.
- **Backend:** abrí `backend/` en IntelliJ como proyecto Gradle y trabajá en `src/main/java/cr/petmatchcr/`. Las migraciones SQL están en `src/main/resources/db/migration/`.
- Después de editar código, repetí el comando de arranque para reconstruir y ver los cambios. Trabajá en tu propia rama y entregá mediante un PR hacia `main`.

Para detener sin borrar los datos, desde la raíz:

```powershell
docker compose down
```

Conservá tu `.env` y no lo subás a Git. No agregués `-v` al comando de apagado: elimina los datos. Usá una sola copia activa del proyecto y dejá libres los puertos 4200 y 8080.

Si algo falla: `docker compose ps` muestra el estado y `docker compose logs --tail 80` muestra los errores.

Se conservan las [licencias de la base utilizada](backend/THIRD_PARTY.md). PostgreSQL sigue pendiente de confirmación docente.

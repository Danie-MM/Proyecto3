# PetMatchCR: descargar y encender

Esta es la base para empezar a desarrollar juntos: Angular, Spring Boot y PostgreSQL conectados. Los módulos de usuarios, mascotas y adopción todavía se construirán sobre ella.

## La primera vez, en Windows

1. Instalá [Docker Desktop desde su página oficial](https://docs.docker.com/desktop/setup/install/windows-install/), completá su configuración y abrilo. Necesita contenedores Linux; en Windows se puede usar WSL 2. Esperá a que indique **Engine running**. Seguí los requisitos del fabricante si tu equipo pide virtualización, WSL o reinicio.
2. Descargá el ZIP de la versión compartida y elegí **Extraer todo**. Abrí la carpeta extraída donde están este documento y `INICIAR.cmd`. No ejecutés archivos desde dentro del ZIP.
3. Hacé doble clic en **`INICIAR.cmd`**. Prepara `.env` si falta, instala las versiones del proyecto dentro de Docker y enciende las tres partes. La primera descarga necesita internet y puede tardar varios minutos.
4. Cuando aparezca **Listo**, se abrirá <http://localhost:4200/comprobar-entorno>. Escribí un texto, guardalo y consultá el ID que aparece. Eso comprueba que tu pantalla, tu servidor y tu base se comunican.

No necesitás instalar Java, Maven, Gradle, Node, Angular ni PostgreSQL en Windows para ejecutar esta base. Docker los prepara; el backend usa Gradle, no Maven. Para editar necesitás un editor; para aportar código al equipo, usá Git y el flujo de ramas del README.

## Cada día

- Abrí Docker Desktop y ejecutá `INICIAR.cmd`.
- Editá los archivos con tu editor. Volvé a ejecutar `INICIAR.cmd` después de cambiar código: reconstruye la aplicación y conserva tus datos. Todavía no hay sincronización automática de cambios.
- Al terminar, ejecutá **`DETENER.cmd`**. No borra el volumen de PostgreSQL. Cada integrante tiene su propia base local; los datos no se comparten por GitHub.
- No subás `.env` al repositorio. El ejemplo incluye una clave ficticia para esta práctica local. Si ya existe un `.env`, el inicio lo conserva.

## Si van a desarrollar y subir cambios

Para trabajar en Git es preferible clonar la rama compartida en vez de descargar ZIP:

```powershell
git clone --branch feature/arranque-backend https://github.com/Danie-MM/Proyecto3.git
cd Proyecto3
.\INICIAR.cmd
```

Mientras esta entrega esté en revisión, descargá o cloná **`feature/arranque-backend`**: `main` todavía puede contener solamente la planificación. El PR indica el estado de integración. Desde la rama compartida, cada integrante crea su propia rama de trabajo; no cambien todos la misma rama.

## Pruebas y ayuda

Desde una terminal en la carpeta que contiene `compose.yaml`:

```powershell
docker compose ps
docker compose exec frontend npm run test:ci
docker compose exec backend ./gradlew test --no-daemon --rerun-tasks
```

`ps` debe mostrar `frontend`, `backend` y `db` como `healthy`. Las pruebas Java usan un esquema separado de tus registros manuales.

| Lo que sucede | Qué revisar |
| --- | --- |
| No encuentra Docker o el motor no responde | Instalación de Docker Desktop, abrirlo y esperar a que arranque. |
| Docker está en modo Windows containers | Cambiar a contenedores Linux desde Docker Desktop. |
| Dice que el puerto está ocupado | Otro programa usa 4200 u 8080. Identificá y detené ese programa, o pedí ayuda al coordinador; no borres datos. |
| Falla una descarga | Revisá internet y repetí `INICIAR.cmd`. Docker reutiliza las descargas terminadas. |
| La página no guarda o consulta | Ejecutá `docker compose logs --tail 80 frontend backend db` y compartí el error con coordinación, sin contraseñas. |
| No ves un cambio de código | Ejecutá `INICIAR.cmd` otra vez y recargá el navegador. |

No usés `docker compose down -v`: borra el volumen con tus datos. Usá una sola copia activa del proyecto en tu computadora: por defecto todas las copias usan los mismos nombres de servicios y volumen.

## macOS o Linux

Con Docker y Compose disponibles, desde la carpeta del proyecto:

```sh
if [ ! -f .env ]; then
  if [ -f backend/.env ]; then cp backend/.env .env; else cp .env.example .env; fi
fi
docker compose up --build -d --wait --wait-timeout 240
```

Abrí la misma dirección de la pantalla. Para detener: `docker compose down`. Los archivos `.cmd` son solamente para Windows. Estas instrucciones requieren validación del integrante en su sistema; la comprobación local se realizó en Windows con Docker Linux.

## Entender el proyecto

Las versiones acordadas son Angular 17.3.12, CLI 17.3.17, TypeScript 5.4.5, Node 20.11.1, npm 10.2.4, Java 21, Spring Boot 3.2.5, Gradle Wrapper 8.7 y PostgreSQL 15.19. Están fijadas en los archivos de construcción. Son la base académica acordada para desarrollo local; el motor PostgreSQL aún requiere confirmación docente.

Seguí la [guía del frontend](frontend/README.md), la [guía del backend](backend/README.md) y la [evidencia de pruebas](docs/evidencia-arranque.md) para aprender qué hace cada parte y conocer lo que ya se verificó.

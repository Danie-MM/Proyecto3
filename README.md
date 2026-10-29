# PetMatchCR · Proyecto 3

Base de desarrollo con Angular, Spring Boot y PostgreSQL conectados mediante Docker. La pantalla de prueba funciona; los módulos del proyecto se construirán sobre esta base.

## 1. Descargar y encender

Necesitás **Docker Desktop funcionando con contenedores Linux**, Git y tu editor. Docker prepara Java, Gradle, Node, Angular y PostgreSQL; no necesitás instalarlos por separado para ejecutar el proyecto.

Desde una terminal:

```powershell
git clone https://github.com/Danie-MM/Proyecto3.git
cd Proyecto3
.\INICIAR.cmd
```

También podés [descargar el ZIP](https://github.com/Danie-MM/Proyecto3/archive/refs/heads/main.zip), extraerlo y hacer doble clic en **INICIAR.cmd**. Para aportar cambios al equipo, usá Git.

Esperá a que termine y abrí <http://localhost:4200/comprobar-entorno>. Guardá un texto y consultá su ID para comprobar la conexión completa. La primera descarga puede tardar varios minutos.

## 2. Dónde programar

| Parte | Carpeta que abrís | Dónde escribís el código |
| --- | --- | --- |
| Frontend | `frontend/` en Visual Studio Code | `src/app/`: `pages`, `components`, `models`, `services`, `guards`, `interceptors`. |
| Backend | `backend/` en IntelliJ, como proyecto Gradle | `src/main/java/cr/petmatchcr/`: `controllers`, `models`, `services`, `repositories`, `dto`, `config`, `exceptions`. |
| Base de datos | Dentro del backend | `src/main/resources/db/migration/`: agregá migraciones nuevas; no modifiqués una ya aplicada. |

Las carpetas vacías contienen `.gitkeep` para que Git las conserve. El ejemplo funcional está en `frontend/src/app/features/entorno/` y `backend/src/main/java/cr/petmatchcr/entorno/`.

**Después de editar, ejecutá INICIAR.cmd otra vez:** reconstruye la aplicación; los cambios todavía no se sincronizan automáticamente. Para terminar el día, usá **DETENER.cmd**. Cada persona tiene su propia base local.

## 3. Trabajar con el equipo

Guardá tus cambios en tu rama antes de actualizar. Con `git status` limpio, desde la raíz del repositorio:

```powershell
git fetch origin
git switch main
git pull --ff-only origin main
git switch -c feature/mi-tarea
```

Reemplazá `mi-tarea` por un nombre de tu trabajo. Si Git avisa de cambios pendientes o ramas divergentes, consultá con coordinación sin descartar ni forzar cambios.

- Elegí tu tarea en el [tablero](https://github.com/users/NatanaelSolis/projects/1/views/1) o en los [issues](https://github.com/Danie-MM/Proyecto3/issues).
- Hacé commits pequeños con mensajes claros, subí tu rama y abrí un **PR hacia main** enlazando el issue. Indicá qué hiciste y qué probaste; pedí revisión a otro integrante antes de integrar.
- Acordá cambios de rutas, campos y tipos usando el [contrato API compartido](contratos/PetMatchCR_Contrato_API_inicial.yaml). Los bloques A–E del contrato son diseño pendiente de implementar.
- Coordiná cambios de dependencias, versiones y Docker con Natanael y José. No subás `.env`, `node_modules` ni archivos generados.

## 4. Comprobar y resolver problemas

Desde la raíz, con Docker encendido:

```powershell
docker compose ps
docker compose exec frontend npm run test:ci
docker compose exec backend ./gradlew test --no-daemon --rerun-tasks
```

Los tres servicios deben aparecer `healthy`. GitHub también ejecuta las comprobaciones del proyecto: [ver resultados](https://github.com/Danie-MM/Proyecto3/actions). Si falla el arranque, revisá `docker compose logs --tail 80 frontend backend db`.

**Conservá tu `.env` y tus datos:** INICIAR.cmd crea la configuración solo si falta. No usés `docker compose down -v`, porque borra la base. Usá una sola copia activa y el `compose.yaml` de la raíz. Los puertos 4200 y 8080 deben estar libres.

<details>
<summary>Versiones y archivos que sí necesita el proyecto</summary>

Angular 17.3.12, CLI 17.3.17, TypeScript 5.4.5, Node 20.11.1, npm 10.2.4, Java 21, Spring Boot 3.2.5, Gradle 8.7 y PostgreSQL 15.19. Usamos Gradle; PostgreSQL todavía requiere confirmación docente.

`compose.yaml`, los Dockerfile y `.env.example` preparan el entorno; `.gitignore` y `.gitattributes` cuidan lo que comparte Git; `.github/` y `scripts/` ejecutan las comprobaciones automáticas. Se conservan las [referencias y licencias](backend/THIRD_PARTY.md).

</details>

<details>
<summary>Si usás macOS/Linux o la configuración anterior del backend</summary>

En macOS/Linux, con Docker y Compose disponibles, desde la raíz:

```sh
if [ ! -f .env ]; then
  if [ -f backend/.env ]; then cp backend/.env .env; else cp .env.example .env; fi
fi
docker compose up --build -d --wait --wait-timeout 240
```

Para detener: `docker compose down`. Los `.cmd` son para Windows; cada integrante debe comprobar el arranque en su sistema.

Si todavía tenés encendido el Compose anterior, detenelo primero desde la raíz con `docker compose --env-file backend/.env -f backend/compose.dev.yaml down`. Después usá INICIAR.cmd o el Compose raíz; no los encendás al mismo tiempo. Se conserva el volumen anterior y se reutiliza `backend/.env` si falta `.env` en la raíz.

</details>

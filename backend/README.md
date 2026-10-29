# Backend de PetMatchCR: tu primer recorrido

Esta base permite guardar un texto y recuperarlo desde PostgreSQL. La pantalla Angular `/comprobar-entorno` usa esa API para comprobar la integración. Los módulos de negocio todavía quedan pendientes. Para recorrer la aplicación completa, seguí [la guía del frontend](../frontend/README.md).

## 1. Entender lo que descargaste

Abrí la carpeta `Proyecto3` en tu editor. En `backend/` encontrarás:

| Archivo o carpeta | Qué hace |
| --- | --- |
| `build.gradle` | Define Java 21, Spring Boot 3.2.5 y las bibliotecas del backend. |
| `gradlew` y `gradle/` | Descargan y usan Gradle 8.7 dentro de Docker. |
| `Dockerfile` | Construye el entorno Java y compila el código. |
| `../compose.yaml` | Configuración habitual: enciende frontend, backend y base de datos juntos. |
| `compose.dev.yaml` | Alternativa anterior para backend y BD; no usar al mismo tiempo que el Compose raíz. |
| `src/main/java/cr/petmatchcr/entorno/` | Código del ejemplo: recibir, validar, guardar y consultar. |
| `src/main/resources/db/migration/` | Cambios SQL numerados que Flyway aplica automáticamente. |
| `src/test/` | Pruebas automáticas contra PostgreSQL real, en el esquema `pruebas`. |

Docker ejecuta Java, Gradle y PostgreSQL. Tu Windows solo necesita Docker Desktop, Git y el editor. No ejecutes el Wrapper con el Java 25 del sistema; en este recorrido todos los comandos Gradle van dentro de Docker.

## 2. Preparar la configuración local

Abrí Docker Desktop y esperá `Engine running`. Usá una terminal ubicada en la **raíz `Proyecto3`** para todos los comandos de esta guía:

```powershell
if (-not (Test-Path .env)) {
    if (Test-Path backend/.env) { Copy-Item backend/.env .env }
    else { Copy-Item .env.example .env }
}
```

La copia se hace solamente si `.env` todavía no existe. Reutilizar `backend/.env` conserva la configuración de la base anterior. El ejemplo contiene una contraseña ficticia local y Git ignora `.env`. Cambiar ese archivo no cambia automáticamente la contraseña guardada en una base existente. No pongás contraseñas reales en el código.

## 3. Encender la base

Si ya estaba funcionando `backend/compose.dev.yaml`, detenelo primero desde la raíz con `docker compose --env-file backend/.env -f backend/compose.dev.yaml down`. Después usá el Compose raíz. La configuración anterior queda como alternativa para backend y BD, pero **no se deben mezclar ambas simultáneamente**: comparten nombre de proyecto y datos. Ver [configuración compartida](../docs/configuracion-backend.md).

Desde la raíz `Proyecto3`:

```powershell
docker compose config --quiet
docker compose up --build -d --wait
docker compose ps
```

`config` valida el archivo; `up` construye y arranca; `--wait` espera los servicios saludables. La primera construcción descarga dependencias y puede tardar varios minutos. Deben aparecer `db`, `backend` y `frontend` con estado `healthy`.

Abrí <http://localhost:8080/actuator/health>. Debe mostrar `{"status":"UP"}`. Esa respuesta incluye la comprobación de conexión a la base. La pantalla está en <http://localhost:4200/comprobar-entorno>; el puerto 8080 corresponde a la API.

## 4. Guardar y consultar algo real

En Postman, elegí **POST**, URL `http://localhost:8080/api/v1/pruebas-entorno`, Body → raw → JSON:

```json
{"texto":"Mi primera prueba de PetMatchCR"}
```

Esperá **201 Created** con `id`, `texto` y `creadoEn`. Copiá el ID y enviá **GET** a `http://localhost:8080/api/v1/pruebas-entorno/EL-ID-COPIADO`. Debe responder **200 OK** con los mismos datos. No escribás literalmente `EL-ID-COPIADO`.

También podés hacerlo desde PowerShell:

```powershell
$registro = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/pruebas-entorno' -ContentType 'application/json' -Body '{"texto":"Prueba desde PowerShell"}'
$registro
Invoke-RestMethod -Uri ("http://localhost:8080/api/v1/pruebas-entorno/" + $registro.id)
```

Las peticiones y errores de ejemplo están en [pruebas-entorno.http](../docs/pruebas-entorno.http). Los archivos `.http` son texto reutilizable; podés copiar sus datos a Postman sin instalar extensiones.

## 5. Seguir el código para aprender

1. Abrí `PruebaEntornoController`: identifica las dos rutas HTTP.
2. Seguí la llamada a `PruebaEntornoService`: recorta espacios, valida entre 1 y 80 caracteres y genera UUID/fecha.
3. Seguí la llamada a `PruebaEntornoRepository`: usa parámetros para invocar las rutinas SQL.
4. Abrí `V1__crear_prueba_entorno.sql`: identifica tabla, procedimiento de escritura y función de lectura.

`creadoEn` es fecha UTC, no la hora local de la computadora. Las rutas de prueba solo se habilitan con perfiles `local` o `test`; el acceso de usuarios le corresponde al bloque A.

## 6. Comprobar tus cambios

Después de editar Java o SQL nuevo, reconstruí y ejecutá las pruebas:

```powershell
docker compose up --build -d --wait
docker compose exec backend ./gradlew test --no-daemon --rerun-tasks
```

Los archivos se copian a la imagen; no hay sincronización automática, por eso reconstruís después de editar. Las pruebas usan el esquema `pruebas`, separado de tus registros manuales en `public`. El parámetro `--rerun-tasks` hace que realmente vuelvan a ejecutarse. No modifica el contenido de `public`. Para revisar también Angular, ejecutá `docker compose exec frontend npm run test:ci`.

Si algo falla:

```powershell
docker compose logs --tail 100 backend
docker compose logs --tail 50 db
```

Los mensajes explican qué parte falló. No cambies versiones ni borres la base para ocultar un fallo. Si V1 ya fue utilizada, agregá cambios en V2, V3, etc.

## 7. Detener y comprobar que tus datos siguen ahí

Conservá el ID creado en el paso 4:

```powershell
docker compose down
docker compose up -d --wait
```

Consultá ese mismo ID. `down` conserva el volumen; **`down -v` elimina los datos**, por lo que no se usa para detener normalmente.

## 8. Compartir un avance revisable

La rama de trabajo es `feature/arranque-backend`, creada desde `main` porque aún no existía `develop` al clonar. Esta preparación no crea commits ni publica cambios por sí sola.

Desde la raíz `Proyecto3` revisá `git status` y `git diff` antes de guardar un commit. José y vos deben acordar el punto de integración en `develop` antes de subir y abrir el PR de G-02. La tarea completa incluye revisar la integración y repetir las pruebas en la computadora de José.

Entregale [la configuración de integración](../docs/configuracion-backend.md) y [el contrato API](../contratos/PetMatchCR_Contrato_API_inicial.yaml). PostgreSQL sigue pendiente de confirmación del docente, según el acuerdo del equipo.

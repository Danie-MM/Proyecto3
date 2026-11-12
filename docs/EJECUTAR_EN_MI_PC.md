# Ejecutar PetMatchCR desde IntelliJ y Visual Studio Code

Esta es la modalidad para programar: **PostgreSQL corre en Docker**, Java se edita
en **IntelliJ** y Angular en **VS Code**. Las tareas compartidas de VS Code también
pueden iniciar y verificar el mismo backend Java; no crean otro proyecto.

| Parte | Dirección predeterminada |
| --- | --- |
| Angular | `http://localhost:4200/login` |
| API | `http://127.0.0.1:8083` |
| PostgreSQL | `127.0.0.1:55432`, base `petmatchcr` |
| Perfiles backend | `local,integracion` |
| Esquema de aplicación | `integracion_abc` |

Elegí esta modalidad o la de [todo en Docker](../README.md#todo-en-docker).
No las iniciés simultáneamente: compiten por puertos y pueden ejecutar dos
backends sobre los mismos datos.

## 1. Preparar herramientas y obtener la rama

En Windows usá:

- Git y Docker Desktop con contenedores Linux.
- Node **20.11.1**, que incluye npm **10.2.4**.
- JDK **21**, instalable desde IntelliJ, y Maven Wrapper del repositorio.
- Visual Studio Code y Chrome para las pruebas frontend.

Seguí [Obtener la revisión](../README.md#obtener-la-revisión) para descargar
`fix/integracion-abc`. Antes de cambiar de rama, comprobá
`git status` y conservá cualquier trabajo pendiente. No hace falta tener la ruta
personal de Natanael: cada integrante usa su propia carpeta.

Desde la raíz de esa copia comprobá:

```powershell
git branch --show-current
git log -1 --oneline
node --version
npm.cmd --version
```

Debe aparecer la rama de revisión y las versiones indicadas. Si falta Node,
instalá la distribución completa de esa versión. La tarea de VS Code busca
primero `%LOCALAPPDATA%\PetMatchCR\tools\node-v20.11.1-win-x64` y después `PATH`.
Esa carpeta portable es opcional; el equipo puede usar Node desde `PATH`.

## 2. Preparar la configuración de la computadora

En la raíz del repositorio ejecutá:

```powershell
node tools/Preparar-Entorno.cjs
```

El archivo crea `.env` si no existe y completa solamente `JWT_SECRET` y
`GOOGLE_CLIENT_ID` ausentes o vacíos. La clave JWT se genera con 32 bytes
aleatorios; el identificador Google es público y corresponde al mismo cliente del
frontend. Los valores existentes se conservan y no se imprimen. El archivo
`.env` permanece local, ignorado por Git.

Revisá `DB_PASSWORD`. En una base que ya existe debe ser la contraseña con la que
se inicializó el volumen: editar `.env` no cambia una contraseña de PostgreSQL.
No borres el volumen para resolver una diferencia de configuración.

Para el cliente Google actual, abrí **http://localhost:4200/login**. Ese origen
permitió seleccionar la cuenta en la comprobación visual; `http://127.0.0.1:4200`
produjo `origin_mismatch`. La aplicación redirige las visitas a ese host numérico
en el puerto 4200 hacia `localhost` antes de iniciar Angular y Google. No cambia
el proxy ni la conexión de API/PostgreSQL.

El script no modifica Google Cloud Console ni agrega orígenes autorizados. Si se
usa otro cliente o puerto, su configuración debe revisarse por separado y mantener
el mismo identificador público en backend y frontend.

## 3. Encender PostgreSQL

Abrí Docker Desktop y, desde la raíz, ejecutá:

```powershell
docker compose -f compose.yaml -f compose.ide.yaml up -d db
docker compose -f compose.yaml -f compose.ide.yaml ps
```

`compose.ide.yaml` está versionado y publica PostgreSQL solo en
`127.0.0.1:55432`. No hace falta crear un override personal. Si modificás
`DB_PORT` en `.env`, también debe coincidir el puerto de `DB_URL` usado por el
lanzador Java.

Si antes iniciaste todo en Docker, detené sus aplicaciones para liberar los puertos:

```powershell
docker compose -f compose.yaml stop backend frontend
```

Esto conserva la base. No agregués opciones para eliminar volúmenes.

## 4. Abrir la misma copia en ambos editores

En IntelliJ abrí **`backend/pom.xml` como proyecto Maven**, seleccioná **JDK 21**
y **Maven Wrapper** y recargá Maven. No importés módulos Gradle antiguos ni copies
dependencias manualmente.

En VS Code usá **Archivo > Abrir carpeta** y abrí **`frontend` de la misma copia**.
Compará las rutas completas: dos carpetas llamadas Proyecto3 pueden contener
código diferente. La configuración antigua de IntelliJ con `local,demo-b01` no
es el arranque actual de integración.

## 5. Verificar e iniciar desde las tareas compartidas

En VS Code, con `frontend` abierto, usá **Terminal > Ejecutar tarea**:

| Tarea | Para qué sirve |
| --- | --- |
| Backend: verificar integracion ABC | Ejecuta `clean verify`; termina y escribe `backend-verificacion.log` en la raíz. |
| Frontend: compilar | Comprueba la compilación de producción y termina. |
| Frontend: probar | Ejecuta las pruebas con Chrome Headless y termina. |
| Backend: iniciar integracion ABC | Inicia Java en 8083 con `local,integracion`; permanece en su terminal. |
| Frontend: iniciar local | Inicia Angular en 4200 y observa cambios. |

Primero verificá; después iniciá backend y frontend, cada uno en su terminal.
No ejecutés a la vez **verificar** e **iniciar** backend: ambas tareas reconstruyen
la misma salida Java. Para detener un servidor usá **Ctrl+C**.

El backend de la tarea es el mismo código que editás en IntelliJ. No iniciés además
una ejecución de IntelliJ en 8083. Tras modificar Java, detené y volvé a iniciar
su tarea; Angular recompila al guardar cambios de pantalla.

El lanzador busca JDK 21 en `JAVA_HOME` y en `.jdks`. Usa Maven Wrapper **3.9.16**,
sin instalar Maven globalmente ni cambiar la política de PowerShell. La salida
Java queda en `%LOCALAPPDATA%\PetMatchCR\build\<identificador-de-la-copia>`,
fuera de OneDrive, para evitar bloqueos de sincronización. Cada copia tiene su
propia salida; los informes no tienen por qué estar en `backend/target`.

El lanzador frontend prepara `node_modules` con `npm ci` si faltan dependencias o
cambia `package-lock.json`. Son dependencias de Windows; las del contenedor no
las reemplazan. Solo al iniciar local agrega `--prebundle=false` para evitar el
caché de dependencias de Vite que puede bloquear OneDrive. Compilación de producción
y pruebas conservan sus comandos.

## 6. Entrar y comprobar el funcionamiento

Abrí **http://localhost:4200/login** y conservá `localhost` durante la sesión.
Una visita a `http://127.0.0.1:4200` se redirige automáticamente; sus cookies y
almacenamiento previos no se comparten con `localhost`. `/mascotas`, `/mi-ubicacion` y
`/comprobar-entorno` están protegidas; se requiere autenticación real. El selector
histórico de actores de B no sustituye este acceso.

El navegador llama a `/api`; el proxy local lo dirige a 8083. El comando
`npm start` se reserva para Docker, donde el proxy apunta a `backend:8080`.
No cambiés las URLs de cada servicio Angular para alternar modalidades.

Después de completar el acceso Google con una cuenta de prueba, verificá con datos
ficticios: crear y recargar una mascota, editarla, guardar y recargar la ubicación,
y rechazar accesos desde otra cuenta. Registrá revisión, pasos y resultados.
No des por aprobado ese recorrido si solo viste la pantalla de login.

Las tablas de aplicación están en `integracion_abc`, separado de `public` y
`demo_b01`. Es un esquema dentro de la misma base, no un contenedor nuevo.
Las pruebas usan `pruebas_abc` y `pruebas_demo_abc` por separado; no deben
apuntar a datos que se quieran conservar. Revisá las advertencias de migraciones en
[INTEGRACION_ABC.md](INTEGRACION_ABC.md) antes de reutilizar un esquema antiguo.

## 7. Interpretar un error

| Mensaje o síntoma | Qué comprobar primero |
| --- | --- |
| Node/npm de otra versión | Node 20.11.1 y npm 10.2.4 seleccionados en la terminal/tarea. |
| Falta una variable local | Ejecutar el preparador y revisar `.env` sin publicar sus valores. |
| No se encuentra JDK 21 | SDK en IntelliJ y JDK 21 disponible para el lanzador en `JAVA_HOME` o `.jdks`. |
| Fallo de conexión PostgreSQL | Docker encendido, servicio db, puerto 55432 y credenciales correctas. |
| Puerto 4200 u 8083 ocupado | Otra tarea o aplicación Docker sigue ejecutándose. Detener la instancia que no corresponde. |
| Google muestra `origin_mismatch` | Abrir `http://localhost:4200/login`, origen comprobado con el cliente actual. La redirección desde `127.0.0.1:4200` requiere esta revisión del frontend. |
| API rechaza una operación | Revisar respuesta y logs; no desactivar seguridad ni usar una identidad de demo para ocultarlo. |
| Flyway encuentra una versión incompatible | Revisar el historial de ese esquema; no borrar volúmenes ni ejecutar `clean`. |
| VS Code marca `describe`, `it` o `expect` | Elegir **TypeScript: Select TypeScript Version > Use Workspace Version**, versión 5.4.5; conservar las pruebas. |

## Evidencia y antecedentes

En esta reparación se confirmaron **160 pruebas backend** (0 fallos, 0 errores,
0 omitidas), sin cambios posteriores en ese backend. La última tarea frontend
terminó con **49/49 pruebas aprobadas** en Chrome Headless nativo, incluidas cinco
nuevas de redirección. También se comprobó la compilación frontend tras `npm ci`;
backend y frontend iniciaron localmente.
En la última comprobación visual desde `localhost:4200` se seleccionó una cuenta
Google y se completó su registro real en el esquema local `integracion_abc`.
La aplicación mostró `/comprobar-entorno` con la sesión iniciada y rol `ADOPTANTE`.
Se observó en navegador la redirección de `127.0.0.1:4200` a `localhost:4200`.
La ruta `/mascotas` cargó correctamente su lista vacía y `/mi-ubicacion` mostró
formulario y mapa sin errores. El recorrido CRUD completo de mascotas y ubicación
con esa cuenta sigue pendiente.

Las verificaciones anteriores de B con Luna, `demo-b01` y 28 pruebas frontend
pertenecen a una revisión histórica. Las guías antiguas siguen sirviendo para
entender su código, pero no describen el acceso actual ni garantizan que el selector
de demostración funcione a través de las rutas protegidas.

Los estilos compartidos continúan en `frontend/src/app/shared/styles/` y el modal
de confirmación en `shared/components/`. Conservá esos controles y las pruebas;
no hace falta reconstruirlos para unir las pantallas del equipo.

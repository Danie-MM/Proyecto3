# Ejecutar y revisar la integración A + B + C

Esta guía corresponde a las correcciones de integración sobre `main` en `0fec949`.
La copia de reparación trabaja en `fix/integracion-abc`. Tener los archivos unidos no
equivale a haber aceptado todas las historias: deben revisarse compilación, pruebas
y recorridos completos con la misma revisión.

## Estado comprobado de esta copia

La base de la reparación es `main` en `0fec949` y la rama de revisión es
`fix/integracion-abc`. Registrá el commit que muestre tu copia al descargarla;
trabajar en esta rama no implica haber integrado sus correcciones a `main`.

| Comprobación | Resultado y alcance |
| --- | --- |
| Dependencias frontend | Reinstaladas con `npm ci`, respetando `package-lock.json`. |
| Compilación frontend | Compilación de producción completada después de preparar las dependencias. |
| Pruebas frontend | Última tarea nativa: **49/49 aprobadas** con Chrome Headless de Windows, incluidas cinco nuevas de redirección. |
| Pruebas backend | **160 pruebas aprobadas**, 0 fallos, 0 errores y 0 omitidas. La ejecución nativa terminó con `BUILD SUCCESS` en el registro de las 20:49:21; el backend no se modificó después. |
| Arranque local | Backend y frontend iniciaron correctamente con la configuración local de integración. |
| Origen Google | `http://localhost:4200` permitió seleccionar la cuenta. `http://127.0.0.1:4200` había producido `origin_mismatch`. |
| Registro y sesión reales | Registro Google completado en el esquema local `integracion_abc`; la aplicación mostró `/comprobar-entorno` con sesión iniciada y rol `ADOPTANTE`. |
| Redirección local | Verificada en navegador: `127.0.0.1:4200` pasó a `localhost:4200`. |
| Pantallas autenticadas | `/mascotas` cargó correctamente la lista vacía; `/mi-ubicacion` mostró formulario y mapa sin errores. |
| Recorrido completo A/B/C | El CRUD completo autenticado de mascotas y ubicación sigue pendiente. Las pruebas automatizadas, el arranque y el registro no sustituyen esa comprobación ni la aceptación del equipo. |

La verificación backend aprobada se obtuvo después de corregir los fallos y
separar los esquemas de las pruebas normales y de demostración. El resultado
vigente es el de esa ejecución completa; no el de los intentos fallidos anteriores.
Estos resultados verifican esta copia y configuración, sin dar por aceptadas todas
las historias del proyecto.

## Primer bloque: obtener y comprobar la revisión

Desde la raíz del repositorio comprobá primero el estado y obtené las referencias
remotas:

```powershell
git status --short --branch
git fetch origin
git branch -a
```

Conservá cualquier trabajo pendiente antes de cambiar de rama. Si
`origin/fix/integracion-abc` no aparece, confirmá su publicación con el coordinador.
Si todavía no existe la rama local, creala siguiendo la remota:

```powershell
git switch -c fix/integracion-abc --track origin/fix/integracion-abc
```

Si ya existe, usá `git switch fix/integracion-abc` en lugar del comando anterior.
Después actualizá únicamente esa rama:

```powershell
git pull --ff-only origin fix/integracion-abc
git branch --show-current
git log -1 --oneline
git diff --stat
git diff --cached --stat
```

Se espera `fix/integracion-abc` y el commit publicado para revisión. Las dos últimas
consultas separan los cambios sin preparar y los ya preparados para commit. Si Git
informa cambios locales o historias divergentes, revisá el estado antes de
continuar; no uses `reset` ni descartes trabajo para forzar la actualización.

Si preferís conservar intacta tu copia anterior, podés clonar la rama de revisión
en una carpeta nueva:

```powershell
git clone --branch fix/integracion-abc https://github.com/Danie-MM/Proyecto3.git Proyecto3-integracion
cd Proyecto3-integracion
```

Conservá rama y commit para acompañar las pruebas. Descargar esta rama no incorpora
sus cambios a `main`; esa integración requiere la revisión acordada del equipo.

## Abrir la misma copia en los dos editores

En IntelliJ abrí `backend/pom.xml` como proyecto Maven y elegí JDK 21. En Visual
Studio Code abrí la carpeta `frontend` del mismo repositorio. Comprobá la ruta
completa: dos carpetas con el mismo nombre pueden contener revisiones diferentes.

Las tareas de VS Code están en `frontend/.vscode/tasks.json`. Usá **Terminal >
Ejecutar tarea**:

| Tarea | Resultado |
| --- | --- |
| Backend: verificar integracion ABC | Ejecuta `clean verify` con Maven Wrapper y registra el resultado en `backend-verificacion.log`; usa esquemas de prueba separados. No deja un servidor iniciado. |
| Backend: iniciar integracion ABC | Maven Wrapper inicia Spring en `http://127.0.0.1:8083`, perfiles `local,integracion`. |
| Frontend: iniciar local | Angular queda disponible en `http://localhost:4200/login`. |
| Frontend: compilar | Compila producción y termina; no inicia un servidor. |
| Frontend: probar | Ejecuta las pruebas una vez con Chrome Headless. Requiere Chrome instalado. |

Los servidores permanecen en sus terminales; detenelos con **Ctrl+C**. No ejecutés
simultáneamente otra configuración de IntelliJ en el puerto 8083. El backend
iniciado desde VS Code es el mismo código Java que editás en IntelliJ; un cambio de
Java requiere detener y volver a iniciar el backend.

Ejecutá **verificar** y **iniciar** por separado. Ambas tareas limpian y utilizan
la misma salida de compilación de esta copia; no conviene reconstruirla mientras
el backend está funcionando. La verificación termina; el inicio permanece activo.

Las tareas buscan primero Node portable 20.11.1 en
`%LOCALAPPDATA%\PetMatchCR\tools\node-v20.11.1-win-x64` y después en PATH. El
lanzador de Angular comprueba Node/npm y prepara `node_modules` con `npm ci` cuando
cambia `package-lock.json`. No instala herramientas globales ni cambia políticas
de PowerShell.

Para **Frontend: iniciar local**, el lanzador agrega `--prebundle=false`. Esto
desactiva el preempaquetado local de dependencias de Vite, cuyo caché puede quedar
bloqueado por OneDrive. Angular sigue compilando y observando cambios. El ajuste
solo afecta al servidor de desarrollo; no cambia los comandos de compilación de
producción ni de pruebas.

## Configuración personal y base separada

En Windows necesitás Node 20.11.1 con npm 10.2.4, JDK 21 y Docker Desktop con
contenedores Linux. Desde la raíz de la misma copia ejecutá:

```powershell
node tools/Preparar-Entorno.cjs
docker compose -f compose.yaml -f compose.ide.yaml up -d db
```

El preparador crea `.env` a partir del ejemplo si falta, genera un `JWT_SECRET`
aleatorio cuando está ausente o vacío y completa el cliente Google público del
equipo si falta. Conserva los valores existentes y no imprime secretos.
`compose.ide.yaml` está versionado y expone PostgreSQL en `127.0.0.1:55432`.
Si cambiás `DB_PORT`, también debe coincidir el puerto de `DB_URL` del backend.

PostgreSQL debe estar encendido antes de iniciar el backend. El archivo **`.env` de
la raíz**, ignorado por Git, necesita `DB_PASSWORD`, `JWT_SECRET` y
`GOOGLE_CLIENT_ID`. La clave JWT debe ser Base64 de al menos 32 bytes aleatorios;
el cliente Google debe coincidir con el autorizado para el frontend. Conservá esos
valores únicamente en tu configuración local. No los pongás en capturas, mensajes
de commit ni esta guía.

El cliente Google es un identificador público compartido con la constante del
frontend. **http://localhost:4200** es el origen aceptado por la configuración
actual: la comprobación visual permitió seleccionar una cuenta, completar su
registro e iniciar sesión. El host `http://127.0.0.1:4200` había producido
`origin_mismatch`.

La aplicación redirige las visitas a `http://127.0.0.1:4200` hacia
`http://localhost:4200` antes de iniciar Angular y Google. Se conservan la ruta,
la consulta, el fragmento y el puerto; API, base y proxy mantienen sus direcciones. Las cookies y el
almacenamiento de ambos hosts son distintos, por lo que la sesión debe continuar
en `localhost`. El preparador no cambia Google Cloud Console ni autoriza otros
orígenes.

Si el volumen de PostgreSQL ya existe, conservá la contraseña con la que se
inicializó: cambiar `DB_PASSWORD` en `.env` no cambia la contraseña de la base.

`tools/Iniciar-Backend.cjs` lee ese archivo sin ejecutarlo ni imprimir su contenido.
Las variables ya definidas en el proceso tienen prioridad. Si se omiten, utiliza:

| Configuración | Valor local |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://127.0.0.1:55432/petmatchcr` |
| `DB_USERNAME` | `petmatch_app` |
| Perfiles | `local,integracion` |
| Esquema | `integracion_abc` |
| API | `127.0.0.1:8083` |

El lanzador busca JDK 21 en `JAVA_HOME` y luego en la carpeta `.jdks` del usuario.
Usa el Wrapper Maven del repositorio. En Windows invoca su archivo `.cmd`, sin
ejecutar archivos `.ps1` propios ni cambiar `ExecutionPolicy`.

La salida de Java/Maven se guarda fuera de OneDrive, en
`%LOCALAPPDATA%\PetMatchCR\build\<identificador-de-la-copia>`. El identificador se
calcula a partir de la ruta del repositorio: dos copias distintas no comparten
clases compiladas. Esto evita que la sincronización bloquee esos archivos sin
mover el código que abrís en los editores. Las tareas de inicio y verificación
usan esa misma salida; no busqués necesariamente sus resultados en
`backend/target`.

El perfil `integracion` separa las tablas e historial de Flyway de `public`,
`demo_b01` y `pruebas`. Sigue usando la base PostgreSQL local existente; un esquema
separado no es un contenedor ni un volumen nuevo. El usuario de base debe poder
crear ese esquema. No actives `dev` ni `demo-b01` para aparentar integración con
usuarios reales.

La tarea **Backend: verificar integracion ABC** no activa `local,integracion`.
Por defecto, las pruebas normales usan `pruebas_abc` y las de demostración usan
`pruebas_demo_abc`; deben ser diferentes. Se pueden configurar mediante
`TEST_DB_SCHEMA` y `TEST_DEMO_DB_SCHEMA`, con nombres que empiecen por `pruebas_`
y contengan solo minúsculas, números o guion bajo. Las pruebas preparan y modifican
datos en esos esquemas: no deben apuntar a `public`, `integracion_abc`, `demo_b01`
ni a información que se quiera conservar.

## Alternativa: todo en Docker

En otra computadora podés ejecutar las aplicaciones en contenedores. Instalá
Node para preparar `.env` y abrí Docker Desktop con contenedores Linux. Desde la
raíz:

```powershell
node tools/Preparar-Entorno.cjs
docker compose -f compose.yaml up --build -d --wait
```

El archivo explícito evita cargar un `compose.override.yaml` personal. En esta
modalidad la API se publica en 8080, Angular en 4200 y el backend usa
`local,integracion` con el mismo esquema `integracion_abc`. PostgreSQL queda en la
red de Docker. Los puertos indicados son los predeterminados del ejemplo.

Usá una sola modalidad por vez. Antes de pasar a los editores, detené las
aplicaciones de Docker con `docker compose -f compose.yaml stop backend frontend`
y levantá la base con el comando de `compose.ide.yaml`. Para detener todo sin
eliminar datos, usá `docker compose -f compose.yaml down`, sin `-v`.
La reproducción completa en otra computadora requiere registrar su propio
resultado; las pruebas 160/49 citadas aquí son las ejecuciones nativas verificadas.

## Qué se reconcilió

- Maven conserva Java 21, dependencias de seguridad, JDBC para B y JPA para A/C.
  MapStruct 1.6.3 y su integración con Lombok 0.2.0 tienen versiones explícitas.
- Las migraciones B **V1–V6 se conservan**. A pasa a `V7__crear_usuarios.sql` y C a
  `V8__crear_preferencia_ubicacion.sql` para evitar tres migraciones llamadas V2.
- V9 agrega un UUID público persistente al usuario para enlazar identidad de A con
  B/C sin transformar arbitrariamente correos o números en UUID.
- V10 agrega persistencia de idempotencia para las preferencias de ubicación.
- La navegación reúne acceso, mascotas y preferencias. Las autorizaciones deben
  comprobarse también mediante API y con dos cuentas diferentes.

**Si alguien ya ejecutó las antiguas V2 de usuarios o ubicación**, no debe aplicar
esta secuencia sobre ese esquema sin analizar `flyway_schema_history` y preparar
una transición acordada. Renombrar un archivo no actualiza el historial de la base.
No uses `Flyway clean`, no borres volúmenes ni manipules el historial para ocultar
un conflicto. El primer ensayo se hace en `integracion_abc` nuevo.

## Orden para comprobar el resultado

1. Completar el primer bloque de Git y abrir esa misma copia en ambos editores.
   Recargar Maven en IntelliJ con JDK 21.
2. Iniciar PostgreSQL, completar `.env` y ejecutar **Backend: verificar integracion
   ABC**. Esperar a que termine. Solo `BUILD SUCCESS`, junto con los informes de
   pruebas sin fallos ni errores, permite registrar la verificación como aprobada.
   Si falla, revisar `backend-verificacion.log`, conservar el error y corregir su
   causa; no borrar pruebas ni tablas para presentar un resultado satisfactorio.
3. Ejecutar **Frontend: compilar** y **Frontend: probar** cuando corresponda
   verificar cambios nuevos. Con ambas verificaciones resueltas, ejecutar
   **Backend: iniciar integracion ABC** y luego **Frontend: iniciar local**.
   Comprobar que el backend inicia sin errores de configuración o migraciones y
   entrar por `http://localhost:4200/login`.
   Mantener `localhost` durante la sesión; las visitas a `127.0.0.1:4200` se
   redirigen a ese origen.
4. Con datos ficticios, probar registro/ingreso, crear una mascota, recargar y
   editar. Guardar y recargar las preferencias de ubicación de la misma cuenta.
5. Probar cuenta distinta, acceso ajeno, cierre de sesión, reintentos y edición
   concurrente. Un fallo debe conservar datos y producir un mensaje interpretable.
6. Registrar comando, revisión, resultado y límites de las pruebas. Una compilación
   exitosa no sustituye las pruebas funcionales ni la aceptación del equipo.

El frontend llama a `/api` mediante su proxy local hacia 8083. Si aparece un error
de conexión, comprobá primero que el backend siga activo; no cambiés las URLs de
cada servicio por separado.

## Alcance pendiente

Esta integración se centra en **A, B y C**. **D no tiene aún un catálogo de adopción
implementado** en la revisión de partida; su carpeta vacía no constituye una
entrega. E permanece fuera de este trabajo. El acceso existente con Google debe
contrastarse con los criterios originales de A-01; conectar los módulos no supone
aprobar automáticamente todos los requisitos del proyecto.

| Módulo | Qué no se debe dar por terminado solo por integrar el código |
| --- | --- |
| A | El registro original, el ciclo completo de sesión y la autorización de organizaciones necesitan evidencia específica. Google no sustituye automáticamente esos criterios. |
| B | B-01 conserva perfiles privados. Deben comprobarse identidad real y aislamiento entre cuentas; publicación y proyección pública de B-02 son trabajo distinto. |
| C | Guardar preferencias de ubicación no completa reportes de extravío, selección de destinatarios ni envío de alertas. Deben probarse recarga, acceso ajeno, versiones y reintentos. |
| D | El catálogo público y sus filtros todavía requieren implementación y la información publicable de B-02; no se debe exponer una respuesta privada de B. |

La aceptación final exige los recorridos y criterios acordados con el equipo. Las
49 pruebas frontend y las 160 pruebas backend aprobadas son evidencia de esas
ejecuciones; no certifican que PetMatchCR completo esté terminado ni sustituyen la
comprobación pendiente del recorrido CRUD completo autenticado.

Las guías anteriores de B con `demo-b01` y el selector de Luna describen una
demostración histórica. Las rutas actuales `/mascotas`, `/mi-ubicacion` y
`/comprobar-entorno` requieren autenticación; no se promete acceso por aquel
selector ni se debe usar para aparentar integración real.

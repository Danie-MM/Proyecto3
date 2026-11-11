# Ejecutar PetMatchCR desde IntelliJ y Visual Studio Code

En esta forma de trabajo, **Docker mantiene PostgreSQL**, **IntelliJ ejecuta el backend** y **Visual Studio Code ejecuta el frontend**. Así podés editar Java y Angular y observar los cambios sin reconstruir los tres contenedores. La alternativa de ejecutar todo en Docker sigue en el [README](../README.md).

| Parte | Herramienta | Dirección local |
| --- | --- | --- |
| Pantalla Angular | Visual Studio Code | `http://127.0.0.1:4200/mascotas` |
| API Spring Boot | IntelliJ IDEA | `http://127.0.0.1:8083` |
| PostgreSQL | Docker Desktop | `127.0.0.1:55432`, base `petmatchcr` |

Usá `127.0.0.1` de forma consistente durante la demostración. El navegador envía las consultas a `/api` en el puerto 4200; el proxy local las dirige al backend en 8083. No hace falta cambiar las URLs de cada servicio Angular.

## 1. Abrir la copia correcta del proyecto

La copia de trabajo de Natanael está en:

```text
C:\Users\natan\OneDrive\Documentos\GitHub\Proyecto3
```

En IntelliJ abrí **`backend/pom.xml` como proyecto Maven**. En Visual Studio Code usá **Archivo → Abrir carpeta** y seleccioná **`frontend` dentro de esa misma copia**. Los compañeros usan la ruta donde hayan clonado este repositorio; no necesitan una carpeta llamada `natan`.

Revisá la ruta, no solo el nombre de la ventana. Una copia anterior dentro de `Documents/Codex/.../Proyecto3` puede tener el mismo nombre y mostrar código diferente. En GitHub Desktop comprobá que tu repositorio sea el correcto y tu rama sea `NATANAEL-DEV` antes de guardar avances.

## 2. Encender la base de datos

Abrí Docker Desktop. Desde una terminal en la raíz de `Proyecto3`, ejecutá:

```powershell
docker compose up -d db
```

La configuración local de esta computadora publica PostgreSQL en 55432. En una copia nueva, el archivo personal `compose.override.yaml` puede contener:

```yaml
services:
  db:
    ports:
      - "127.0.0.1:55432:5432"
```

La contraseña se toma de `DB_PASSWORD` en el archivo local `.env`, preparado a partir de `.env.example`. No copies contraseñas a Java, a esta guía ni a un commit. La base persiste en el volumen de Docker; detener el contenedor no elimina sus tablas.

Si venías usando la aplicación completa en Docker y vas a trabajar desde los editores, detené sus dos aplicaciones para evitar confundir pantallas y servidores:

```powershell
docker compose stop backend frontend
```

Ese comando mantiene PostgreSQL encendido. No uses opciones para eliminar volúmenes al cambiar de forma de ejecución.

## 3. Ejecutar el backend en IntelliJ

1. En **File → Project Structure**, elegí **SDK 21**. El proyecto usa Java 21; el Java global de otra versión no reemplaza esta selección.
2. En las opciones de Maven elegí **Maven Wrapper**. El repositorio fija Maven **3.9.16**; no necesitás instalar Gradle ni un Maven global.
3. Recargá el proyecto desde el panel **Maven** y esperá a que termine de importar las dependencias.
4. Seleccioná la configuración **PetMatchCR Maven local**. La clase principal es `cr.petmatchcr.PetMatchCrApplication` y el módulo debe ser **`petmatchcr-backend`**, sin `.main` ni `.test`.
5. Confirmá que esa configuración use estos valores locales:

| Configuración | Valor esperado |
| --- | --- |
| Directorio de trabajo | La carpeta `backend` de esta copia |
| Java de la ejecución | JDK 21 |
| `SERVER_ADDRESS` | `127.0.0.1` |
| `SERVER_PORT` | `8083` |
| `SPRING_PROFILES_ACTIVE` | `local,demo-b01` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://127.0.0.1:55432/petmatchcr` |
| `SPRING_DATASOURCE_USERNAME` | `petmatch_app` |
| `SPRING_DATASOURCE_PASSWORD` | La contraseña local de PostgreSQL, sin publicarla |

6. Presioná **Run** y esperá el mensaje de aplicación iniciada. Los errores de arranque se leen en esa consola; abrir el archivo Java no inicia el servidor.

Maven administra dependencias y construcción. La configuración de ejecución de IntelliJ inicia la clase principal de Spring Boot; que su nombre diga “Maven local” no la convierte en un comando Maven.

El perfil `local` permite trabajar por HTTP en esta computadora. `demo-b01`, usado junto con `local`, habilita las identidades ficticias y el esquema **`demo_b01`**. La sesión real del módulo A todavía se integra por separado; sin la demostración no se inventa un usuario para permitir el acceso.

## 4. Ejecutar el frontend desde Visual Studio Code

El proyecto fija **Node 20.11.1 y npm 10.2.4**. En esta computadora se preparó una copia de Node exclusiva para PetMatchCR en:

```text
%LOCALAPPDATA%\PetMatchCR\tools\node-v20.11.1-win-x64
```

La tarea también acepta Node 20.11.1 con npm 10.2.4 desde `PATH` si no existe esa copia. Si encuentra una versión diferente, se detiene y explica el problema. No descarga programas, no cambia el Node global y no modifica las políticas de PowerShell.

1. Con la carpeta **`frontend`** abierta en Visual Studio Code, seleccioná **Terminal → Ejecutar tarea**.
2. Elegí **Frontend: iniciar local**.
3. Esperá la compilación de Angular y abrí **`http://127.0.0.1:4200/mascotas`**.
4. Mantené la terminal abierta mientras usás la pantalla. Para detenerla, presioná **Ctrl+C** en esa terminal.

La tarea ejecuta Node directamente con `tools/Iniciar-Frontend.cjs`. Este archivo comprueba las versiones y las dependencias locales. Si faltan, ejecuta `npm ci`, que instala las versiones registradas en `package-lock.json`; después ejecuta `npm run start:local`. Usa el mismo Node para iniciar npm y pasa sus argumentos por separado, sin construir comandos de shell.

El primer intento de la tarea con Windows PowerShell 5 fue bloqueado por su política `Restricted`. Por eso la tarea ahora inicia JavaScript con Node: no requiere cambiar la política, usar `Bypass` ni ejecutar `npm.ps1`.

`node_modules` existe solo en tu computadora y no se sube a Git. Las dependencias instaladas dentro del contenedor de Docker no sustituyen las que necesita Visual Studio Code en Windows. Si el equipo cambia dependencias y ya tenés una instalación local, actualizala con `npm ci` usando las versiones indicadas.

Con Node 20.11.1 ya seleccionado en `PATH`, también podés iniciar el archivo desde una terminal en la raíz del repositorio:

```text
node tools/Iniciar-Frontend.cjs
```

Se conserva `tools/Iniciar-Frontend.ps1` como alternativa desde la sesión de **PowerShell 7** donde ya funcionó, estando en la raíz del repositorio:

```powershell
.\tools\Iniciar-Frontend.ps1
```

Si otra sesión bloquea ese archivo, usá la tarea de Node; no hace falta cambiar la política de ejecución.

Los dos comandos del proyecto tienen propósitos distintos:

| Comando | Uso | Destino de `/api` |
| --- | --- | --- |
| `npm run start:local` | Frontend local con backend de IntelliJ | `127.0.0.1:8083` |
| `npm start` | Frontend dentro de Docker Compose | `backend:8080` |

`backend` es un nombre de la red de Docker. Windows no lo usa para encontrar el backend de IntelliJ; por eso agregamos `proxy.local.conf.json` y conservamos el proxy de Docker.

## 5. Comprobar un cambio que entendés

Elegí **Responsable de Luna** en la demostración. Conservá la Luna inicial activa y usá **Luna prueba B-01** para practicar alta, edición y desactivación. Guardá una edad, recargá la página y comprobá que persista. El recorrido completo está en [B01_PASO_A_PASO.md](B01_PASO_A_PASO.md).

Angular vuelve a compilar al guardar un cambio de pantalla. Después de modificar Java, reiniciá la ejecución del backend en IntelliJ para usar el código nuevo. Al agregar una migración, el nuevo arranque permite que Flyway la aplique; no edites una migración que ya se ejecutó.

En pgAdmin usá la conexión local a `127.0.0.1:55432`, base `petmatchcr` y las credenciales locales. Para esta demostración buscá **Schemas → demo_b01 → Tables → mascota**. Las tablas de `public` y `pruebas` pertenecen a otros contextos y pueden mostrar datos distintos.

## 6. Entender los archivos rojos

El color por sí solo no explica el problema. Abrí el panel **Problems** y leé el mensaje:

- **Cannot resolve symbol / no encuentra una clase:** verificá el módulo, el proyecto Maven importado y sus dependencias. Los módulos antiguos `petmatchcr-backend.main` y `.test` provenían de Gradle y pueden superponerse al módulo Maven. Se corrige su vinculación en IntelliJ; no se borra el código para ocultar el aviso.
- **No encuentra `@angular/...`:** revisá que abriste el `frontend` correcto y que instalaste sus dependencias locales. Un contenedor funcionando no demuestra que el editor tenga `node_modules`.
- **Función SQL desconocida:** puede ser una inspección del editor sin la conexión o el esquema correctos. Contrastalo con las migraciones de PostgreSQL y con el error real del servidor antes de modificar SQL.
- **Color o marca de Git en el árbol:** indica el estado del archivo respecto al repositorio, por ejemplo un cambio pendiente o un archivo nuevo. No equivale por sí mismo a un error de compilación.
- **Conexión rechazada al guardar:** verificá que IntelliJ siga ejecutando el backend en 8083 y que Angular se haya iniciado con `start:local`.

## 7. Dónde están los estilos compartidos

Los valores comunes del Plan están en `frontend/src/app/shared/styles/tokens.css`. Los controles reutilizables están en `components.css`: `btn-primary`, `btn-secondary`, `btn-link`, `btn-danger`, los campos de entrada, las tarjetas y los tamaños de los diálogos. Cada módulo puede usar esas clases y reservar su CSS para la distribución de su pantalla.

La paleta conserva café claro `#C9A27A`, café oscuro `#7C6349`, crema `#F3E9D7`, gris `#D6D2CE` y verde salvia `#7A8F6A`. Se normalizaron los códigos incompletos de blanco y gris del Plan a `#FFFFFF` y `#CCCCCC`.

Hay una adaptación explícita para poder leer los textos: el botón café claro y la navegación gris usan texto oscuro; el botón con fondo oscuro usa blanco. El criterio de referencia para texto normal es un contraste mínimo de 4.5:1. Esta decisión y los valores están documentados en el CSS para que el equipo pueda revisarlos juntos. Véase [WCAG 2.2: contraste mínimo](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html).

La confirmación de desactivación usa el componente compartido `ConfirmacionModalComponent`, basado en `<dialog>`. Abre con `showModal()`, bloquea la interacción con el fondo, coloca el foco en Cancelar y lo devuelve al cerrar. El formulario de B sigue siendo responsable del motivo, la validación y la petición de baja; el componente compartido se ocupa de presentar el diálogo. La API nativa está explicada en [MDN: elemento dialog](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/dialog).

No hace falta copiar un segundo conjunto de colores o construir otro modal para cada compañero. Si el equipo cambia un control común, revisa estos archivos compartidos y las pantallas que lo usan.

## Verificación realizada el 11 de noviembre de 2026

- Se preparó Node 20.11.1 con npm 10.2.4 en la carpeta local de PetMatchCR y `npm ci` terminó correctamente.
- El backend arrancó desde IntelliJ con los perfiles `local,demo-b01` en el puerto 8083. La comprobación de salud devolvió `UP`.
- Desde **Terminal → Run Task → Frontend: iniciar local** se ejecutó la tarea de Visual Studio Code basada en `Iniciar-Frontend.cjs`. Angular terminó de compilar y quedó en modo de observación en `http://127.0.0.1:4200/`. Al recargar el navegador, Luna mostró 9 meses, energía alta, estado activo y la etiqueta Juguetón (demo). La alternativa `Iniciar-Frontend.ps1` también había funcionado desde PowerShell 7; la tarea usa Node para evitar el bloqueo de scripts observado en Windows PowerShell 5, sin cambiar esa política.
- Se retiraron las referencias de IntelliJ a los módulos antiguos `.main` y `.test`, conservando los archivos fuente. Se resolvieron los errores de símbolos Java observados.
- Se agregó la fuente DDL **PetMatchCR migraciones**, con dialecto PostgreSQL. Al abrir V6 por su ruta, IntelliJ actualizó el modelo que antes solo reconocía V1–V5: pasó a mostrar 7 tablas, 1 vista y 15 rutinas. `MascotaRepository` quedó sin errores rojos y reconoció las funciones SQL utilizadas.
- Visual Studio Code abrió el `frontend` de la copia correcta y mostró cero errores. Con autorización del usuario, se verificó en **Workspace Trust** que esa carpeta exacta ya figuraba como confiable; no se amplió la confianza a su carpeta padre.
- La compilación del frontend y sus **28 pruebas** terminaron correctamente en Docker. Esta ejecución verifica el código compartido; no equivale a haber ejecutado esas pruebas en Windows.
- En el navegador, Angular en 4200 consultó el backend local en 8083. Se creó **Prueba diseño B-01** y se comprobó que sus datos permanecieran después de recargar.
- En la confirmación de desactivación, el foco inicial quedó en **Cancelar**. **Escape** cerró el diálogo y devolvió el foco a **Desactivar**. Un motivo vacío mostró el error dentro del diálogo y llevó el foco al campo correspondiente; con un motivo válido, la desactivación se guardó y permaneció después de recargar.
- En una ventana móvil de **390 × 844**, el diálogo midió aproximadamente **337 × 760**, sin desbordamiento horizontal. La Luna original permaneció activa, con 9 meses y energía alta.

La fuente DDL es un **modelo virtual construido a partir de los archivos SQL**; ayuda al editor a reconocer tablas y funciones, pero no se conecta a PostgreSQL ni reemplaza pgAdmin. Su configuración queda en `.idea`, ignorada por Git.

Permanecen alertas conocidas de las inspecciones del editor: cuatro sobre los argumentos de llamadas JDBC `CALL` que incluyen `NULL` para el parámetro de salida, y una en V6 sobre `DROP CONSTRAINT mascota_especie_check`. Esta última consulta un modelo final donde esa restricción ya fue eliminada, mientras que Flyway ejecuta las migraciones en orden. Son avisos de análisis del editor, distintos de errores de compilación Java. No se ocultaron las inspecciones ni se alteró el SQL para eliminarlos; el alta y la desactivación descritas arriba se comprobaron contra PostgreSQL. Los avisos del corrector por palabras en español también se distinguen de errores de código.

Estas comprobaciones acreditan el recorrido descrito con el arranque local y las identidades de demostración. La integración con el acceso real de A continúa siendo un trabajo separado.

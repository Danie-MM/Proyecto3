# Frontend de PetMatchCR: guía de Natanael y José

El objetivo de esta base es comprobar un recorrido completo: escribir en Angular, guardar mediante Java y recuperar el mismo dato desde PostgreSQL. La pantalla `/comprobar-entorno` es una prueba técnica; los módulos de mascotas, usuarios y adopciones se construirán después.

## 1. Entender qué enciende Docker

El archivo `compose.yaml` de la **raíz `Proyecto3`** reúne tres servicios:

| Servicio | Responsabilidad | Cómo lo usás |
| --- | --- | --- |
| `frontend` | Muestra Angular y recibe las acciones de la persona. | Navegador: `http://localhost:4200/comprobar-entorno`. |
| `backend` | Valida peticiones y trabaja con los datos. | Angular lo llama por `/api`; Postman puede usar `http://localhost:8080`. |
| `db` | Conserva los registros en PostgreSQL. | Solo lo llama el backend; no tiene puerto publicado en Windows. |

Docker incluye **Node 20.11.1, npm 10.2.4, Angular 17.3.12, Angular CLI 17.3.17 y TypeScript 5.4.5**. Son las versiones acordadas para esta base académica. Java 21, Spring Boot 3.2.5, Gradle 8.7 y PostgreSQL 15.19 también se ejecutan en Docker. No necesitás instalar ni cambiar Node, Angular, Java o PostgreSQL en Windows para seguir esta guía.

## 2. Preparar y arrancar

Abrí Docker Desktop y esperá a que el motor esté funcionando. Abrí una terminal de PowerShell en la carpeta **`Proyecto3`**, donde está `compose.yaml`. Todos los comandos siguientes se ejecutan ahí.

Si venías usando `backend/compose.dev.yaml`, primero detené esa configuración con la instrucción de [integración](../docs/configuracion-backend.md). Usá una sola configuración a la vez.

Prepará `.env` una sola vez. Este comando conserva un archivo existente y reutiliza la configuración anterior del backend cuando existe:

```powershell
if (-not (Test-Path .env)) {
    if (Test-Path backend/.env) { Copy-Item backend/.env .env }
    else { Copy-Item .env.example .env }
}
docker compose up --build -d --wait
docker compose ps
```

`up` construye y enciende los servicios; `--build` incorpora el código actual; `-d` los deja funcionando; `--wait` espera a que respondan sus comprobaciones de salud. La primera descarga puede tardar varios minutos. En `ps` deben aparecer `db`, `backend` y `frontend` con estado `healthy`.

## 3. Comprobar que las tres partes se comunican

1. Abrí <http://localhost:4200/comprobar-entorno>.
2. Escribí un texto corto y usá la acción para guardarlo. Debés recibir un registro con ID, texto y fecha.
3. Copiá ese ID y usá la consulta por ID. Debe aparecer el mismo registro: la consulta vuelve al backend y a la base.
4. Conservá el ID, ejecutá `docker compose down` y después `docker compose up -d --wait`. Consultá de nuevo el ID para comprobar que los datos se conservan.

Un texto vacío o demasiado largo debe mostrar un mensaje de validación. Un ID válido que no existe debe indicar que no se encontró el registro. Si hay un problema de conexión, la pantalla debe explicarlo y permitir volver a intentar; un mensaje de carga no significa que el dato ya se guardó.

## 4. Reconocer los archivos antes de modificarlos

| Archivo o concepto | Explicación sencilla |
| --- | --- |
| `frontend/Dockerfile` | Receta para crear el entorno del frontend, instalar dependencias y compilarlo. Incluye Chromium para las pruebas. |
| `frontend/package.json` | Declara bibliotecas, versiones y acciones como `start`, `build` y `test:ci`. |
| `frontend/package-lock.json` | Guarda las versiones exactas del conjunto de dependencias. Se comparte en Git junto con `package.json`. Docker lo instala con `npm ci`. |
| `frontend/src/app/app.routes.ts` | Relaciona direcciones del navegador con pantallas; registra `/comprobar-entorno`. |
| Componente de Angular | Organiza la pantalla: datos, botones, mensajes y plantilla. No guarda datos directamente en PostgreSQL. |
| Servicio de Angular | Reúne las llamadas HTTP al backend para que el componente no repita esa lógica. Es diferente de los tres servicios de Docker. |
| `frontend/proxy.conf.json` | Envía `/api` desde Angular a `http://backend:8080`, conservando la ruta. `backend` es el nombre dentro de la red de Docker. |

El recorrido es **pantalla → servicio Angular → `/api` → backend Java → PostgreSQL**. El navegador usa el mismo origen de Angular; no necesita conocer el nombre interno `backend` ni las credenciales de la base. No agregués contraseñas al frontend ni habilités CORS general para resolver la conexión.

## 5. Editar, reconstruir y revisar

Editá los archivos del repositorio con tu editor. Esta configuración copia el código a la imagen; **no sincroniza los cambios automáticamente**. Después de editar, reconstruí:

```powershell
docker compose up --build -d --wait
docker compose exec frontend npm run test:ci
docker compose exec backend ./gradlew test --no-daemon --rerun-tasks
```

La construcción también compila Angular. `test:ci` ejecuta las pruebas del frontend una vez, con Chromium dentro de Docker; el nombre del comando no implica que exista una automatización en GitHub. Las pruebas del backend usan el esquema `pruebas`, separado de los registros manuales. Revisá los resultados: tener los contenedores encendidos no equivale a tener todas las pruebas aprobadas.

Si algo falla, empezá por el servicio indicado en el error:

```powershell
docker compose ps
docker compose logs --tail 100 frontend backend
docker compose logs --tail 50 db
```

Si necesitan una nueva biblioteca, acuerden su nombre y versión antes de agregarla. Ejecuten npm **dentro de Docker**, con los archivos del repositorio montados para conservar los cambios en `package.json` y `package-lock.json`; después reconstruyan y revisen ambos archivos en Git. Un `npm install` ejecutado solamente dentro del contenedor en marcha puede perderse al recrearlo. No borren el lockfile ni cambien las versiones base para resolver un fallo sin revisarlo juntos.

## 6. Dividir el trabajo y cerrar el día

- **Natanael:** mantiene backend, validaciones, SQL y respuestas del contrato; comprueba que guardar y consultar funcionan.
- **José:** mantiene pantallas, formularios, servicio HTTP y mensajes de carga, éxito y error.
- **Ambos:** acuerdan cambios de rutas, nombres y tipos en [el contrato API](../contratos/PetMatchCR_Contrato_API_inicial.yaml) antes de implementarlos, revisan el PR y repiten el recorrido en la computadora del otro. Las dependencias y el Compose común se coordinan entre los dos.

Antes de presentar un avance, revisá `git status` y `git diff`, explicá qué cambió y adjuntá el resultado de las pruebas que realmente ejecutaste. No incluyás `.env`, `node_modules` ni archivos generados de compilación.

Para detener el trabajo usá `docker compose down`. **No agregués `-v`: elimina el volumen con los datos.** La próxima vez podés iniciar con `docker compose up -d --wait`; si cambió el código, agregá `--build`.

La base integrada no completa los módulos A–E ni el inicio de sesión. PostgreSQL todavía requiere confirmación con el docente; los detalles compartidos están en [configuración de integración](../docs/configuracion-backend.md).

La [evidencia del arranque](../docs/evidencia-arranque.md) registra qué se comprobó en la computadora de Natanael y qué falta validar con José.

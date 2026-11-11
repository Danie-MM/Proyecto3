# PetMatchCR

Base común para los cinco bloques de PetMatchCR. El ejemplo de entorno guarda un mensaje desde Angular, lo registra en PostgreSQL a través de Spring Boot y permite consultarlo por ID. Sirve para comprobar la instalación antes de empezar un issue.

El backend usa **Spring Boot 4.1.2, Java 21 y Maven 3.9.16**. El frontend conserva Angular 17.3.12 y la base de datos usa PostgreSQL 15.19.

## Empezar en Windows

1. Cloná este repositorio. Si vas a revisar el PR de la base nueva, seleccioná su rama en GitHub Desktop. Una vez integrado, empezá desde una copia actualizada de `main`.
2. Abrí Docker Desktop con contenedores Linux. En PowerShell, abrí la carpeta que contiene `compose.yaml` y ejecutá:

   ```powershell
   if (-not (Test-Path .env)) { Copy-Item .env.example .env }
   docker compose up --build -d --wait --wait-timeout 240
   ```

3. Abrí [Comprobar entorno](http://localhost:4200/comprobar-entorno), guardá un texto ficticio y copiá el ID. Recargá la página y consultá el mismo ID. Debés recuperar el texto y la fecha de creación.
4. Para detener el entorno y conservar la base, ejecutá `docker compose down`. Para ver el estado y los errores de arranque, usá `docker compose ps` y `docker compose logs --tail 80`.

Cada integrante trabaja con su propia base de desarrollo. Si ya usás los puertos 4200 u 8080, cambiá `FRONTEND_PORT` o `BACKEND_PORT` en tu `.env` y abrí el puerto elegido. En macOS o Linux podés preparar la configuración con `test -f .env || cp .env.example .env`.

## Dónde programar

| Carpeta | Qué va allí |
| --- | --- |
| `backend/pom.xml` | Proyecto Maven para abrir en IntelliJ IDEA con JDK 21. Incluye las dependencias del servidor. El Wrapper permite usar la misma versión de Maven en Windows, Linux y Docker. |
| `backend/src/main/java/cr/petmatchcr/` | Código Java organizado por bloque. Agregá las rutas, reglas y consultas de tu módulo dentro de su paquete. |
| `backend/src/main/resources/db/migration/` | Migraciones SQL numeradas de Flyway. Acordá un número con el equipo y agregá un archivo nuevo para cada cambio. |
| `backend/src/test/java/` | Pruebas Java. |
| `frontend/` | Proyecto Angular para abrir en Visual Studio Code. Las pantallas y sus servicios viven en `src/app/features/`. |
| `contratos/` | Descripción de las rutas y las respuestas compartidas entre frontend y backend. |
| `.github/workflows/` | Comprobaciones que GitHub ejecuta al proponer cambios a `main`. |

## Qué carpeta usa cada integrante

El nombre de la carpeta es el mismo en el backend y en el frontend. Las carpetas de negocio están preparadas para empezar a programar. La implementación de referencia está en `entorno/`.

| Bloque | Carpeta | Responsable | Primer issue |
| --- | --- | --- | --- |
| A · Acceso y sesión | `acceso/` | Danie-MM | [A-01 · Registrar una cuenta individual](https://github.com/Danie-MM/Proyecto3/issues/3) |
| B · Mascotas y veterinaria | `mascotas/` | NatanaelSolis | [B-01 · Crear, consultar y editar el perfil privado](https://github.com/Danie-MM/Proyecto3/issues/6) |
| C · Extravíos y avisos | `extravios/` | Xaverry | [C-01 · Ubicación y preferencias de avisos](https://github.com/Danie-MM/Proyecto3/issues/9) |
| D · Adopciones | `adopciones/` | RicarLacon | [D-01 · Catálogo público de adopción](https://github.com/Danie-MM/Proyecto3/issues/12) |
| E · Campañas y aportes | `campanias/` | GmendezCO | [E-01 · Crear y consultar campañas](https://github.com/Danie-MM/Proyecto3/issues/15) |

Consultá el alcance y las dependencias en el issue. Creá tu rama a partir de la base integrada, desarrollá el módulo y enlazá el issue en tu PR con los pasos para probarlo. El equipo coordina el arranque en [G-02](https://github.com/Danie-MM/Proyecto3/issues/2) y los estados en el [tablero de la primera iteración](https://github.com/users/NatanaelSolis/projects/1/views/1).

## Ejecutar las pruebas

Con el entorno encendido, ejecutá desde la raíz:

```powershell
docker compose exec backend ./mvnw -B -ntp verify
docker compose exec frontend npm run test:ci
```

Maven genera los informes en `target/surefire-reports/`. El flujo del PR también comprueba el alta y la lectura por el proxy de Angular. La aceptación de G-02 incluye que otra persona reproduzca el arranque desde una base vacía y registre su resultado en el issue.

## B-01: perfiles privados de mascotas

Implementa alta, listado, detalle, edición con `versionEsperada` y desactivación lógica con motivo.
Los reenvíos de creación y baja usan `Idempotency-Key`; el servidor deriva responsable y ámbitos.
El catálogo de especies se consulta en PostgreSQL. V1–V5 conservan el trabajo manual; V6 amplía el perfil.

Para recorrerlo localmente, con Docker Desktop encendido y `.env` preparado:

```powershell
docker compose -f compose.yaml -f compose.b01-demo.yaml up --build -d --wait --wait-timeout 240
```

Abrí [Mis mascotas](http://localhost:4200/mascotas) con el puerto predeterminado 4200, o el `FRONTEND_PORT`
que elegiste en tu `.env`. En la computadora de Natanael se usa 4202; ese ajuste no es obligatorio para el equipo.
Si tu instalación necesita un `compose.override.yaml` personal, agregá `-f compose.override.yaml` antes de
`-f compose.b01-demo.yaml`; ese archivo es opcional, local y no se incluye en el repositorio.
Elegí **Responsable de Luna** en el selector de demostración. Luna ya viene como dato inicial.
Para practicar el recorrido completo, creá **Luna prueba B-01**, editá su edad, recargá y desactivá esa copia
desde su detalle. Conservá la Luna inicial activa para otras demostraciones.

La demostración usa el esquema `demo_b01`, actores ficticios y las tres etiquetas locales autorizadas:
Sociable, Tranquilo y Juguetón, identificadas con `(demo)`. No reemplazan el catálogo aprobado del equipo.
`public` conserva los datos de desarrollo normales y `pruebas` se usa para verificaciones automatizadas.
En pgAdmin, abrí **petmatchcr → Schemas → demo_b01 → Tables → mascota** para ver los registros de la demostración.

La sesión real de A y sus organizaciones todavía requieren integración: su rama actual usa email/Long y JWT,
mientras el acuerdo de B usa UUID y sesión con CSRF. Los puertos `IdentidadMascotas` y `AmbitosMascotas`
separan esa conexión; fuera de la demostración no se inventan identidades ni permisos.

- [Explicación del código y recorrido para aprender](docs/B01_PASO_A_PASO.md).
- [Auditoría de las ramas del equipo y pendientes de integración](docs/INTEGRACION_B01.md).
- [Contrato HTTP de B-01](contratos/mascotas.openapi.json).

El funcionamiento local de B no acredita por sí solo la integración con A. El catálogo público de D también
necesita B-02 (publicación); fotos, publicación y clínica quedan en sus respectivos issues.

Validación automatizada del 10 de noviembre de 2026: `mvnw clean verify` completó **130 pruebas de backend**
sin fallos ni errores; el frontend compiló y completó **18 pruebas**. Estos resultados corresponden al módulo
con contextos controlados y no acreditan todavía la integración del ingreso real de A.

La comprobación en el navegador recorrió alta, recarga, edición, conflicto entre dos pestañas,
aislamiento entre responsables y desactivación con conservación del identificador. Luna quedó activa
con 9 meses y energía alta; la mascota ficticia de comprobación quedó desactivada en `demo_b01`.

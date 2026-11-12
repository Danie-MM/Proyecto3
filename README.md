# PetMatchCR

PetMatchCR reúne acceso, perfiles privados de mascotas y preferencias de ubicación.
Esta entrega reúne la revisión de **A + B + C** en la rama
`fix/integracion-abc`, a partir de `main` en `0fec949`. Sus correcciones requieren
la revisión acordada antes de incorporarse a `main`.

La verificación de esta copia completó **160 pruebas backend** sin fallos, errores
ni omisiones y **44 pruebas frontend** con Chrome Headless nativo de Windows.
También se comprobaron los arranques locales del backend y del frontend. El ingreso
real con Google y el recorrido completo con cuentas del equipo siguen pendientes;
estos resultados no significan que todo PetMatchCR esté terminado.

## Obtener la revisión

Si ya tenés el repositorio, guardá tu trabajo en tu propia rama antes de cambiar
de rama. Desde la raíz, inspeccioná el estado y traé las referencias remotas:

```powershell
git status --short --branch
git fetch origin
git branch -a
```

Los siguientes pasos trabajan con la rama remota `origin/fix/integracion-abc`.
Si no aparece, detenete y confirmá su publicación con quien coordina la integración.

Si todavía no existe la rama local:

```powershell
git switch -c fix/integracion-abc --track origin/fix/integracion-abc
```

Si ya existe, usá `git switch fix/integracion-abc`. Después actualizala:

```powershell
git pull --ff-only origin fix/integracion-abc
git log -1 --oneline
```

Si Git informa cambios pendientes o historias divergentes, revisalos antes de
continuar; no uses `reset` para descartarlos. Otra opción es clonar una carpeta
nueva y conservar intacta tu copia anterior:

```powershell
git clone --branch fix/integracion-abc https://github.com/Danie-MM/Proyecto3.git Proyecto3-integracion
cd Proyecto3-integracion
```

## Elegir una modalidad de ejecución

| Modalidad | Uso | Puertos predeterminados |
| --- | --- | --- |
| Editores + PostgreSQL en Docker | Programar Java en IntelliJ y Angular en VS Code. | Angular 4200, API 8083, PostgreSQL 55432. |
| Todo en Docker | Reproducir la aplicación sin configurar Java/Maven localmente. | Angular 4200, API 8080; PostgreSQL permanece en la red de Docker. |

Usá **una modalidad por vez**: las dos comparten el puerto 4200 y la base local.
No mantengás dos backends trabajando sobre el mismo esquema durante una prueba.

En Windows necesitás Git, Docker Desktop encendido con **contenedores Linux** y
**Node 20.11.1 con npm 10.2.4**. Para trabajar desde los editores, agregá **JDK 21**,
IntelliJ IDEA y Visual Studio Code; Chrome permite ejecutar las pruebas frontend.
El repositorio usa Maven Wrapper 3.9.16: no hace falta instalar Maven o Gradle.

Desde la raíz, prepará la configuración local:

```powershell
node --version
npm.cmd --version
node tools/Preparar-Entorno.cjs
```

El preparador crea `.env` desde el ejemplo si falta. Completa únicamente valores
vacíos de `JWT_SECRET` con una clave aleatoria y de `GOOGLE_CLIENT_ID` con el cliente
público del equipo; conserva valores existentes y no imprime secretos. Revisá la
configuración local de PostgreSQL. Si el volumen ya existe, su contraseña debe
coincidir: cambiar `.env` no cambia la contraseña almacenada en PostgreSQL.

### Editores + base en Docker

```powershell
docker compose -f compose.yaml -f compose.ide.yaml up -d db
```

Abrí `backend/pom.xml` en IntelliJ con JDK 21 y `frontend` en VS Code.
En **Terminal > Ejecutar tarea**, iniciá **Backend: iniciar integracion ABC** y
después **Frontend: iniciar local**. Entrá por
[Iniciar sesión](http://127.0.0.1:4200/login).
Los pasos de instalación, verificación y solución de errores están en
[Ejecutar en mi PC](docs/EJECUTAR_EN_MI_PC.md).

### Todo en Docker

Después de preparar `.env`, ejecutá desde la raíz:

```powershell
docker compose -f compose.yaml up --build -d --wait
docker compose -f compose.yaml ps
```

El `-f compose.yaml` explícito evita cargar un `compose.override.yaml` personal.
El backend usa `local,integracion` y el esquema `integracion_abc`. Abrí
[Iniciar sesión](http://127.0.0.1:4200/login). Para revisar fallos o detener sin
eliminar datos:

```powershell
docker compose -f compose.yaml logs --tail 80 backend frontend
docker compose -f compose.yaml down
```

No agregués `-v` al detener. Esta modalidad queda documentada para reproducción;
las 160/44 pruebas indicadas arriba corresponden a la ejecución nativa verificada.

## Google y rutas protegidas

`GOOGLE_CLIENT_ID` es un identificador público, no una contraseña, y debe coincidir
con la constante del frontend. El responsable de la cuenta de Google debe tener
habilitados los orígenes `http://localhost:4200` y `http://127.0.0.1:4200`.
El preparador no cambia Google Cloud Console ni garantiza esa autorización.
Si aparece un error de origen/cliente, coordiná esa configuración con su responsable.

`/mascotas`, `/mi-ubicacion` y `/comprobar-entorno` requieren autenticación.
Empezá por `/login`; no se promete acceso por el antiguo selector de demostración.

## Organización y alcance

| Bloque | Carpeta de código | Responsable | Referencia |
| --- | --- | --- | --- |
| A: acceso y sesión | `acceso/` | Danie-MM | [A-01](https://github.com/Danie-MM/Proyecto3/issues/3) |
| B: mascotas y veterinaria | `mascotas/` | NatanaelSolis | [B-01](https://github.com/Danie-MM/Proyecto3/issues/6) |
| C: ubicación y extravíos | `extravios/` | Xaverry | [C-01](https://github.com/Danie-MM/Proyecto3/issues/9) |
| D: adopciones | `adopciones/` | RicarLacon | [D-01](https://github.com/Danie-MM/Proyecto3/issues/12) |
| E: campañas y aportes | `campanias/` | GmendezCO | [E-01](https://github.com/Danie-MM/Proyecto3/issues/15) |

La integración actual se concentra en A/B/C. D no tiene un catálogo implementado
en la revisión de partida y necesita publicación/proyección pública de B-02.
E permanece fuera de estas correcciones. Organizaciones, criterios originales de
registro, ciclo completo de sesión y recorridos reales requieren su propia evidencia.

- [Guía de integración, pruebas y límites actuales](docs/INTEGRACION_ABC.md).
- [Contrato de perfiles privados](contratos/mascotas.openapi.json).
- [Tablero del equipo](https://github.com/users/NatanaelSolis/projects/1/views/1).

Las guías [B01_PASO_A_PASO.md](docs/B01_PASO_A_PASO.md) e
[INTEGRACION_B01.md](docs/INTEGRACION_B01.md) conservan antecedentes de B aislado.
Sus referencias a `demo-b01`, al selector de Luna y a resultados de 130/18/28
pruebas son **históricas**; no reemplazan el arranque autenticado ni la verificación
actual de A/B/C.

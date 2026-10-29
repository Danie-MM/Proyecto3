# Proyecto3
Curso Proyecto 3

## Arrancar PetMatchCR en tu computadora

**Windows:** descargá y extraé la versión compartida, abrí Docker Desktop y hacé doble clic en **`INICIAR.cmd`**. Para apagar sin borrar datos: **`DETENER.cmd`**. Leé [COMENZAR.md](COMENZAR.md) para los cuatro pasos, requisitos y solución de problemas. Mientras el PR esté en revisión, usá la rama `feature/arranque-backend`.

La base de desarrollo reúne **Angular + Java + PostgreSQL en Docker**. Abrí Docker Desktop y una terminal de PowerShell en la raíz `Proyecto3`. Si antes encendiste `backend/compose.dev.yaml`, detenelo primero como explica [la configuración compartida](docs/configuracion-backend.md). Prepará `.env` solo si falta; si ya trabajabas con el backend, el comando conserva su configuración:

```powershell
if (-not (Test-Path .env)) {
    if (Test-Path backend/.env) { Copy-Item backend/.env .env }
    else { Copy-Item .env.example .env }
}
docker compose up --build -d --wait
docker compose ps
```

Abrí <http://localhost:4200/comprobar-entorno> para guardar y consultar un dato de prueba. Seguí [la guía paso a paso del frontend](frontend/README.md) para entender los archivos, editar y ejecutar pruebas; consultá [la guía del backend](backend/README.md) y [la configuración compartida](docs/configuracion-backend.md) para integrar cambios.

Usá el `compose.yaml` raíz para el trabajo diario. Después de editar código, repetí `docker compose up --build -d --wait`. Para detener: `docker compose down`, **sin `-v` para conservar los datos**. Esta base técnica todavía no implementa los módulos del proyecto.

## PetMatchCR · Organización del desarrollo

Este repositorio contiene las **17 tareas iniciales** del equipo, con entregables, dependencias, criterios de aceptación y forma de presentar los avances en GitHub.

### Accesos del equipo

- **[Tablero de trabajo — General](https://github.com/users/NatanaelSolis/projects/1/views/1)**: Por hacer → En curso → Bloqueado → En revisión → Hecho.
- **[Tareas organizadas por bloque](https://github.com/users/NatanaelSolis/projects/1/views/3)**: módulos, prioridades y estado.
- **[Mis tareas](https://github.com/users/NatanaelSolis/projects/1/views/2)**: muestra lo asignado a la persona que abre la vista.
- **[Issues del repositorio](https://github.com/Danie-MM/Proyecto3/issues)** y **[hito Primera iteración](https://github.com/Danie-MM/Proyecto3/milestone/1)**.

El tablero está administrado por NatanaelSolis; sus tarjetas corresponden a los issues de **Danie-MM/Proyecto3**. Este es también su repositorio predeterminado para crear nuevas tareas.

### Entregas

| Entrega | Fecha | Resultado esperado |
| --- | --- | --- |
| Primera iteración | 14 de noviembre de 2026 | 40–50 % de avance global aceptado por esfuerzo, con frontend, backend y datos integrados. |
| Entrega final | 8 de diciembre de 2026 | Completar y demostrar el alcance acordado del proyecto. |

El porcentaje automático del hito cuenta issues cerrados; no equivale por sí solo al porcentaje de avance global. La aceptación del alcance y los acuerdos técnicos se registran en **[G-01](https://github.com/Danie-MM/Proyecto3/issues/1)**.

### Elegir y repartir los bloques

| Bloque | Trabajo inicial | Tareas |
| --- | --- | --- |
| Común | Acuerdos, responsables y proyecto base ejecutable | [G-01](https://github.com/Danie-MM/Proyecto3/issues/1), [G-02](https://github.com/Danie-MM/Proyecto3/issues/2) |
| A · Acceso | Registro, roles, permisos y sesiones | [A-01](https://github.com/Danie-MM/Proyecto3/issues/3), [A-03](https://github.com/Danie-MM/Proyecto3/issues/5), [A-02](https://github.com/Danie-MM/Proyecto3/issues/4) |
| B · Mascotas | Perfil privado, publicación, fotografía y vacunas | [B-01](https://github.com/Danie-MM/Proyecto3/issues/6), [B-02](https://github.com/Danie-MM/Proyecto3/issues/7), [B-03](https://github.com/Danie-MM/Proyecto3/issues/8) |
| C · Extravíos | Ubicación manual, reportes y avisos | [C-01](https://github.com/Danie-MM/Proyecto3/issues/9), [C-02](https://github.com/Danie-MM/Proyecto3/issues/10), [C-03](https://github.com/Danie-MM/Proyecto3/issues/11) |
| D · Adopción | Catálogo, solicitudes y seguimiento de decisiones | [D-01](https://github.com/Danie-MM/Proyecto3/issues/12), [D-02](https://github.com/Danie-MM/Proyecto3/issues/13), [D-03](https://github.com/Danie-MM/Proyecto3/issues/14) |
| E · Aportes | Campañas, aportes pendientes sin cobro e historial | [E-01](https://github.com/Danie-MM/Proyecto3/issues/15), [E-02](https://github.com/Danie-MM/Proyecto3/issues/16), [E-03](https://github.com/Danie-MM/Proyecto3/issues/17) |

Los responsables están **pendientes de elección**. Al elegir un bloque, registrar la decisión en G-01 y asignar sus issues a la cuenta real de la persona.

### Por dónde empezar

1. **G-01 → G-02 → A-01 → A-03 → A-02** para cerrar acuerdos, tener una base común y habilitar identidad y permisos.
2. Revisar la sección **Qué necesita de los demás** de cada issue antes de integrar. Se pueden preparar pantallas, migraciones y pruebas en paralelo.
3. Priorizar las tareas con **prioridad-P0**: G-01, G-02, A-01, A-02, A-03, B-01 y B-02. Las otras diez tienen **prioridad-P1** y completan los primeros recorridos funcionales.

### Cómo registrar avances

- Mantener el estado del tablero actualizado. Si algo se bloquea, escribir qué falta, de quién depende y el siguiente paso.
- En cada avance, dejar el enlace al commit o PR, qué se completó, qué se probó y qué sigue.
- Usar commits con un cambio coherente y un mensaje claro; enlazar el issue desde el PR.
- Pasar a **En revisión** con los criterios de aceptación comprobados y evidencia. El autor no se autoaprueba.
- Pasar a **Hecho** después de revisión e integración; una pantalla sola no termina una tarea que requiere frontend y backend.
- Adoptar versiones, contratos, nombres de datos, estructura y reglas de ramas en G-01 antes de construir dependencias compartidas.

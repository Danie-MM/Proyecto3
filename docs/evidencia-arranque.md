# Evidencia del arranque local de PetMatchCR

La comprobación más reciente es la integración del frontend del **28 de octubre de 2026**, al final de este documento. El apartado del 27 conserva el resultado histórico del backend.

## Backend: 27 de octubre de 2026

Fecha: 27 de octubre de 2026, Costa Rica. Computadora: Windows de Natanael, Docker Desktop con WSL 2.

Repositorio: `Danie-MM/Proyecto3`. Base al clonar: `c4bf78d45f3a83bb0df745d321d00d82f22e709e` (`main`). Rama local: `feature/arranque-backend`. Esta entrega se verificó con cambios todavía sin commit ni publicación; no existe un commit nuevo que atribuir a las pruebas.

## Resultado observado

| Comprobación ejecutada | Resultado |
| --- | --- |
| `docker compose -f backend/compose.dev.yaml config --quiet` | Configuración válida. |
| Construcción del backend con Docker | Compiló aplicación y pruebas usando Java 21 y Gradle 8.7. |
| Primer arranque con volumen nuevo | Flyway aplicó V1 y Hibernate validó la tabla. |
| Salud | `db` y `backend` saludables; GET `/actuator/health` devolvió 200 y `status: UP`, sin detalles internos. |
| Pruebas JUnit contra PostgreSQL real | **17 pruebas, 0 errores, 0 fallos, 0 omitidas.** |
| POST y GET reales desde Windows | POST 201 y GET 200; UUID, texto recortado y fecha UTC coinciden. |
| Apagar con `down` y levantar con `up -d --wait` | El registro manual conservó ID, texto y fecha. No se eliminó el volumen. |
| Detener únicamente `db` | El backend respondió 503, `codigo: BD_NO_DISPONIBLE`. |
| Volver a encender `db` | Ambos servicios saludables; GET volvió a responder 200 con el mismo registro. |
| Separación de datos | Después de JUnit, `public.prueba_entorno` conservaba solo el registro manual; las pruebas escribieron en `pruebas`. |
| Contrato YAML | Parseado con SnakeYAML, claves duplicadas rechazadas y 807 referencias internas resueltas. No equivale a validar las rutas A–E todavía no implementadas. |
| Configuración local | `.env` ignorado por Git; PostgreSQL sin puerto publicado; backend accesible solo en 127.0.0.1:8080. |

JUnit cubre guardar/consultar, normalización, límite de longitud Unicode, JSON y tipos incorrectos, texto inválido sin inserción, UUID inválido, UUID inexistente, salud sin detalles, bloqueo de otros módulos y ausencia de rutas técnicas fuera de los perfiles local/test.

Registro ficticio usado para persistencia, consultable mientras se conserve este volumen local:

```json
{
  "id": "f6adfe2f-7976-4f90-96d4-aad98d966a58",
  "texto": "Prueba de persistencia Natanael",
  "creadoEn": "2026-10-27T23:28:35.050103Z"
}
```

Las imágenes base se fijaron por digest en Dockerfile/Compose. Se observó Temurin 21.0.12.1, PostgreSQL 15.19, Gradle 8.7 y Docker Engine 29.9.2. El Wrapper JAR coincide con el checksum oficial de Gradle 8.7; la distribución también tiene checksum fijado.

## Cómo repetir la comprobación

Desde `Proyecto3/backend`, con `.env` preparado:

```powershell
docker compose -f compose.dev.yaml up --build -d --wait
docker compose -f compose.dev.yaml exec backend ./gradlew test --no-daemon --rerun-tasks
```

Los reportes JUnit se generan dentro del contenedor en `/app/build/test-results/test/` y el reporte HTML en `/app/build/reports/tests/test/`. Para conservarlos antes de hacer `down`, se pueden copiar a una carpeta local ignorada por Git:

```powershell
New-Item -ItemType Directory -Force build | Out-Null
docker compose -f compose.dev.yaml cp backend:/app/build/reports/tests/test build/reporte-tests
```

Para persistencia, seguir el paso 7 del [README](../backend/README.md) con un registro propio. Para repetir la caída de BD, detener solo `db`, consultar el registro y volver a ejecutar `up -d --wait`; hacerlo cuando nadie esté usando esta composición local.

## Pendiente del equipo

- José: integrar frontend y Compose raíz, revisar esta base y probarla en su computadora.
- Natanael: registrar confirmación del motor PostgreSQL con el docente.
- Ambos: revisar y publicar mediante el flujo de ramas acordado; validar el clon completo en las otras computadoras.
- Bloques A–E: desarrollar los módulos funcionales. Esta prueba de entorno no significa que se completó la primera iteración.

Solo la computadora de Natanael quedó verificada en esta sesión. No se ejecutaron CI, despliegue público, frontend ni pruebas en los otros equipos.

## Frontend integrado: 28 de octubre de 2026

Verificación realizada en la misma computadora, desde la raíz `Proyecto3`, con el nuevo `compose.yaml`. Los cambios siguen locales en `feature/arranque-backend`, sin commit ni publicación. Este apartado actualiza el pendiente anterior de integrar Angular: la base ya está integrada; José debe revisarla y probarla en su equipo.

| Comprobación ejecutada | Resultado |
| --- | --- |
| Versiones dentro del contenedor frontend | Node **20.11.1**, npm **10.2.4**, Angular **17.3.12**, Angular CLI y build-angular **17.3.17**, TypeScript **5.4.5**. |
| `docker compose config --quiet` | Configuración válida. |
| Construcción de la imagen frontend | `npm ci` usa el lockfile; `npm run build` completó la compilación de producción sin errores ni advertencias de tamaño. |
| `docker compose exec -T frontend npm run test:ci` | **20 pruebas Jasmine, 20 aprobadas**, con Chromium dentro de Docker. |
| Estado de los servicios | `db`, `backend` y `frontend` saludables. |
| Guardar desde el navegador | La pantalla recibió el registro real a través del proxy `/api`; eliminó espacios externos y conservó el emoji. Solo confirma éxito si recibe 201 y un cuerpo válido. |
| Consultar desde el navegador | Recuperó el mismo ID, texto y fecha desde la base, con respuesta 200. |
| Consultar UUID inexistente | Mostró «No encontramos ese registro» y retiró el resultado exitoso anterior. |
| Detener únicamente `backend` | La pantalla mostró un mensaje de conexión y volvió a habilitar los controles. Después se restauró el servicio. |
| `docker compose down` y `docker compose up -d --wait` | Se recrearon los tres contenedores sin eliminar el volumen. La pantalla volvió a recuperar el registro guardado. |
| Datos anteriores | El registro del 27 de octubre permanece intacto. `public.prueba_entorno` conserva los dos registros manuales, el anterior y el nuevo. |
| Configuración local | `.env` y `backend/.env` ignorados por Git; frontend publicado solo en 127.0.0.1:4200; versiones de Node/npm de Windows no utilizadas. |

Las 20 pruebas cubren contrato HTTP, estados 201/200, rechazo de respuestas inesperadas, errores 400/404/503, respuestas HTML del proxy, desconexión, límite de espera de 10 segundos, ausencia de reenvíos automáticos, texto Unicode, UUID, bloqueo de envíos duplicados y limpieza de resultados anteriores. Las pruebas HTTP de Jasmine simulan respuestas; las comprobaciones manuales anteriores sí recorren Angular, Spring y PostgreSQL reales.

Registro de prueba creado desde la pantalla:

```json
{
  "id": "c17f23d4-50f8-4473-8a29-a57bb1bc4561",
  "texto": "Prueba Angular y Docker del equipo 🐾",
  "creadoEn": "2026-10-29T00:42:02.973872Z"
}
```

La fecha del registro está en UTC; corresponde al 28 de octubre en Costa Rica. Para repetir el recorrido y las pruebas, seguir [la guía del frontend](../frontend/README.md). Para el trabajo actual se usa el Compose raíz, no el comando histórico de backend aislado.

Alcance: base de desarrollo local y pantalla técnica. Los módulos A–E, login, CI, despliegue público, publicación en GitHub y comprobación en otras computadoras siguen pendientes. No se volvieron a ejecutar las 17 pruebas Java en esta integración; su resultado previo se conserva en el apartado del 27, y el código Java no cambió. PostgreSQL sigue pendiente de confirmación docente.

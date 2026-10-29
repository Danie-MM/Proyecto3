# Configuración compartida de Natanael y José

El **`compose.yaml` de la raíz `Proyecto3` es la configuración habitual del equipo**. Reúne Angular, backend y PostgreSQL. Empezá por [la guía del frontend](../frontend/README.md); [la guía del backend](../backend/README.md) explica Java y las pruebas de la API. Esta base comprueba la comunicación entre las tres partes; los módulos de negocio quedan pendientes.

| Elemento | Valor acordado |
| --- | --- |
| Node / npm | 20.11.1 / 10.2.4, dentro de Docker |
| Angular / Angular CLI / TypeScript | 17.3.12 / 17.3.17 / 5.4.5 |
| Java / Spring / Gradle | 21 / 3.2.5 / Wrapper 8.7 |
| Imagen frontend | `node:20.11.1-bookworm-slim`, digest fijado en Dockerfile |
| Imagen backend | `eclipse-temurin:21-jdk-jammy`, digest fijado en Dockerfile |
| Imagen base de datos | `postgres:15.19-bookworm`, digest fijado en Compose |
| Servicios de Compose | `frontend`, `backend`, `db` |
| Proyecto Compose | `petmatchcr-backend`, conservado para reutilizar los datos anteriores |
| Base / usuario local | `petmatchcr` / `petmatch_app` |
| URL JDBC dentro de Docker | `jdbc:postgresql://db:5432/petmatchcr` |
| Puerto de backend en Windows | `127.0.0.1:8080:8080` |
| Puerto de frontend en Windows | `127.0.0.1:4200:4200` |
| Puerto de PostgreSQL | Solo interno 5432, sin publicar en Windows |
| Volumen lógico | `petmatchcr_db:/var/lib/postgresql/data` |
| Volumen físico | `petmatchcr-backend_petmatchcr_db` |
| Perfil de desarrollo | `local` |

## Arranque y configuración local

Desde la raíz `Proyecto3`, prepará `.env` si todavía no existe. Si tenés `backend/.env`, copialo para conservar su contraseña; si es la primera instalación, usá `.env.example`. No sobrescribás un `.env` existente.

Si aún está funcionando la configuración anterior, antes de arrancar ejecutá `docker compose --env-file backend/.env -f backend/compose.dev.yaml down`. Conserva los datos y evita mezclar configuraciones.

```powershell
if (-not (Test-Path .env)) {
    if (Test-Path backend/.env) { Copy-Item backend/.env .env }
    else { Copy-Item .env.example .env }
}
docker compose up --build -d --wait
docker compose ps
```

Compose envía estas variables al backend:

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/petmatchcr
SPRING_DATASOURCE_USERNAME: petmatch_app
SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
SPRING_PROFILES_ACTIVE: local
```

`DB_PASSWORD` sale de `.env` y también se envía a `db` como `POSTGRES_PASSWORD`. El ejemplo `petmatch_solo_local` es ficticio y exclusivo de esta práctica; la variable nunca va a Angular. Cambiar `.env` después de inicializar un volumen no cambia automáticamente la contraseña que PostgreSQL ya tiene guardada.

El archivo anterior `backend/compose.dev.yaml` queda como alternativa para trabajar solamente backend y BD. **No mezclar ambas configuraciones simultáneamente:** comparten nombre de proyecto, servicios y volumen. Si la anterior está encendida, detenela antes de pasar al Compose raíz:

```powershell
docker compose --env-file backend/.env -f backend/compose.dev.yaml down
docker compose up --build -d --wait
```

`down` conserva los datos. Mantener el nombre `petmatchcr-backend` y el volumen permite reutilizarlos. Cambiar el nombre del proyecto produciría otro volumen vacío. No usar `down -v` para detener normalmente.

## Cómo se conectan las tres partes

1. El navegador abre `http://localhost:4200/comprobar-entorno`.
2. El servicio de Angular solicita `/api/v1/pruebas-entorno` o la consulta por ID.
3. El proxy de desarrollo envía la ruta completa a `http://backend:8080` dentro de Docker.
4. Java valida los datos y llama a PostgreSQL en `db:5432`.

No llamar desde el navegador al nombre interno `backend` ni a PostgreSQL.

El backend no habilita CORS general. El proxy de Angular mantiene el mismo origen para el navegador. El bloque A implementará sesión y CSRF según el contrato vigente; esta base no incluye login ni JWT.

## Salud, datos y contrato

- `db`: `pg_isready -U petmatch_app -d petmatchcr`.
- `backend`: `curl -fsS http://localhost:8080/actuator/health`.
- `frontend`: petición HTTP desde Node a `/comprobar-entorno` en su puerto 4200.
- Intervalo 10 s, timeout 5 s, inicio 60 s, 12 intentos. El backend espera a que la BD esté saludable y el frontend espera al backend. Un estado `healthy` permite arrancar; guardar y recuperar un dato confirma la integración completa.
- Flyway aplica `V1__crear_prueba_entorno.sql`; Hibernate valida la tabla. Escribir mediante `create_prueba_entorno_pr` y leer mediante `ret_id_prueba_entorno_pr`.
- POST técnico recibe `texto`; devuelve 201 con UUID, texto recortado y fecha UTC. GET por UUID devuelve 200 o 404. Texto/JSON/UUID inválidos producen 400; conexión a BD caída produce 503 mientras Spring responda.
- Los errores de la API contienen `codigo`, `mensaje`, `erroresCampos` y `trazaId`. Ver [contrato](../contratos/PetMatchCR_Contrato_API_inicial.yaml). Una caída del servidor o del proxy también puede producir una respuesta distinta; la pantalla debe mostrar un error de conexión comprensible.
- Solo las rutas técnicas están disponibles en `local` y `test`. Los módulos A–E del contrato siguen siendo alcance futuro.

Después de editar, desde la raíz:

```powershell
docker compose up --build -d --wait
docker compose exec frontend npm run test:ci
docker compose exec backend ./gradlew test --no-daemon --rerun-tasks
```

No hay sincronización automática de archivos: los cambios requieren reconstrucción. Las pruebas Java usan `currentSchema=pruebas`, con Flyway y Hibernate en ese esquema, sin limpiar `public`. El dato manual debe sobrevivir a `down` / `up` sin `-v`.

## Confirmación pendiente

Estas versiones fijan el entorno académico acordado. Natanael debe confirmar con el docente PostgreSQL: el acuerdo del grupo lo usa, pero los materiales de clase incluyen también MySQL/MariaDB. Esta implementación no convierte esa elección provisional en un requisito docente confirmado.

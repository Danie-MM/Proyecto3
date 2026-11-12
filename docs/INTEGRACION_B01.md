# Integración de B-01 con el equipo

Este documento registra el contrato que ofrece el módulo de mascotas y la revisión remota del **10 de noviembre de 2026, hora de Costa Rica**. Las ramas de compañeros se consultaron sin integrarlas ni modificarlas. Que una prueba local use actores ficticios no demuestra conexión con el ingreso real de A.

## Qué ofrece B-01

El [contrato HTTP](../contratos/mascotas.openapi.json) describe alta, listado privado paginado, detalle, edición con `versionEsperada`, desactivación lógica e idempotencia. El servidor deriva el responsable del contexto autorizado; el cliente no puede elegirlo ni enviar disponibilidad, datos clínicos o una fotografía en estas operaciones.

La creación fija `NO_DISPONIBLE`. La desactivación conserva el identificador y las relaciones, registra motivo y actor y no publica ni reabre una adopción. El catálogo de especies se consulta en PostgreSQL: agregar una especie activa no exige cambiar un enum Java. Una especie o etiqueta retirada puede conservarse en un perfil que ya la tenía, pero no asignarse de nuevo a otro perfil.

Los puertos Java `IdentidadMascotas` y `AmbitosMascotas` son la frontera con A. El primero entrega un `Optional<UUID>` de usuario vigente; el segundo consulta las organizaciones que ese usuario puede administrar y sus nombres. Se consultan en cada operación. Sin una identidad verificable se rechaza el acceso, aunque exista un principal autenticado cuyo nombre tenga forma de UUID. En una mascota organizativa, revocar el ámbito también revoca el acceso del representante que la creó.

## Estado remoto comprobado

| Parte | Rama y revisión | Resultado observado |
|---|---|---|
| Base | [`main`, `3423b7e`](https://github.com/Danie-MM/Proyecto3/commit/3423b7ef59060c3e7e21ab62bbca1e948eddaa93) | Último PR integrado: [#22](https://github.com/Danie-MM/Proyecto3/pull/22), base Maven. |
| A, Daniela | [`daniela-dev`, `e7b9671`](https://github.com/Danie-MM/Proyecto3/commit/e7b9671b532553bd5c15de256d408f70de488747) | Once commits sobre main; último 10/nov a las 15:57 CR. Ingreso Google, JWT y pantallas. |
| C, José | [`xaverry-dev`, `a9d12b9`](https://github.com/Danie-MM/Proyecto3/commit/a9d12b93a57adceedf320973b7db0f3fe7dfbb84) | C-01: preferencias de ubicación; 10/nov a las 20:26 CR. Todavía no es el consumidor de B para reportes. |
| D, Ricardo | [`ricardo-dev`, `b5d1cb0`](https://github.com/Danie-MM/Proyecto3/commit/b5d1cb0d466b8cfe7782de76c148dcd69c15b621) | Base anterior; no se encontró implementación de adopciones en las ramas publicadas revisadas. Esto no permite concluir nada sobre su trabajo local. |
| E, Greivin | [`GMENDEZCO-dev`, `ef4dcc5`](https://github.com/Danie-MM/Proyecto3/commit/ef4dcc52c333b6395c38f7daf3bb92fd2ad56bbc) | Campañas y cambios en Maven; dos commits sobre main. |

No se encontraron PR nuevos de A, C o D. En las ramas revisadas existe `contratos/entorno.openapi.json`, pero no el `contratos/openapi.yaml` de negocio mencionado por los issues. El estado cerrado de [B-01, issue #6](https://github.com/Danie-MM/Proyecto3/issues/6), desde octubre, no sustituye la evidencia de sus criterios.

## Decisiones necesarias para conectar A

1. **Identificadores:** A publica `Usuario.id` como `Long` y `usuarios.id` como `BIGINT`; B y C usan UUID. El principal que instala A contiene el email, no el UUID del usuario. El equipo debe acordar el identificador compartido y su migración. No convertir un número o correo a UUID arbitrariamente ni tomar el responsable desde el formulario.
2. **Ingreso y sesión:** A expone `/api/auth/registro`, `/api/auth/login` y `/api/auth/reactivar`, devuelve `{token, usuario}`, usa JWT Bearer y guarda el token en `localStorage`. Su servidor es stateless y deshabilita CSRF. El [issue A-02](https://github.com/Danie-MM/Proyecto3/issues/4) todavía describe cookie de sesión y CSRF. Hay que registrar cuál contrato se adopta y ajustar el adaptador y las pruebas. La demostración local de B no reemplaza esa decisión.
3. **Vigencia:** A sí consulta `findByEmailAndActivoTrue` al autenticar cada petición. Falta la organización y la representación vigentes que exige [A-03](https://github.com/Danie-MM/Proyecto3/issues/5). El rol global `REFUGIO` no identifica por sí solo una organización ni autoriza sus mascotas.
4. **Configuración compartida:** A modifica `SeguridadHttp`, `pom.xml`, `frontend/src/main.ts`, rutas y pantalla de entorno. Al integrar hay que combinar esos cambios con los de B. Si se consumen APIs desde otro origen, CORS deberá admitir `Idempotency-Key`; la configuración publicada de A solo permite `Authorization`, `Content-Type` y `Accept`.

Referencia del código auditado: [Usuario](https://github.com/Danie-MM/Proyecto3/blob/e7b9671b532553bd5c15de256d408f70de488747/backend/src/main/java/cr/petmatchcr/acceso/entities/Usuario.java), [UserDetailsServiceImpl](https://github.com/Danie-MM/Proyecto3/blob/e7b9671b532553bd5c15de256d408f70de488747/backend/src/main/java/cr/petmatchcr/security/UserDetailsServiceImpl.java) y [SeguridadHttp](https://github.com/Danie-MM/Proyecto3/blob/e7b9671b532553bd5c15de256d408f70de488747/backend/src/main/java/cr/petmatchcr/config/SeguridadHttp.java).

## Migraciones que actualmente chocan

| Módulo | Archivo publicado o local |
|---|---|
| A | `V2__crear_usuarios.sql` |
| B | `V2__etiqueta_comportamiento.sql` |
| C | `V2__crear_preferencia_ubicacion.sql` |

Flyway no permite tres migraciones con la misma versión. Antes de integrar las ramas se debe acordar una secuencia única y revisar qué versiones ya aplicó cada entorno. No renombrar ni editar una migración aplicada sin preparar la transición correspondiente. En esta entrega se conservan V1–V5 y los cambios nuevos de perfil privado se agregan en V6.

## Entrega a C: reportes de extravío

C debe consumir el contrato interno `MascotasParaExtravios`, no el detalle privado HTTP ni el catálogo de adopción:

```java
void comprobarResponsable(UUID mascotaId);
ResumenMascotaExtravio consultarResumenParaExtravio(UUID mascotaId);
```

El resumen contiene únicamente `mascotaId`, `nombre` y `especie`. Requiere identidad vigente, acceso al ámbito y que el actor sea el responsable actual. Otro representante puede gestionar el perfil en B, pero eso no le permite suplantar al responsable para C. Una mascota privada `NO_DISPONIBLE` puede producir el resumen; una desactivada no. Un recurso ajeno y uno inexistente devuelven el mismo rechazo.

C-01 publica una implementación de usuario fijo exclusivamente bajo perfil `dev`; no se reutiliza como identidad real de B. El [issue C-02](https://github.com/Danie-MM/Proyecto3/issues/10) es el que consume este contrato para reportes.

## Entrega a D: catálogo de adopción

[D-01](https://github.com/Danie-MM/Proyecto3/issues/12) necesita dos entregas diferentes: B-01 aporta perfiles persistidos y catálogos compartidos; **B-02 aporta publicación, disponibilidad y proyección pública**. D no debe consultar ni devolver `MascotaPrivadaRespuesta` para construir el catálogo.

La futura consulta pública debe seleccionar únicamente mascotas activas, publicadas y `DISPONIBLE`, mantener paginación `pagina`, `limite`, `totalElementos` y compartir los UUID de comportamiento. Las rutas públicas indicadas por el issue son `/api/v1/catalogo/mascotas` y `/api/v1/mascotas/{mascotaId}/publica`; B-01 no las presenta como ya implementadas.

## Qué demuestran las pruebas de B

Las pruebas HTTP recorren controlador, seguridad, servicio, repositorio y PostgreSQL en el esquema `pruebas`; usan adaptadores de identidad y ámbitos definidos exclusivamente en clases de test. Comprueban persistencia, límites, campos reservados, aislamiento, pérdida de identidad o representación, edición por versión, bajas e idempotencia. Las pruebas del contrato C verifican la proyección mínima y sus permisos. Las de entorno restringido verifican que no se active la demostración ni se acepte un principal UUID sin el adaptador de identidad.

Ejecución automatizada del 10 de noviembre de 2026: `mvnw clean verify` completó **130 pruebas de backend, sin fallos ni errores**; Angular compiló y completó **18 pruebas de frontend**, incluida la conservación de un reintento pendiente cuando cambia el actor. Estos resultados no validan el login de A ni la interfaz de D. El siguiente paso de integración es resolver identificadores, sesión y migraciones, conectar el adaptador real de A y repetir los recorridos con cuentas y ámbitos del equipo.

En el navegador se comprobó el recorrido local sobre `demo_b01`: crear desde el formulario y recargar, editar a Luna y recargar, rechazar una edición antigua desde otra pestaña conservando sus campos, impedir acceso de otro responsable y desactivar una mascota de prueba conservando su identificador. También se verificó que el representante del Refugio A solo recibiera su ámbito y perfiles autorizados. Las identidades y los catálogos de esa demostración son ficticios.

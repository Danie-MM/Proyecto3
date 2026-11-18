# Verificación de B-02 y B-03

La entrega parte de `main` en `a024c107` y está en la rama local `natanael/b02-b03`. B-02 contiene publicación y fotografía. B-03 contiene vacunas y permisos clínicos. El contrato HTTP y los recorridos están en `contratos/mascotas.openapi.json`, `docs/B02_PUBLICACION.md` y `docs/B03_VACUNAS.md`.

Al revisar GitHub, `main` ya incorporaba el catálogo de Ricardo mediante el PR #28. Daniela tenía tres commits de acceso y sesiones pendientes de integración, Xaverry dos de extravíos y GMENDEZCO cinco de campañas sobre una base anterior. `NATANAEL-DEV` estaba 28 commits detrás de `main`. Las ramas de A y C usaban V11 para migraciones distintas. B usa V14 y V15. La consulta final del remoto volvió a confirmar `main` en `a024c107`. La consulta se hizo por la conexión de GitHub, porque el shell aislado rechazó la resolución de GitHub durante `git fetch`.

## Resultados del 18 de noviembre de 2026

| Comprobación | Resultado observado |
| --- | --- |
| Suite completa backend desde IntelliJ, con JDK 21 y PostgreSQL 15.19 | 192 pruebas aprobadas, 0 fallos. |
| B-02 en una revisión que contiene únicamente ese ticket, Maven Wrapper y JDK 21 | 12 pruebas de validación de fotografías, servicio, seguridad HTTP y organización demostrativa aprobadas. |
| Empaquetado backend con Maven Wrapper y JDK 21 | `mvnw.cmd -o -B -ntp -DskipTests package` completó `BUILD SUCCESS`. Las pruebas completas se ejecutaron por separado desde IntelliJ. |
| Suite Angular original, compilada con AOT y ejecutada con Jasmine en Chromium Edge WebView2 | 69 pruebas aprobadas, 0 pendientes. |
| Compilación Angular de producción | Completada. Advertencia existente de dependencia CommonJS de Leaflet en C. |
| API local arrancada desde IntelliJ con JDK 21 | `GET http://127.0.0.1:8083/actuator/health` devolvió `UP` desde el navegador del equipo. |
| Angular iniciado como aplicación Node.js de IntelliJ | `http://localhost:4200/mascotas` mostró la pantalla y el selector de actores en el navegador integrado de VS Code. |
| B-02 desde la pantalla del representante | Nube se publicó y guardó una foto PNG que persistió al recargar. Un JPEG de contenido falso mostró el rechazo y conservó la miniatura anterior. Se comprobó el ocultamiento y el retiro de foto. El retiro se comprobó antes de cargar las fotografías de muestra de la revisión visual. |
| B-03 desde las pantallas del responsable y el veterinario | Se registró «Antirrábica de prueba QA», con fecha 17/11/2026. Persistió una sola vez al recargar y el detalle mostró el origen del responsable. El veterinario con lectura pudo consultar y recibió un rechazo al intentar registrar. Tras la revocación, su siguiente consulta quedó bloqueada y se ocultó la información clínica. |

Las pruebas de PostgreSQL incluyen cuatro casos de publicación y fotografía, cinco casos clínicos y el recorrido demostrativo que concede un permiso, registra una vacuna y lo revoca. Se verifican reenvíos, conflictos de versión, autorización vigente, pérdida de visibilidad y reversión de la transacción si falla el almacenamiento o la auditoría. Los datos usados son ficticios.

Los informes completos de JUnit están en el historial local de IntelliJ. La suite Angular se ejecutó desde un archivo de pruebas autocontenido con el motor Chromium del equipo. El entorno aislado rechazó las conexiones de Chrome Headless con Karma. La instalación limpia `npm ci` quedó sin verificar por falta de acceso al registro y 29 paquetes ausentes de las cachés. El build y las pruebas usaron las dependencias locales ya instaladas. Se conservaron los manifiestos y el archivo de bloqueo del equipo.

## Entorno de demostración

La configuración local de IntelliJ **PetMatchCR B demo** usa los perfiles `local,demo-b01` y el esquema `pruebas_demo_natan_b0203_manual`. Para reproducir esa configuración, estas propiedades deben tener el mismo valor de esquema.

| Propiedad JVM | Valor |
| --- | --- |
| `spring.datasource.hikari.schema` | `pruebas_demo_natan_b0203_manual` |
| `spring.flyway.schemas` | `pruebas_demo_natan_b0203_manual` |
| `spring.flyway.default-schema` | `pruebas_demo_natan_b0203_manual` |
| `spring.jpa.properties.hibernate.default_schema` | `pruebas_demo_natan_b0203_manual` |

Se eligió un esquema nuevo porque el esquema anterior `demo_b01` contenía una migración V11 distinta. El volumen, los datos y el historial anteriores se conservaron. La demostración proporciona los actores de ensayo, la organización responsable y la auditoría clínica de B. La integración productiva requiere que A implemente el padrón organizativo, la habilitación veterinaria y su auditoría común. Los puertos de B ya permiten consumir esos servicios.

## Cambios compartidos

- `SeguridadHttp.java` permite los dos GET públicos de B-02. Una prueba usa la cadena de seguridad y el filtro JWT reales y comprueba las lecturas anónimas y la protección de las rutas privadas.
- `app.routes.ts` usa el guard de B para la entrada de mascotas. En desarrollo y loopback, ese guard permite mostrar el selector si el servidor confirma que ofrece la demostración. El guard original de acceso sigue resolviendo las sesiones reales.
- `catalogo.component.spec.ts` prepara HTTP y Router de prueba y responde a la carga inicial con datos ficticios. El fallo de proveedor ausente estaba en la revisión base del catálogo.

El clon original conservó su rama `fix/integracion-abc`, commit `14da960`, la migración de comportamientos y la prueba local sin seguimiento. Los cambios implementados se encuentran en la copia abierta en ambos editores.

Al terminar quedó abierto `http://localhost:4200/mascotas` en VS Code con el actor ficticio **Responsable de Luna**. IntelliJ mantiene las configuraciones locales **PetMatchCR B demo** y **Angular B local** ejecutándose en 8083 y 4200. PostgreSQL continúa disponible en Docker en 55432. La vacuna de prueba se conserva y el permiso clínico de ensayo quedó revocado.

## Paleta y datos para la revisión visual

Todo el bloque B usa azul y coral en navegación, listado, formularios, detalle, publicación, expediente y diálogo de desactivación. La paleta principal es #245B78, hover #19445C, fondo #F5F8FC, texto #233C4B, secundario #F9DED5 y acento #E29782. Las miniaturas consultan la API privada y liberan su imagen al cambiar de sesión.

La demostración incluye Luna, Milo, Kira, Sol, Nube y Coco, con fotografías incluidas en el proyecto. El responsable de Luna ve tres tarjetas con imágenes diferentes. Se añaden seis vacunas de ejemplo y se conserva la vacuna QA y el historial de permisos que ya existía. Las cargas sucesivas conservan las fotos que se reemplazan o retiran y los permisos revocados. Las fuentes están en `docs/FOTOS_DEMO.md`.

La revisión posterior amplió la suite de 187 a 192 casos backend y de 66 a 69 casos Angular. Los casos nuevos comprueban las miniaturas, el perfil de activación, la carga inicial y la conservación de los datos al reiniciar. Los pools de las dos clases de pruebas de demo de B se limitan a dos conexiones para permitir que la suite completa conviva dentro del límite de PostgreSQL.

La comprobación final de la carga confirmó por la API privada las seis fotografías y su coincidencia SHA-256 con los recursos correctos de gatos y perros. Luna conserva cuatro registros clínicos contando la vacuna QA previa. La paleta y las tres tarjetas se observaron en la ventana real. Después del cambio de entorno, el controlador de ventanas dejó de iniciar por una ruta Linux inválida en Windows. Se completaron las cargas pendientes mediante los endpoints normales de B, con sesión, CSRF, versión e idempotencia. La recarga visual final de esa ventana quedó pendiente del controlador.

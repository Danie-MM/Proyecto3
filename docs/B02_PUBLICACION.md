# B-02 · Publicación y fotografía

El perfil privado de **Mis mascotas → Ver perfil** contiene la sección **Publicación y fotografía**. El cambio usa la versión de B-01, actualiza el perfil persistido y mantiene separadas las respuestas pública y privada. El contrato HTTP está en `contratos/mascotas.openapi.json`. [Issue #7](https://github.com/Danie-MM/Proyecto3/issues/7)

## Prueba manual

### Demostración con Nube

1. Levantá PostgreSQL como indica `EJECUTAR_EN_MI_PC.md`. En IntelliJ detené el backend que esté usando el puerto 8083 y ejecutá **PetMatchCR B demo**. Esta configuración activa `local,demo-b01` y usa el esquema separado `pruebas_demo_natan_b0203_manual`, preparado para esta revisión.
2. Desde la carpeta `frontend` de esa misma copia, iniciá Angular con la tarea **Frontend: iniciar local** de VS Code. Abrí una ventana privada, sin sesión Google, en `http://localhost:4200/mascotas`.
3. En **Usuario de prueba**, elegí **Representante del Refugio A**. En **Mis mascotas**, abrí **Ver perfil** de **Nube**. Su identificador ficticio es `10000000-0000-4000-8000-000000000003`. En **Publicación y fotografía**, seleccioná una imagen JPEG o PNG y pulsá **Guardar fotografía**. Si Nube ya tenía una foto de una práctica anterior, el botón dice **Reemplazar fotografía**.
4. Pulsá **Publicar mascota**. En otra pestaña abrí el [perfil público de Nube](http://localhost:4200/api/v1/mascotas/10000000-0000-4000-8000-000000000003/publica) y luego su [fotografía pública](http://localhost:4200/api/v1/mascotas/10000000-0000-4000-8000-000000000003/foto/publica). El perfil debe identificar a **Refugio A (demo)** y la foto debe abrir sin credenciales. Si ya estaba publicada, podés comenzar por **Ocultar del catálogo** y volver a publicarla.
5. Volvé al perfil privado, pulsá **Ocultar del catálogo** y recargá las dos URLs públicas. Ambas deben devolver 404. Desde **Ver perfil**, la fotografía privada continúa disponible hasta que pulsés **Retirar fotografía**.

Estos actores solo existen en la demostración local. Con **PetMatchCR B local** se usa la identidad real del bloque A y el esquema de integración del equipo.

### Integración con A

Iniciá el proyecto en la modalidad de editores descrita en `EJECUTAR_EN_MI_PC.md`. Entrá en `http://localhost:4200/login` con una cuenta de prueba registrada mediante el acceso de A. Usá una mascota de esa cuenta. Para publicar, usá un representante vigente de su organización responsable activa. El usuario y la organización de ejemplo en las pruebas automatizadas son ficticios.

| Acción | Resultado esperado |
| --- | --- |
| Elegir un PNG o JPEG válido, pulsar **Guardar fotografía** y recargar el perfil | Se conserva la imagen. El límite inclusive es 5 000 000 bytes. |
| Reemplazarla por un archivo de texto renombrado a `.jpg` | Aparece el rechazo. El perfil y la imagen anterior siguen disponibles al recargar. |
| Abrir el mismo perfil en dos pestañas y cambiarlo en una | Guardar desde la otra con la versión anterior devuelve 409 y ofrece **Recargar perfil**. |
| Como representante, pulsar **Publicar mascota** y abrir el recurso público | El visitante recibe los campos públicos. La foto pública requiere que el perfil siga publicado. |
| Guardar la URL pública de la foto, pulsar **Ocultar del catálogo** y volver a abrir esa URL | Se obtiene 404. El responsable todavía puede consultar la foto privada. |
| Pulsar **Retirar fotografía** y recargar | La mascota se conserva y la fotografía deja de estar disponible. |

En una mascota particular, el control de publicación indica que se necesita una organización activa. La edición ordinaria rechaza el estado `ADOPTADA`.

## Consumo desde D

Un consumidor Java puede inyectar `MascotasParaAdopciones`. Para comprobar la disponibilidad actual llama a `esPublicable(mascotaId)`. Para renderizar un perfil llama a `consultarPerfilPublico(mascotaId)` y maneja que la disponibilidad haya cambiado. La proyección incluye la organización pública y la URL pública opcional de foto. En HTTP las lecturas son `GET /api/v1/mascotas/{mascotaId}/publica` y `GET /api/v1/mascotas/{mascotaId}/foto/publica`. La lista privada de B-01 mantiene su propia autorización.

La publicación necesita que A exponga la organización activa, su nombre y los ámbitos vigentes del usuario. `AmbitosMascotas` permite esa integración. Sin el padrón real el servicio deniega la publicación. Las pruebas de integración de B aportan una organización ficticia y simulan su pérdida de vigencia.

## Evidencia reproducible

- `FotografiaVerificadaTest` cubre contenido JPEG/PNG, archivos inválidos, límite y privacidad de metadatos. `PublicacionServiceTest` cubre reglas del servicio y reenvío.
- `OrganizacionesDemoPublicacionTest` comprueba que el representante pueda publicar a Nube con el padrón de los actores de demostración y que una organización desconocida no se considere activa.
- `PublicacionIntegrationTest` comprueba el flujo HTTP, la persistencia, la visibilidad y los conflictos con PostgreSQL. Las pruebas solo escriben y limpian sus propios datos en un esquema de prueba.
- `publicacion-mascota.component.spec.ts` verifica la interacción del formulario con el contrato HTTP. Los mensajes del servicio de pruebas no acreditan por sí solos la integración de la organización real.

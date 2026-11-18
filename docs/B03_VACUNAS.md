# B-03 · Vacunas y permisos clínicos por mascota

El expediente es privado. Esta iteración admite el evento `VACUNA` y el fundamento `VETERINARIO_AUTORIZADO`. La identidad de la sesión fija el autor y el origen. `V15__expediente_y_permisos_clinicos.sql` persiste vacunas, permisos y claves de reenvío. Las seis operaciones HTTP están en `contratos/mascotas.openapi.json`. [Issue #8](https://github.com/Danie-MM/Proyecto3/issues/8)

## Demostración local con titulares y veterinario de prueba

1. Con PostgreSQL disponible en Docker y Angular en el puerto 4200, abrí en IntelliJ la configuración local **PetMatchCR B demo** y ejecutala como único backend en 8083. Si necesitás reproducirla, usá los perfiles Spring `local,demo-b01` y el esquema aislado `pruebas_demo_natan_b0203_manual`. La configuración local de esta PC ya fija Hikari, Flyway y Hibernate a ese esquema.
2. Abrí `http://localhost:4200/mascotas` en una ventana privada del navegador, sin una sesión Google previa. En **Usuario de prueba** elegí **Responsable de Luna**. En su tarjeta elegí **Abrir expediente**.
3. Registrá una vacuna ficticia con fecha de ayer y elegí **Guardar vacuna**. Recargá y abrí **Ver detalle** para comprobar el autor y el origen. En **Autorizar veterinario**, pegá el identificador profesional de la tabla, elegí **Solo lectura** y pulsá **Conceder permiso**.
4. Elegí **Veterinario de prueba**. En **Mis mascotas → Acceso veterinario**, pegá el identificador de Luna y elegí **Consultar expediente**. Probá consultar y después guardar una vacuna. El permiso de lectura permite la consulta y la API responde 403 al intento de escritura.
5. Volvé a **Responsable de Luna**. Revocá el permiso con motivo y concedé uno nuevo de **Lectura y escritura**. Como **Veterinario de prueba**, volvé a abrir el expediente, guardá otra vacuna y verificá en su detalle el origen veterinario.
6. Para probar un formulario abierto, usá dos sesiones de navegador independientes sin JWT de Google (por ejemplo, dos perfiles de navegador). Dejá el formulario abierto como veterinario en una sesión. En la otra, como responsable, elegí **Revocar permiso**, escribí el motivo y confirmá. El siguiente intento del veterinario devuelve 404. **Otro responsable** también recibe 404 al consultar a Luna.

| Referencia en la demo | Identificador |
| --- | --- |
| Luna | `10000000-0000-4000-8000-000000000001` |
| Responsable de Luna | `11111111-1111-4111-8111-111111111111` |
| Veterinario de prueba | `55555555-5555-4555-8555-555555555555` |
| Otro responsable | `22222222-2222-4222-8222-222222222222` |

La concesión y la revocación quedan en `pruebas_demo_natan_b0203_manual.auditoria_permiso_clinico_demo`, en la misma transacción que el permiso. El registro guarda actor, permiso, mascota, acción y fecha. El rol demostrativo existe únicamente en el adaptador del perfil local y comprueba que la cuenta ficticia siga activa.

## Prueba con la integración de A

Usá el arranque con editores descrito en `EJECUTAR_EN_MI_PC.md` y entrá por `http://localhost:4200/login`. Para el recorrido del responsable, usá una cuenta de prueba registrada mediante Google en A y una mascota propia existente. Las pruebas automatizadas crean sus propios identificadores y no requieren compartir una cuenta o contraseña.

| Actor y acción | Resultado esperado |
| --- | --- |
| Responsable → **Mis mascotas → Abrir expediente**. Registrar una vacuna de prueba con fecha de ayer y recargar | La vacuna persiste, aparece en el listado cronológico y **Ver detalle** muestra el origen del responsable. |
| Responsable → filtrar por tipo y fechas, luego invertir el rango | El rango válido filtra. La pantalla explica el rango inválido y la API devuelve 400. Una fecha futura se rechaza. |
| Responsable → conceder a un veterinario habilitado acceso de lectura para esa mascota | El permiso figura una sola vez. Repetir un envío con la misma clave conserva el mismo recurso. |
| Veterinario → **Mis mascotas → Acceso veterinario**, ingresar el identificador y consultar | Con lectura puede ver el expediente. Para registrar necesita lectura y escritura en esa mascota. |
| Responsable → revocar con motivo. Veterinario → volver a consultar o guardar desde el formulario ya abierto | La siguiente petición comprueba la relación vigente y el acceso ajeno devuelve 404. |
| Usuario ajeno → consultar la mascota o el evento por identificador | Obtiene 404, sin información clínica. La proyección pública de B-02 nunca incluye vacunas. |

## Dependencia del bloque A

La identidad real procede de A. Para conceder permisos, `AccesoClinico` requiere verificar el rol veterinario vigente y auditar el cambio en la misma transacción. Con `local,integracion` se puede probar el expediente del responsable. Mientras falte el proveedor de A-03, la concesión devuelve 503 con código `INTEGRACION_CLINICA_PENDIENTE`. Los datos y el adaptador de la demostración pertenecen al perfil `local,demo-b01`. El recorrido clínico compartido requiere la implementación real de A-03.

## Evidencia reproducible

- `B03ServicioClinicoTest` prueba el registro del responsable, el rechazo de concesiones sin integración y la comprobación de habilitación del veterinario.
- `B03PerfilDemoTest` comprueba los cinco actores y el aislamiento de perfiles. `B03DemoRecorridoIntegrationTest` usa el selector, la auditoría y las cuentas de la demo contra PostgreSQL en el esquema aislado `pruebas_demo_b03`.
- `B03ClinicaIntegrationTest` usa PostgreSQL, titulares ficticios y un adaptador de prueba de A. Cubre idempotencia, filtros, fechas, permisos por mascota, revocación y reversión de la transacción si falla la auditoría.
- `expediente-mascota.component.spec.ts` verifica la interfaz con transporte HTTP de prueba. Para el recorrido veterinario integrado hacen falta cuentas habilitadas, un padrón real y el servicio de auditoría de A.

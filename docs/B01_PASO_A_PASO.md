# Cómo entender y demostrar B-01

La meta es que una persona autorizada pueda guardar a Luna, volver a consultarla, editarla y desactivarla sin perder los datos. Esta guía recorre las capas en el orden en que conviene estudiarlas. Los archivos incluyen comentarios sobre la responsabilidad de cada sección; no es necesario memorizar el código para explicar el recorrido.

## 1. Modelo: qué representa cada objeto

Empezá por `mascotas/model/Especie.java` y `Mascota.java`. `Especie` representa una entrada del catálogo: su código, nombre y vigencia. `Mascota` reúne los datos del perfil y su relación con esa especie. El constructor establece los valores de una instancia; los getters permiten consultarlos.

El catálogo permite incorporar otra especie sin agregar una constante a un enum. Eso no equivale a aceptar cualquier texto: el código enviado debe existir en PostgreSQL y estar habilitado para una asignación nueva.

## 2. DTO: qué puede enviar el formulario

Leé `CrearMascotaSolicitud`, `EditarMascotaSolicitud` y `DesactivarMascotaSolicitud` en `mascotas/dto`. Son los mensajes de entrada. Sus validaciones rechazan nombres vacíos, edades negativas o decimales, catálogos inválidos y etiquetas duplicadas.

Alta y edición no aceptan `responsableUsuarioId`, `disponibilidad` ni información clínica. Esos valores no dependen de lo que escriba una persona en el navegador. La edición agrega `versionEsperada`: el número que tenía el registro cuando se abrió el formulario.

`MascotaPrivadaRespuesta` define lo que se devuelve después. No se debe reutilizar para mostrar un perfil público de adopción porque incluye datos de gestión privada.

## 3. Base de datos y repositorio: dónde quedan guardados los datos

Revisá las migraciones en `backend/src/main/resources/db/migration`. Flyway aplica cada versión una vez y registra el resultado en `flyway_schema_history`. V4 creó el catálogo de especies y V5 agregó su comprobación de vigencia. Los cambios completos de B-01 se agregan en V6; no reemplazan los archivos ya aplicados.

`MascotaRepository` invoca las funciones y procedimientos SQL y transforma los resultados en objetos Java. PostgreSQL conserva los datos aunque se recargue la página. Las claves foráneas impiden relaciones con registros inexistentes; los procedimientos comprueban las condiciones que deben mantenerse durante la escritura.

Una consulta previa de vigencia ayuda a explicar un error, pero otro proceso podría modificar el catálogo después de esa consulta. Por eso la escritura también valida y bloquea lo necesario dentro de su transacción.

## 4. Servicio: quién puede hacer cada operación

`MascotaService` coordina validaciones, contexto autorizado y persistencia. `@Transactional` hace que una operación se confirme completa o se revierta ante un fallo. No significa que cualquier consulta y escritura queden automáticamente protegidas de carreras: la versión y los bloqueos de SQL resuelven los casos concurrentes.

El servicio obtiene al actor mediante `IdentidadMascotas` y consulta sus organizaciones mediante `AmbitosMascotas`. Una mascota individual corresponde a su responsable; una organizativa exige una representación vigente. Conocer el UUID de una mascota ajena no concede acceso.

El adaptador real de A está pendiente de la integración documentada en [INTEGRACION_B01.md](INTEGRACION_B01.md). La demostración usa identidades ficticias expresamente separadas; no convierte un usuario escrito en el navegador en una identidad de producción.

## 5. Controlador y pantalla: cómo viaja una petición

`MascotaController` recibe JSON, valida el DTO, llama al servicio y devuelve el resultado con un estado HTTP. La pantalla usa ese resultado para confirmar el guardado y actualizar su lista. Si hay un error, no debe mostrar éxito ni descartar silenciosamente lo que se estaba editando.

El recorrido es: **formulario → controlador → servicio → repositorio → PostgreSQL → respuesta → pantalla**. Cada capa tiene una tarea distinta. Si mañana un compañero modifica una regla de autorización, debe buscarla en el contexto y el servicio; si cambia el formato del JSON, debe revisar DTO, controlador, contrato y pantalla.

## 6. Dos protecciones que conviene explicar con un ejemplo

**Versión:** dos formularios abren Luna con versión 0. El primero guarda y la versión pasa a 1. El segundo todavía envía 0: recibe HTTP 409 y no sobrescribe el cambio anterior. Debe recargar y decidir si vuelve a editar.

**Idempotencia:** una creación lleva una `Idempotency-Key`. Si el navegador no recibió la respuesta y reenvía la misma intención con esa clave, obtiene la misma mascota, no una segunda Luna. Reutilizar la clave con otros datos produce conflicto. La desactivación también usa esta protección y conserva un solo evento de auditoría por intención.

## Demostración manual

1. Seguí el arranque de demostración del README y elegí **Responsable de Luna**. El puerto predeterminado es 4200; si cambiaste `FRONTEND_PORT` en `.env`, usá ese puerto. Los datos de demostración y de pruebas están separados del esquema habitual.
2. La Luna inicial ya existe. Conservála activa y creá otra llamada **Luna prueba B-01**, especie **GATO**, **8 meses**, tamaño **PEQUENO**, energía **MEDIA** y al menos una etiqueta ficticia activa del catálogo. Confirmá el mensaje de guardado y abrí el detalle de esa nueva mascota.
3. Recargá la página. Luna prueba B-01 debe seguir en la lista con el mismo identificador. Esto prueba que el dato se recupera del servidor; no depende solo de una variable del formulario.
4. Editá la edad a **9 meses**, guardá y recargá otra vez. La edad debe seguir en 9 y la versión debe haber aumentado.
5. Abrí el mismo perfil en dos pestañas antes de editar. Guardá en una y luego intentá guardar desde la otra: la segunda debe avisar el conflicto y ofrecer volver a consultar.
6. Cambiá a otra identidad ficticia sin el ámbito de Luna prueba B-01. No debe verla ni acceder a su detalle utilizando el UUID anterior.
7. Volvé a la identidad autorizada y desactivá **Luna prueba B-01** con un motivo. Consultá el filtro de inactivas: conserva su identificador y no se convierte en una mascota publicada para adopción. La Luna inicial debe continuar activa.

En pgAdmin podés comprobar la persistencia con una consulta de solo lectura, usando el UUID que mostró el detalle y el esquema de la demostración:

```sql
SELECT id, nombre, edad_estimada_meses, version, activa, disponibilidad
FROM demo_b01.mascota
WHERE id = 'REEMPLAZAR_POR_EL_UUID_DE_LUNA';
```

El texto entre comillas es un marcador: sustituilo por el UUID antes de ejecutar. Para observar las migraciones, consultá `demo_b01.flyway_schema_history`. No hace falta insertar ni modificar registros manualmente para demostrar el formulario.

## Evidencia automatizada

El 10 de noviembre de 2026 se completó `mvnw clean verify`: **130 pruebas de backend, sin fallos ni errores**. También se completaron la compilación de Angular y **16 pruebas del frontend**. Las pruebas comprueban el comportamiento con contextos y datos ficticios; no son evidencia de que el login de A ya esté conectado. Los pasos manuales anteriores son una guía de demostración, no una afirmación de ejecución en navegador.

## Cómo explicar tu parte

Podés explicar que el módulo separa los datos del perfil, los mensajes del formulario, las reglas de negocio y el acceso a PostgreSQL; que el servidor controla permisos y campos reservados; y que versión e idempotencia protegen dos problemas distintos. Acompañá esa explicación con el recorrido de Luna y con los resultados reales de las pruebas. La conexión al ingreso de A y el catálogo público de B-02/D son pasos de integración separados, no resultados demostrados por este recorrido local.

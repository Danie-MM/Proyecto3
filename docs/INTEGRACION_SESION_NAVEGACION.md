# Integración de acceso, navegación y catálogo

Se corrigieron los conflictos entre la identidad de A, la demostración de B y los consumidores C y D. El backend actualizado está ejecutándose en 8083 y Angular en 4200. Esta revisión parte de `main` en `a024c107`.

## Problemas corregidos

| Situación | Corrección |
| --- | --- |
| Un JWT válido llegaba al perfil demo y B rechazaba su correo como identidad ficticia. | El adaptador demo delega los principales `UserDetails` al resolvedor real de A y conserva la resolución de los actores ficticios por cookie. El Bearer válido prevalece y los inválidos se rechazan. |
| B consultaba actores de demo tras Google y desmontaba su pantalla al recibir un actor nulo. | La entrada con Google consulta el contexto real. El selector se usa al entrar por la demostración local. |
| Un 401 público, auxiliar o tardío borraba otra sesión válida. | El interceptor termina la sesión únicamente si la petición rechazó el mismo Bearer que continúa vigente en el navegador. |
| Login y registro siempre llevaban a Entorno. | Se recupera el destino interno solicitado o se abre Mis mascotas. El destino se valida contra las rutas de la aplicación. |
| Los módulos mostraban barras duplicadas o perdían enlaces. | El shell compartido conserva Mis mascotas, Catálogo, Mi ubicación y Entorno. El cierre desde el menú limpia la sesión del navegador. |
| D consultaba la lista privada y esperaba campos distintos de la respuesta de B. | El catálogo consume `GET /api/v1/catalogo/mascotas`, con proyección pública, filtros y paginación. La organización activa se verifica antes del conteo y la página. |

La nueva migración de B es `V16__catalogo_mascotas_publicas.sql`. Las ramas pendientes de los compañeros mantienen sus migraciones. La integración futura de A-02 debe revisar sus cambios de acceso y sesión junto con los ajustes de esta entrega.

## Resultados observados

- Maven Wrapper con JDK 21 completó `clean verify` sobre PostgreSQL con **195 pruebas aprobadas**, cero fallos, errores y omisiones. También generó el JAR ejecutable.
- Angular compiló producción y pasó **81 pruebas Jasmine en Chromium WebView2**, sin fallos ni pendientes.
- La regresión de servidor usa un JWT emitido por `JwtService` para una cuenta de prueba mientras coexiste una cookie demo. Recorre el contexto B, la ubicación C y Entorno y comprueba el rechazo tras desactivar la cuenta.
- En la aplicación activa, `/actuator/health` devolvió `UP`, la página de ingreso devolvió 200, el catálogo por el proxy frontend devolvió 200 y la ubicación con sesión demo devolvió 200.
- Se publicaron únicamente Nube y Coco de la demostración local para mostrar dos resultados con fotos en el catálogo. La consulta pública devuelve esos dos perfiles.

## Comprobación en Chrome

El servidor confirmó el ingreso con Google de una cuenta de prueba activa y la consulta de las mascotas asociadas. Las identidades ficticias conservan su propia asignación.

La comprobación visual final de navegación en el Chrome del usuario quedó pendiente. El controlador de ventanas no pudo arrancar y el adaptador nativo devolvió `failed to launch codex app-server: program not found`. El backend se actualizó desde el ejecutor local con el JAR verificado y la misma configuración de demo. El identificador del proceso activo se registra fuera del repositorio en `work/backend-login-activo.pid`. El ingreso real confirmado por el servidor no demuestra por sí solo la navegación visual en Chrome.

La revisión final comprobó que el backend seguía en estado `UP`, las cinco rutas de entrada de Angular respondían 200 y el catálogo público por el proxy seguía devolviendo los dos perfiles de demostración. La revisión del código de identidad, guards, interceptor y catálogo no encontró nuevos defectos críticos.

La ruta de ingreso es `http://localhost:4200/login`. Tras Google se debe conservar el menú y permitir cambiar entre los cuatro módulos. Una vuelta solicitada desde otra pantalla conserva su ruta, filtros y fragmento.

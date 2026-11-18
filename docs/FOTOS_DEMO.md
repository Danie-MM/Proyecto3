# Fotografías de la demostración

Estas imágenes ilustran mascotas ficticias del perfil local de B. Se guardan en el classpath privado del backend y se cargan en `archivo_mascota` mediante el inicializador de demostración. La tarjeta obtiene la fotografía por la API autorizada.

| Recurso | Uso | Procedencia |
| --- | --- | --- |
| `luna.jpg` | Luna | Chelsea, fotografía de Stefan van der Walt cedida mediante CC0 para scikit-image. [Fuente y licencia](https://scikit-image.org/docs/0.23.x/api/skimage.data.html#skimage.data.chelsea) |
| `sol.jpg` | Sol y Kira | Imagen de perro que ya formaba parte del proyecto en `frontend/public/assets/fotos_ejemplo/luna.jpg`. |
| `nube.jpg` | Nube y Milo | Imagen de gato que ya formaba parte del proyecto en `frontend/public/assets/fotos_ejemplo/mia.jpg`. |
| `coco.jpg` | Coco | Imagen de perro que ya formaba parte del proyecto en `frontend/public/assets/fotos_ejemplo/luna.jpg`. |

Chelsea se conserva con sus dimensiones originales y se convirtió de PNG a JPEG para la carga de ejemplo. Los nombres de mascotas, responsables y expedientes de ejemplo corresponden a la demostración de PetMatchCR. Las otras imágenes se reutilizaron de los recursos existentes del proyecto.

El inicializador respeta cualquier fotografía que el usuario haya reemplazado o retirado. Los datos se preparan en la demostración local, con los perfiles `local,demo-b01` y el esquema separado configurado para ella.

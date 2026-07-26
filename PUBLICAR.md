# Publicar una versión

La app se distribuye **fuera de Play Store**: un APK firmado en GitHub Releases y un
manifiesto `docs/updates.json` servido por GitHub Pages, que es lo que la app consulta
para saber si hay versión nueva. Todo el ritual está en `scripts/publicar_release.py`.

## Antes de nada: la keystore

Los APK se firman siempre con **la misma clave**. Android solo actualiza una app
instalada si la versión nueva lleva la misma firma, así que:

> **Si se pierde esa keystore, ninguna instalación existente podrá actualizarse nunca
> más.** Solo quedaría desinstalar y volver a empezar, perdiendo los datos del viaje.

- Vive **fuera del repo** (por defecto `C:/Users/marcm/keystores/middleearthjourney.jks`).
- Sus datos van en `keystore.properties`, en la raíz, **no versionado**
  (hay plantilla en `keystore.properties.example`). Alternativa sin fichero: las
  variables de entorno `MEJ_STORE_FILE`, `MEJ_STORE_PASSWORD`, `MEJ_KEY_ALIAS` y
  `MEJ_KEY_PASSWORD`; el script materializa un `keystore.properties` temporal y lo borra.
- Haz **copia de seguridad de la keystore y de su contraseña** en sitio distinto del PC.
- La huella del certificado queda registrada en `scripts/firma_esperada.txt`; si algún
  día no coincide, el script **aborta** antes de publicar nada.

## Pasos

1. Sube la versión en `composeApp/build.gradle.kts` (**fuente única**):
   `versionCode` **siempre +1** (es lo único que compara la app) y `versionName` para
   las personas.
2. Ensayo sin publicar:
   ```bash
   python scripts/publicar_release.py --dry-run --notas "Qué trae esta versión"
   ```
   Construye el APK firmado, calcula el sha256, escribe `docs/updates.json` y verifica la
   coherencia. No sube nada.
3. Publicación real (desde la rama `main`, que es la que sirve Pages):
   ```bash
   python scripts/publicar_release.py --notas "Qué trae esta versión"
   ```
   Crea la Release `vX.Y` con el APK, commitea y empuja el manifiesto, y espera a que la
   URL pública sirva ya el `versionCode` nuevo.

## Lo que el script comprueba antes de publicar

| Cinturón | Por qué |
|---|---|
| `gh auth status` | Sin sesión no hay Release; mejor fallar al principio |
| Rama = `main` | Pages sirve `main/docs`: publicar desde otra rama crearía la Release sin avisar a nadie |
| `versionCode` del APK (`aapt2`) == el declarado | Que el APK subido sea de verdad la versión anunciada |
| `versionCode` del manifiesto == el declarado | Coherencia del JSON |
| `sha256` del manifiesto == el del APK real | La app rechaza (y borra) cualquier descarga cuyo hash no cuadre |
| `versionCode` > el ya publicado | Con un número igual o menor, nadie detectaría la actualización |
| Huella de firma == `firma_esperada.txt` | Una firma distinta rompe la actualización de todas las instalaciones |

## Detalles que conviene recordar

- **La primera auto-actualización pide confirmación.** Android solo instala en silencio
  si la app es el instalador registrado de su propio paquete, y eso solo es cierto a
  partir de la primera vez que se actualiza a sí misma. De la segunda en adelante, sin
  preguntar.
- **Play Protect** puede bloquear la primera instalación de una firma sin reputación
  («Aplicación bloqueada para proteger tu dispositivo» → *Más detalles → Instalar de
  todas formas*). Pasa una vez por dispositivo.
- El `versionName` **nunca** se compara: la app decide solo con el `versionCode` entero.

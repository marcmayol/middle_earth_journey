# Middle Earth Journey

App personal que convierte tus **pasos diarios** en avance por una **ruta de la Tierra
Media** (El Señor de los Anillos / El Hobbit). Cada paso real te acerca a tu destino. No
hay competición: es un viaje personal con tono de diario. Todo el texto está en **español**.

## Capturas

| Misión | Mapa | Crónicas | Stats | Logros |
|:---:|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/01-mision.png" width="170"> | <img src="docs/screenshots/02-mapa.png" width="170"> | <img src="docs/screenshots/03-cronicas.png" width="170"> | <img src="docs/screenshots/04-stats.png" width="170"> | <img src="docs/screenshots/05-logros.png" width="170"> |
| Pasos de hoy y progreso | Carta de la Tierra Media | Capítulos narrados | Semana, mes y año | 7 / 15 desbloqueados |

Y las **cinemáticas**, que se dibujan trazo a trazo sobre pergamino mientras la voz narra:

<img src="docs/screenshots/06-cinematica.png" width="220">

> Capturas tomadas en un emulador con un viaje de ejemplo (202 días, ~1,96 M de pasos),
> no con datos reales.

## Descargar (Android)

**➡️ [Descargar APK — v1.1](https://github.com/marcmayol/middle_earth_journey/releases/latest)**

Descarga el archivo `.apk` desde la [última release](https://github.com/marcmayol/middle_earth_journey/releases/latest)
e instálalo en el móvil (Android 8+). Necesitarás activar *"instalar apps de origen desconocido"*.

A partir de la v1.1 la app **se actualiza sola**: consulta el manifiesto publicado y te
avisa cuando hay versión nueva.

## Qué hace

- Cuenta los pasos del día (podómetro del sistema) y los convierte en kilómetros
  (1 paso ≈ 0,7 m), avanzando por una ruta con sus hitos.
- **Dos rutas**, con ida y vuelta: *La marcha de Frodo* (→ Monte del Destino, 2.860 km) y
  *Ida y vuelta de Bilbo* (→ Erebor, 1.500 km).
- **5 secciones:** Misión, Mapa (carta ilustrada de la Tierra Media), Crónicas
  (cinemáticas), Stats (gráficas) y Logros.
- **Cinemáticas narradas**: line-art que se dibuja sobre pergamino + voz (TTS) + subtítulos,
  desbloqueadas por etapa.
- **Sucesos aleatorios** del camino (cada ~5 días, tras andar 3 km), con notificación.
- Estética "códice": dorado sobre fondo oscuro, tipografías Cinzel + EB Garamond.

## Estado / ramas

- **`main`** — La rama de verdad: **Kotlin / Compose Multiplatform** (módulo `:composeApp`).
  **Android compila, corre y es lo que se publica.** El lado **iOS está escrito pero sin
  compilar nunca**: se desarrolló en Windows, así que falta pasarlo por un Mac y pulir la
  interop de Kotlin/Native. Ver [`MIGRATION.md`](MIGRATION.md) y [`iosApp/README.md`](iosApp/README.md).
- **`kmp-migration`** — Histórica: es de donde salió la migración, ya fusionada en `main`.
  No se trabaja ahí.

> La app Android nativa original (módulo `:app`, Jetpack Compose sin multiplataforma) vivió
> en `main` hasta julio de 2026 y solo queda en el histórico de git.

## Compilar

Requisitos: JDK 17, Android SDK (compileSdk 34), un dispositivo/emulador con Android 8+ (minSdk 26).

```bash
./gradlew :composeApp:assembleDebug
# APK en: composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

La build de `debug` lleva el sufijo `.debug` en el `applicationId`, así que **se instala al
lado de la app publicada** en vez de actualizarla: son dos apps distintas para Android y no
comparten datos. Para generar la versión firmada que se distribuye, ver [`PUBLICAR.md`](PUBLICAR.md).

En iOS el objetivo es que el proyecto Xcode `iosApp/` compile ese mismo código común: abrirlo
en Xcode, poner tu Team y darle a Run.

## Stack

- **Compartido (`commonMain`):** Compose Multiplatform (UI), lógica de viaje, rutas, logros,
  sucesos, cinemáticas, perfil corporal y calorías.
- **Android:** pasos vía **Health Connect** (agrega móvil + smartwatch) con caída automática
  al sensor `TYPE_STEP_COUNTER`, servicio en primer plano + notificaciones, `TextToSpeech`,
  DataStore. Módulo `:actualizador` para actualizarse fuera de Play Store.
- **iOS (escrito, sin compilar):** `CMPedometer` (pasos), `AVSpeechSynthesizer` (voz),
  `UNUserNotificationCenter` (avisos), `multiplatform-settings` (persistencia).

App personal. **No está en ninguna tienda**: se instala por sideload y a partir de ahí se
actualiza sola contra su propio manifiesto (ver [`PUBLICAR.md`](PUBLICAR.md)).

# Migración a Kotlin / Compose Multiplatform (Android + iOS)

> ## ✅ ACTUALIZACIÓN: Android-KMP ya COMPILA y FUNCIONA
> El código compartido ya está convertido a multiplataforma y **el target Android compila
> y arranca** sobre Compose Multiplatform (`./gradlew :composeApp:assembleDebug`, verificado).
> Lo hecho: `java.time`→`kotlinx-datetime`, `String.format`→util común (`util/Format.kt`,
> `util/Dates.kt`), `ViewModel` común, fuentes vía Compose Resources, logo del splash en
> `Canvas`, `PathMeasure` de las cinemáticas en común (por subpaths), reloj con
> `withFrameNanos`. Las capas de plataforma están detrás de interfaces comunes:
> `data/JourneyRepository.kt` (impl Android = `StepRepository`) y `ui/cinematic/Narrator.kt`
> (`expect fun rememberNarrator()`, actual Android = `Narrator.android.kt`).
>
> ### Lo que QUEDA es solo iOS (tu parte, en el Mac):
> 1. **`iosMain`** con los `actual`/implementaciones:
>    - `actual fun rememberNarrator(): Narrator` → `AVSpeechSynthesizer` (voz es-ES, rate/pitch).
>    - Una implementación de **`JourneyRepository`** para iOS: pasos con **`CMPedometer`**
>      (CoreMotion) y persistencia con **`multiplatform-settings`** (`NSUserDefaultsSettings`).
>      Reutiliza la misma lógica de viaje que `StepRepository` (mira ese archivo como guía;
>      el grueso —km, hitos, sucesos— ya es común en el `MainViewModel`).
>    - Notificaciones de sucesos con `UNUserNotificationCenter` (en Android están en
>      `StepTrackingService`; en iOS, al abrir la app o con `BGTaskScheduler`).
> 2. **Punto de entrada iOS**: un `fun MainViewController()` en `iosMain` que cree el repo
>    iOS + `MainViewModel` y monte `ComposeUIViewController { AppTheme { /* raíz */ } }`.
>    La lógica de raíz (colectar flows del ViewModel + `MainScreen` + splash) está hoy en
>    `androidMain/MainActivity.kt`; replícala en común o en iOS (las partes de permisos
>    Android y el servicio NO aplican a iOS).
> 3. **Proyecto Xcode `iosApp/`** (genera una plantilla KMP de Android Studio o el wizard y
>    copia el código), firma con tu Apple ID gratis, `Info.plist` con `NSMotionUsageDescription`.
> 4. En `composeApp/build.gradle.kts` los targets de iOS se activan **solos al compilar en
>    Mac** (`if (isMac) { iosX64(); iosArm64(); iosSimulatorArm64() }`).
>
> El resto de esta guía (abajo) es el detalle original; sigue siendo válido como referencia.

---

Guía de handoff para terminar la migración. La app **Android original y funcional**
está en la rama **`master`** (compila y corre). Esta rama (`kmp-migration`) tiene la
**estructura KMP ya montada** pero el código compartido **aún no compila**: falta
convertir las APIs de JVM/Android a equivalentes multiplataforma y escribir el lado iOS.

> Objetivo: una sola base de código (`composeApp`) que compile en **Android** y en
> **iOS** (iPhone), compartiendo UI (Compose Multiplatform) y lógica, con lo específico
> de cada plataforma detrás de `expect/actual`.

---

## Estado actual (lo que YA está hecho en esta rama)

- `app` → **`composeApp`** con source sets `commonMain` / `androidMain` (+ `iosMain` por crear).
- **Gradle Compose Multiplatform** listo: `gradle/libs.versions.toml`, `settings.gradle.kts`,
  `build.gradle.kts` raíz y `composeApp/build.gradle.kts` (Kotlin 1.9.23, CMP 1.6.11).
- **Targets de iOS declarados solo en Mac** (en `composeApp/build.gradle.kts`,
  `if (isMac) { iosX64(); iosArm64(); iosSimulatorArm64() }`). En tu Mac se activan solos.
- **Fuentes** movidas a `composeApp/src/commonMain/composeResources/font/` (Cinzel + EB Garamond).
- **Recursos Android** (iconos, manifest, themes) en `composeApp/src/androidMain/res`.
- Dependencias previstas: `kotlinx-datetime`, `multiplatform-settings`, `lifecycle-viewmodel-compose` (MP).

Todo el código de la app está en `composeApp/src/commonMain/kotlin/com/marcm/middleearthjourney/`
y lo de Android en `…/androidMain/kotlin/…` (`MainActivity`, `MiddleEarthApp`, `StepTrackingService`).

---

## Lo que FALTA (tareas concretas, por orden recomendado)

### 1. Quitar APIs JVM-only del código compartido
`commonMain` no tiene `java.*`. Reemplazar:

- **`java.time` → `kotlinx-datetime`** en:
  `MainViewModel.kt`, `data/StepRepository.kt`, `ui/screens/MissionScreen.kt`,
  `ui/screens/StatsScreen.kt`, `ui/Codex.kt`, `ui/cinematic/CinematicPlayer.kt`.
  - `LocalDate.now()` → `Clock.System.todayIn(TimeZone.currentSystemDefault())`.
  - `LocalDate.toEpochDay()` → `LocalDate.toEpochDays()`.
  - `DayOfWeek`, sumar/restar días → `kotlinx.datetime.LocalDate.plus(DatePeriod(days=…))`,
    `dayOfWeek` (kotlinx tiene `DayOfWeek`).
  - `YearMonth`, `TemporalAdjusters`, `DateTimeFormatter` → no existen: calcular a mano
    (primer día de mes, días del mes, "lunes de esta semana") y formatear fechas con un
    helper propio (ver punto 2). Hay nombres de mes ya en `MainViewModel.MONTH_NAMES`.
- **`String.format` / `java.util.Locale` → formateo manual** en:
  `ui/Codex.kt` (`kmEs`, `intEs`), `ui/screens/{Mission,Stats,Chronicle}Screen.kt`,
  `StepTrackingService.kt`. Escribir helpers comunes (p. ej. en `Codex.kt`):
  - `intEs(v): String` con punto de millar.
  - `kmEs(v, decimals): String` con coma decimal y punto de millar.
  - sustituir `"%.1f".format(x)` por un formateo manual (redondeo + montar el string).

### 2. Capas de plataforma con `expect/actual`
Crear interfaces `expect` en `commonMain` y `actual` en `androidMain` (existente) e `iosMain` (nuevo).

- **Persistencia → `multiplatform-settings`**. Reescribir `StepRepository` para usar
  `com.russhwolf.settings.Settings` (síncrono) en vez de DataStore. El `Settings` se crea
  por plataforma: Android `SharedPreferencesSettings`, iOS `NSUserDefaultsSettings`. La
  lógica de (de)serialización del repo es común; solo cambia de dónde sale el `Settings`.
- **Sensor de pasos → `expect class StepSource`** con un `StateFlow<Long>` de pasos
  acumulados + pasos de hoy:
  - **Android (`actual`)**: `SensorManager` `TYPE_STEP_COUNTER` (lo que ya hace
    `StepRepository`/`StepTrackingService` hoy).
  - **iOS (`actual`)**: `CMPedometer` (CoreMotion). `queryPedometerData(from:to:)` para el
    histórico y `startUpdates(from:)` para el directo. Devuelve `numberOfSteps` desde una
    fecha. **No hace falta servicio en segundo plano**: en iOS el podómetro del sistema
    cuenta solo y consultas al abrir la app.
- **Voz (TTS) → `expect class Narrator`** (hoy en `CinematicPlayer.kt`, acoplado a
  `android.speech.tts.TextToSpeech`):
  - **Android (`actual`)**: el `Narrator` actual (TextToSpeech, voz `es-es-x-eed`, pitch 0.8).
  - **iOS (`actual`)**: `AVSpeechSynthesizer` + `AVSpeechUtterance` (`voice =
    AVSpeechSynthesisVoice(language: "es-ES")`, `rate`, `pitchMultiplier`). El `LocalContext`
    que usa hoy `CinematicPlayer` → reemplazar por un `expect fun rememberNarrator(): Narrator`.
- **Notificaciones → `expect`** (hoy en `StepTrackingService`, `NotificationManager`):
  - **Android**: lo actual.
  - **iOS**: `UNUserNotificationCenter` (pedir permiso, `UNMutableNotificationContent`,
    `UNTimeIntervalNotificationTrigger`). Ojo: en iOS no hay servicio 24/7; los sucesos se
    resuelven al abrir la app (o con BGTaskScheduler si se quiere en background).

### 3. ViewModel a común
`MainViewModel : AndroidViewModel` → `class MainViewModel : androidx.lifecycle.ViewModel`
(de `org.jetbrains.androidx.lifecycle`). Inyectar `Settings` + `StepSource` por constructor
(ya no `Application`). En Android, crearlo con un `ViewModelProvider.Factory` o el
`viewModel { }` multiplataforma. `collectAsStateWithLifecycle` → `collectAsState()`
(o el `collectAsStateWithLifecycle` de lifecycle MP).

### 4. Recursos
- **Fuentes**: `Theme.kt` usa `Font(R.font.cinzel_regular)` → Compose Resources:
  `Font(Res.font.cinzel_regular)` (import `com.marcm.middleearthjourney.resources.Res`).
  Las TTF ya están en `commonMain/composeResources/font/`.
- **Logo del splash**: `SplashScreen.kt` usa `painterResource(R.drawable.ic_launcher_foreground)`.
  Opción simple multiplataforma: **dibujar el logo "Ruta Circular" con `Canvas`** en común
  (la geometría está en `ui/cinematic/Cinematic.kt` y en `res/drawable/ic_launcher_foreground.xml`),
  o exportarlo como recurso Compose (`composeResources/drawable/…`).
- **Icono de app**: en Android sigue el adaptive icon de `androidMain/res`. En iOS hay que
  poner el `AppIcon` en el asset catalog del proyecto Xcode (PNG, el del `app_icon_dark.svg`).

### 5. Cinemáticas (`CinematicPlayer.kt`)
- Usa `android.graphics.PathMeasure` (para revelar el trazo por contornos) porque la
  `PathMeasure` de Compose no tiene `nextContour`. En común: **partir cada path por
  comandos `M`/`m`** y animar cada subpath con la `androidx.compose.ui.graphics.PathMeasure`
  multiplataforma (sí tiene `getSegment`). El resto (`PathParser`, `Canvas`) es común.
- `TextToSpeech`/`LocalContext` → el `Narrator` del punto 2.

### 6. iOS app (proyecto Xcode)
- Crear `iosApp/` (proyecto Xcode) que enlaza el framework `ComposeApp` y arranca la UI con
  `ComposeUIViewController { App() }` (hay que exponer un `fun MainViewController()` en
  `iosMain`). La plantilla oficial KMP (`kotlin-multiplatform-wizard` / Android Studio →
  *New → Kotlin Multiplatform App*) genera este `iosApp` y el bloque de Gradle; lo más
  rápido es generar una plantilla y copiar el código `commonMain` dentro.
- **Firma**: con tu Apple ID gratis vale para tu iPhone (la app caduca a los 7 días y se
  reinstala). En Xcode: *Signing & Capabilities* → tu equipo personal.
- **Permisos iOS** (Info.plist): `NSMotionUsageDescription` (podómetro). Notificaciones se
  piden en runtime.

---

## Cómo compilar

- **Android (funciona ya en `master`; en esta rama, tras el punto 1-5):**
  `./gradlew :composeApp:assembleDebug` → APK en `composeApp/build/outputs/apk/debug/`.
- **iOS (en Mac, tras el punto 6):** abrir `iosApp/iosApp.xcodeproj` en Xcode y *Run* en
  el iPhone, o `./gradlew :composeApp:linkDebugFrameworkIosArm64` para el framework.

## Sugerencia de orden
1, 2, 3, 4, 5 hasta dejar **Android-KMP compilando** (comparar comportamiento con la app de
`master`). Luego 6 para iOS. Ir compilando a menudo; los primeros errores serán muchos
imports y tipos de fecha.

## Contexto del proyecto
Ver `README.md`. La app convierte pasos diarios en avance por rutas de la Tierra Media
(Frodo/Bilbo, ida y vuelta), con cinemáticas narradas, logros, estadísticas y sucesos
aleatorios. Todo el texto en español.

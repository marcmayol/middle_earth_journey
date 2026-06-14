# iosApp — app iOS (Compose Multiplatform)

Todo el código (Kotlin compartido + iOS + Swift) ya está escrito. Para probar en tu iPhone:

## Pasos
1. En `iosApp/Configuration/Config.xcconfig`, pon tu **`TEAM_ID`** (Xcode → Settings →
   Accounts → tu Apple ID; el "Personal Team" gratis sirve para tu propio iPhone).
2. Abre **`iosApp/iosApp.xcodeproj`** en Xcode (necesitas un Mac).
3. Selecciona tu iPhone como destino y pulsa **Run** (▶). La primera vez Xcode ejecutará
   el Gradle que compila el framework `ComposeApp` (tarda un poco).
4. En el iPhone, acepta el permiso de **movimiento** (para contar pasos) cuando lo pida.

> Firma gratis: la app caduca a los 7 días; vuelve a darle a Run para reinstalarla.

## Qué hace cada cosa
- `iosApp/iOSApp.swift`, `ContentView.swift` — arranque SwiftUI que monta la UI de Compose
  (`MainViewControllerKt.MainViewController()` del framework `ComposeApp`).
- `iosApp/Info.plist` — incluye `NSMotionUsageDescription` (permiso de podómetro).
- El código Kotlin de iOS está en `composeApp/src/iosMain/`:
  - `MainViewController.kt` — crea repo + ViewModel + la raíz común `App()`.
  - `data/IosJourneyRepository.kt` — pasos con **CMPedometer** + persistencia NSUserDefaults.
  - `ui/cinematic/Narrator.ios.kt` — voz con **AVSpeechSynthesizer**.
  - `PlatformIos.kt` — `Settings` (NSUserDefaults).

## Si Xcode no abre/compila el `.xcodeproj`
El `project.pbxproj` está hecho a mano (sin Mac para validarlo). Si da problemas, lo más
rápido es **regenerar el proyecto**: en Android Studio, *New → New Project → Kotlin
Multiplatform App* (o el wizard KMP), y copia dentro estos 4 archivos Swift/plist/xcconfig
y el bloque de `iosApp` del Gradle. El código Kotlin (`commonMain` + `iosMain`) no cambia.

## Cosas a revisar en el dispositivo (interop iOS, no verificable sin Mac)
- Firmas exactas de CMPedometer (`queryPedometerDataFromDate:toDate:`),
  `NSCalendar.currentCalendar`, y los opcionales (`NSNumber?.longValue`).
- El delegate de `AVSpeechSynthesizer` (callbacks de inicio/fin de frase).
- Que el `embedAndSignAppleFrameworkForXcode` deje el framework donde apunta
  `FRAMEWORK_SEARCH_PATHS`.

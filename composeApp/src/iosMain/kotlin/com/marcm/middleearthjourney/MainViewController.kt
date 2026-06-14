package com.marcm.middleearthjourney

import androidx.compose.ui.window.ComposeUIViewController
import com.marcm.middleearthjourney.data.IosJourneyRepository
import platform.UIKit.UIViewController

/**
 * Punto de entrada de iOS: crea la persistencia + repositorio (CMPedometer) + ViewModel
 * y monta la raíz común [App] en un UIViewController de Compose.
 *
 * Lo llama Swift desde `iosApp/iosApp/ContentView.swift`.
 */
fun MainViewController(): UIViewController {
    requestNotificationAuthorization()
    val settings = createSettings()
    val repo = IosJourneyRepository(settings)
    val viewModel = MainViewModel(repo)
    // En iOS, CMPedometer pide el permiso de movimiento automáticamente al consultar
    // (requiere NSMotionUsageDescription en Info.plist), así que no hace falta acción manual.
    return ComposeUIViewController { App(viewModel) }
}

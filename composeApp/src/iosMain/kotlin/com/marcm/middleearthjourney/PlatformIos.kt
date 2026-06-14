package com.marcm.middleearthjourney

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

/** Persistencia iOS: NSUserDefaults vía multiplatform-settings. */
fun createSettings(): Settings =
    NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)

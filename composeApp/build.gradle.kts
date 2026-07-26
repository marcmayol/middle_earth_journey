import java.util.Properties

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    // Targets de iOS solo cuando se compila en un Mac (en Windows/Linux no compilan).
    val isMac = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)
    if (isMac) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
        targets.withType(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget::class.java).configureEach {
            binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.health.connect)
            // Auto-actualización fuera de Play Store. Solo Android: commonMain e iosMain
            // no lo ven, así que el proyecto sigue compilando para iOS en el Mac.
            implementation(project(":actualizador"))
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.marcm.middleearthjourney.resources"
}

android {
    namespace = "com.marcm.middleearthjourney"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")

    // BuildConfig.VERSION_CODE es la fuente de verdad de la versión instalada: con él
    // compara el actualizador contra el manifiesto remoto.
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.marcm.middleearthjourney"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 2
        versionName = "1.1"
    }
    // Firma de release. Los datos salen de keystore.properties (raíz del proyecto, fuera
    // del control de versiones); el script de publicación lo genera desde variables de
    // entorno si no está. Sin él no se firma nada: más vale un APK sin firmar y un error
    // claro que un APK firmado con una clave que no es la de la app publicada.
    val keystoreProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    val hayFirma = keystoreProps.getProperty("storeFile") != null

    signingConfigs {
        if (hayFirma) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = false
            if (hayFirma) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "AVISO: falta keystore.properties; el APK de release saldrá SIN FIRMAR " +
                        "y no servirá para actualizar la app instalada.",
                )
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

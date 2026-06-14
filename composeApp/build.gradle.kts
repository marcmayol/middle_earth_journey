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

    defaultConfig {
        applicationId = "com.marcm.middleearthjourney"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

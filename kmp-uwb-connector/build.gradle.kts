plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
}

group = "com.atruedev"
version = providers.environmentVariable("VERSION").getOrElse("0.0.0-local")

kotlin {
    explicitApi()

    android {
        namespace = "com.atruedev.kmpuwb.connector.ble"
        compileSdk =
            libs.versions.androidCompileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.androidMinSdk
                .get()
                .toInt()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "KmpUwbConnector"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":"))
            api(libs.kmp.ble)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

mavenPublishing {
    coordinates("com.atruedev", "kmp-uwb-connector", version.toString())
}

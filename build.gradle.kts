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

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-opt-in=kotlinx.cinterop.BetaInteropApi")
                    freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
                }
            }
        }
    }

    android {
        namespace = "com.atruedev.kmpuwb"
        compileSdk =
            libs.versions.androidCompileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.androidMinSdk
                .get()
                .toInt()

        withHostTestBuilder {}.configure {}

        withDeviceTestBuilder {}.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "KmpUwb"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(project(":kmp-uwb-testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core)
            implementation(libs.androidx.core.uwb)
            implementation(libs.androidx.startup)
        }
        named("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.robolectric)
            implementation(libs.androidx.test.core)
        }
        named("androidDeviceTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
        }
    }
}

// KGP does not wire consumerProguardFiles for KMP android targets.
// https://youtrack.jetbrains.com/issue/KT-module-proguard (track upstream fix)
run {
    val taskName = "bundleAndroidMainAar"
    val rules = file("src/androidMain/consumer-rules.pro")
    tasks.withType<Zip>().matching { it.name == taskName }.configureEach {
        from(rules) { rename { "proguard.txt" } }
    }
    afterEvaluate {
        check(tasks.findByName(taskName) != null) {
            "Expected task '$taskName' not found — KGP may have renamed it. ProGuard rules will not be bundled."
        }
    }
}

tasks.register("assembleXCFramework") {
    dependsOn(
        "linkReleaseFrameworkIosArm64",
        "linkReleaseFrameworkIosSimulatorArm64",
        "linkReleaseFrameworkIosX64",
    )
    group = "build"
    description = "Assembles KmpUwb.xcframework from iOS release frameworks"

    val outputDir = layout.buildDirectory.dir("XCFrameworks/release")
    val arm64 = layout.buildDirectory.dir("bin/iosArm64/releaseFramework/KmpUwb.framework")
    val simArm64 = layout.buildDirectory.dir("bin/iosSimulatorArm64/releaseFramework/KmpUwb.framework")
    val simX64 = layout.buildDirectory.dir("bin/iosX64/releaseFramework/KmpUwb.framework")
    val fatSim = layout.buildDirectory.dir("bin/iosSimulatorFat/releaseFramework/KmpUwb.framework")

    doLast {
        outputDir.get().asFile.let { dir ->
            dir.deleteRecursively()
            dir.mkdirs()
        }

        val fatDir = fatSim.get().asFile
        fatDir.deleteRecursively()
        simArm64.get().asFile.copyRecursively(fatDir, overwrite = true)

        fun run(vararg args: String) {
            val result = ProcessBuilder(*args).inheritIO().start().waitFor()
            require(result == 0) { "${args.first()} failed with exit code $result" }
        }

        run(
            "lipo",
            "-create",
            File(simArm64.get().asFile, "KmpUwb").absolutePath,
            File(simX64.get().asFile, "KmpUwb").absolutePath,
            "-output",
            File(fatDir, "KmpUwb").absolutePath,
        )

        run(
            "xcodebuild",
            "-create-xcframework",
            "-framework",
            arm64.get().asFile.absolutePath,
            "-framework",
            fatDir.absolutePath,
            "-output",
            File(outputDir.get().asFile, "KmpUwb.xcframework").absolutePath,
        )
    }
}

dokka {
    dokkaPublications.html {
        moduleName.set("kmp-uwb")
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("com.atruedev", "kmp-uwb", version.toString())

    pom {
        name.set("kmp-uwb")
        description.set("Kotlin Multiplatform UWB library for Android, iOS, and JVM")
        url.set("https://github.com/gary-quinn/kmp-uwb")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        developers {
            developer {
                id.set("gary-quinn")
                name.set("Gary Quinn")
                email.set("gary@atruedev.com")
            }
        }
        scm {
            url.set("https://github.com/gary-quinn/kmp-uwb")
            connection.set("scm:git:git://github.com/gary-quinn/kmp-uwb.git")
            developerConnection.set("scm:git:ssh://github.com/gary-quinn/kmp-uwb.git")
        }
    }
}

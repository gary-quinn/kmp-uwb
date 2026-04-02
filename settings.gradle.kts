pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kmp-uwb"

include(":kmp-uwb-connector")
include(":kmp-uwb-testing")
include(":sample")
include(":sample-android")

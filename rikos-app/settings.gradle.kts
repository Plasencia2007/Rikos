pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "rikos-app"

include(":androidApp")
include(":composeApp")
include(":core:common")
include(":core:designsystem")
include(":feature:auth")
include(":feature:mesas")

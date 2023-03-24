pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "Tachyon"
include(":app")
include(":media")
include(":util")
include(":core")
include(":testutils")
include(":artworkFetcher")
include(":logger")
include(":playback-layers")
include(":playback-layers:database")
include(":playback-layers:artwork")
include(":playback-layers:permission")
include(":playback-layers:sort")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        // (optional) JetBrains mirror – not required, but harmless:
        // maven("https://maven.pkg.jetbrains.space/kotlin/p/ksp/maven")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Street Feast ROM"
include(":app")

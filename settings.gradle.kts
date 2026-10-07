pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "team-supervisor-app"
include(":core", ":wear", ":mobile", ":ui-tokens")

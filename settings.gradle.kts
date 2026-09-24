pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "claude-master-watch"
include(":core", ":wear", ":mobile", ":ui-tokens")

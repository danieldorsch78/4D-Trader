pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "4DMarketIntelligence"

include(":app")
include(":core-ui")
include(":core-network")
include(":core-database")
include(":core-common")
include(":domain")
include(":data")
include(":analytics")
include(":feature-watchlist")
include(":feature-market-overview")
include(":feature-correlations")
include(":feature-signals")
include(":feature-alerts")
include(":feature-world-clock")
include(":feature-settings")
include(":feature-github-sync")

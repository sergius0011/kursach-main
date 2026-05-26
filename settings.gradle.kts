pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                mavenCentral()
                // ПРАВИЛЬНАЯ СТРОКА ДЛЯ KOTLIN DSL:
                maven { url = java.net.URI("https://jitpack.io")

            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            google()
            mavenCentral() // Или mavenCentral() в зависимости от вашей стандартной конфигурации
            // ПРАВИЛЬНЫЙ СИНТАКСИС ДЛЯ KOTLIN DSL:
            maven { url = uri("https://jitpack.io") }
        }
    }

rootProject.name = "kursach"
include(":app")}
 
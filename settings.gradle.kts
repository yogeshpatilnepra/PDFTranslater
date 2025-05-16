pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        jcenter()
        maven {
            setUrl("https://jitpack.io")
//            setUrl("http://jcenter.bintray.com")
        }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven {
            setUrl("https://jitpack.io")
//            setUrl("http://jcenter.bintray.com")
        }

        gradlePluginPortal()
    }
}

rootProject.name = "PdfTranslatorJetpack"
include(":app")

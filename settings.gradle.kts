pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/integral-testing/metatest-rest-java")
            credentials {
                username = settings.providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GPR_USER")
                password = settings.providers.gradleProperty("gpr.token").orNull
                    ?: System.getenv("GPR_TOKEN")

            }
        }
    }
}

rootProject.name = "metatest-rest-java-example"

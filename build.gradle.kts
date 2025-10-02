plugins {
    id("java")
}

group = "io.example.java.rest.metatest"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/integral-testing/metatest-rest-java")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GPR_TOKEN")
        }
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("io.rest-assured:rest-assured:5.3.0")
    implementation("io.rest-assured:json-path:5.3.0")
    implementation("io.metatest:metatest:1.0.0-dev-dac8a0f")
}

tasks.test {
    useJUnitPlatform()
    val aspectjAgent = configurations.runtimeClasspath.get().find { it.name.contains("aspectjweaver") }?.absolutePath
    val runWithMetatest = System.getProperty("runWithMetatest") == "true"

    val jvmArguments = mutableListOf(
        "-Xmx2g",
        "-Xms512m"
    )

    if (runWithMetatest && aspectjAgent != null) {
        jvmArguments.add("-javaagent:${aspectjAgent}")
        // jvmArguments.addAll(listOf(
        //     "-Daj.weaving.verbose=true",
        //     "-Dorg.aspectj.weaver.showWeaveInfo=true",
        //     "-Dorg.aspectj.matcher.verbosity=5"
        // ))
    }
    jvmArguments.add("-DrunWithMetatest=${System.getProperty("runWithMetatest")}")

    jvmArgs = jvmArguments
}
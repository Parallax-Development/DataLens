import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
@Suppress("DSL_SCOPE_VIOLATION")

plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "dev.darkblade.datalens"
version = providers.gradleProperty("pluginVersion").getOrElse("1.0.0-SNAPSHOT")

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper API — provided at runtime by the server
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    // Bundled at runtime in Paper — declare for IDE only
    compileOnly("org.yaml:snakeyaml:2.2")

    // Shaded libraries
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Tests
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
    }

    // Disable plain jar — only produce the shadow fat-jar
    jar {
        enabled = false
    }

    named<ShadowJar>("shadowJar") {
        archiveClassifier = ""
        archiveBaseName = "DataLens"

        // Note: relocation disabled — shadow's ASM cannot process Java 21 bytecode (version 65).
        // Jackson and Caffeine are NOT bundled by Paper, so no classloader conflicts exist.
        // Re-enable relocation once shadow ships an ASM version ≥ 9.7.

        manifest {
            attributes(
                "Implementation-Title" to "DataLens",
                "Implementation-Version" to project.version
            )
        }
    }

    // runServer task provided by run-paper plugin
    runServer {
        minecraftVersion("1.20.4")
    }

    test {
        useJUnitPlatform()
    }

    // Make 'build' produce the shadow jar
    build {
        dependsOn(shadowJar)
    }
}

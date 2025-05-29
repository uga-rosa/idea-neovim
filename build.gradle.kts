import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellij.platform)
    alias(libs.plugins.ktlint)
}

group = "com.ugarosa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    implementation(libs.msgpack)

    intellijPlatform {
        create("IC", "2025.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        bundledPlugin("com.intellij.java")
    }

    testImplementation(libs.bundles.test)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes =
            """
            Initial version
            """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}

tasks.named<RunIdeTask>("runIde") {
    systemProperties["idea.log.trace.categories"] = "#com.ugarosa.neovim"
    systemProperties["intellij.platform.log.sync"] = true
}

tasks.prepareSandbox {
    from("src/main/resources/runtime") {
        into("${rootProject.name}/runtime")
    }
}

tasks.test {
    useJUnitPlatform()
}

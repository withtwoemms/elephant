import org.jetbrains.kotlin.com.intellij.openapi.vfs.StandardFileSystems.jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.tasks.Jar

kotlinProject()

plugins {
    kotlin("jvm").version(Versions.kotlin)
    application
}

dependencies {
    testImplementation(exampleDeps["junitApi"])
    testRuntimeOnly(exampleDeps["junitEngine"])
    println("DEPS: $exampleDeps")
    println("DEP_GROUP_CONTAINS_JUNITAPI: ${exampleDeps.contains("junitApi")}")
}

application {
    mainClassName = "io.withtwoemms.github.example.ExampleKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jvmTarget
    }
}

tasks {
    val fatJar by registering(type = Jar::class) {
        baseName = "${project.name}-fat"
        manifest {
            attributes["Main-Class"] = "io.withtwoemms.github.example.ExampleKt"
        }
        from(configurations.runtime.map({ if (it.isDirectory) it else zipTree(it) }))
        with(tasks["jar"] as CopySpec)
    }

    "build" {
        dependsOn(fatJar)
    }
}
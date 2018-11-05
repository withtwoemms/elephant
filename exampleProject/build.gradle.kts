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
    println("DEPS: $exampleDeps")
    println("PROJECT_CONTAINS: ${project.contains("junitApi")}")
    println("DEP_GROUP_CONTAINS: ${exampleDeps.contains("junitApi")}")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jvmTarget
    }
}

application {
    mainClassName = "io.withtwoemms.github.example.ExampleKt"
}

val fatJar = task("fatJar", type = Jar::class) {
    baseName = "${project.name}-fat"
    manifest {
        attributes["Main-Class"] = "io.withtwoemms.github.example.ExampleKt"
    }
    from(configurations.runtime.map({ if (it.isDirectory) it else zipTree(it) }))
    with(tasks["jar"] as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
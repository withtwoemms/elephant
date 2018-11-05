import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.repositories
import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * Configures the current project as a Kotlin project by adding the Kotlin `stdlib` as a dependency.
 */
fun Project.kotlinProject() {
    dependencies {
        "compile"(kotlin("stdlib", Versions.kotlin))
    }
    repositories {
        mavenCentral()
    }
}

interface DependencyItem {
    operator fun get(key: String): Any
}

data class DependencyNotation(private val notation: String) : CharSequence by notation, DependencyItem {
    override operator fun get(key: String): String = notation
    override fun toString() = notation
}

class DependencyGroup : DependencyItem {
    private val dependencies = mutableMapOf<String, DependencyItem>()

    override operator fun get(key: String): DependencyItem = key.split('.')
            .fold(this as DependencyItem) { acc, k ->
                (acc as? DependencyGroup)?.getItem(k) ?: acc
            }

    private fun getItem(key: String): DependencyItem
            = dependencies[key] ?: throw IllegalArgumentException(
            "dependency `$key` not found in group $this")

    operator fun set(key: String, item: DependencyItem) = dependencies.set(key, item)

    operator fun set(key: String, notation: String) = dependencies.set(key, DependencyNotation(notation))

    operator fun contains(key: String): Boolean = key in dependencies

    operator fun invoke(config: DependencyGroup.() -> Unit): DependencyGroup = apply(config)

    operator fun String.invoke(notation: String) = set(this, notation)

    operator fun String.invoke(vararg dependencies: Pair<String, Any>) = set(this, create(*dependencies))

    operator fun String.invoke(init: DependencyGroup.() -> Unit) = set(this, create().apply(init))

    override fun toString() = dependencies.toString()

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun create(dependencies: Map<String, Any>): DependencyGroup {
            val inst = DependencyGroup()
            dependencies.forEach {
                val (key, item) = it
                when (item) {
                    is String -> inst[key] = item
                    is DependencyItem -> inst[key] = item
                    is Map<*, *> -> inst[key] = create(item as Map<String, Any>)
                    else -> throw IllegalArgumentException("Unsupported dependency item type of `$item`")
                }
            }
            return inst
        }

        private fun create(vararg dependencies: Pair<String, Any>): DependencyGroup
                = create(mapOf(*dependencies))
    }
}

val ExtraPropertiesExtension.deps: DependencyGroup
    get() =
        if (has("deps")) {
            this["deps"] as DependencyGroup
        } else {
            DependencyGroup().apply {
                this@deps["deps"] = this
            }
        }

val Project.exampleDeps: DependencyGroup get() = extra.deps {
    "okHttp3"("com.squareup.okhttp3:${Versions.okHttp3}")
    "kotlinGradlePlugin"("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
    "junitApi"("org.junit.jupiter:junit-jupiter-api:${Versions.junit}") // for "testImplementation"
    "junitEngine"("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}") // for "testRuntimeOnly"
}

operator fun Project.contains(propertyName: String): Boolean = hasProperty(propertyName)

fun Project.loadProperties(path: String, extra: ExtraPropertiesExtension)
        = loadProperties(file(path), extra)

fun Project.loadProperties(file: File, extra: ExtraPropertiesExtension) {
    if (!file.exists()) return
    Properties().apply {
        load(FileInputStream(file))
        forEach { (k, v) ->
            extra["$k"] = v
        }
    }
}

fun DependencyHandler.kotlin(module: CharSequence) = kotlin(module.toString())

fun DependencyHandler.kotlin(module: DependencyItem)
        = kotlin(module as? CharSequence
        ?: throw IllegalArgumentException("Unexpected module type for $module"))

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.Property
import java.io.File

abstract class JsPackagesExtension {
    /** Name of the npm package containing all the non-project JS modules (Kotlin stdlib, coroutines, wrappers, ...). */
    abstract val utilsPackage: Property<String>
}

/** Names of the project components reachable from this one (including itself). */
fun ResolvedComponentResult.projectModuleNames(): Set<String> {
    val visited = mutableSetOf<ResolvedComponentResult>()
    val queue = ArrayDeque(listOf(this))
    while (queue.isNotEmpty()) {
        val component = queue.removeFirst()
        if (!visited.add(component)) continue
        component.dependencies.filterIsInstance<ResolvedDependencyResult>().mapTo(queue) { it.selected }
    }
    return visited.mapNotNullTo(sortedSetOf()) { (it.id as? ProjectComponentIdentifier)?.projectName }
}

// Kotlin/JS puts all the per-module .mjs files into one flat directory and links them by relative paths
// (`./greetingLogic.mjs`). The splitting below turns every JS module of the project into a separate npm package
// (`<dist>/<module>/package.json`) and rewrites the relative imports into package imports (`greetingLogic`).
// All the other modules (Kotlin stdlib, coroutines, wrappers, ...) are put together into a single
// utils package and imported from it by path (`kotlin-utils/kotlin-kotlin-stdlib.mjs`).
object JsPackages {
    private val relativeImport = Regex("""(\bfrom\s*|\bimport\s*\(?\s*)(['"])\./([^'"/]+)\.mjs\2""")

    fun split(dist: File, projectModules: Set<String>, utilsPackage: String) {
        val rootPackageJson = File(dist, "package.json")
        @Suppress("UNCHECKED_CAST")
        val rootPackage = JsonSlurper().parse(rootPackageJson) as MutableMap<String, Any?>
        val rootModule = (rootPackage["main"] as String).removeSuffix(".mjs")
        val version = rootPackage["version"] as String
        rootPackageJson.delete()

        val files = dist.listFiles()!!.filter { it.isFile }.groupBy { it.name.substringBefore('.') }
        val utilsDir = File(dist, utilsPackage)
        for ((module, moduleFiles) in files) {
            val isProjectModule = module in projectModules
            val packageDir = (if (isProjectModule) File(dist, module) else utilsDir).apply { mkdirs() }
            val dependencies = sortedSetOf<String>()
            for (file in moduleFiles) {
                val target = File(packageDir, file.name)
                if (file.name.endsWith(".mjs") || file.name.endsWith(".d.mts")) {
                    target.writeText(relativeImport.replace(file.readText()) {
                        val (prefix, quote, dependency) = it.destructured
                        when {
                            dependency in projectModules -> {
                                dependencies += dependency
                                "$prefix$quote$dependency$quote"
                            }
                            // modules inside the utils package keep referring to each other relatively
                            !isProjectModule -> it.value
                            else -> {
                                dependencies += utilsPackage
                                "$prefix$quote$utilsPackage/$dependency.mjs$quote"
                            }
                        }
                    })
                    file.delete()
                } else {
                    file.renameTo(target)
                }
            }
            if (!isProjectModule) continue

            val packageJson = if (module == rootModule) rootPackage else linkedMapOf<String, Any?>(
                "name" to module,
                "version" to version,
                "main" to "$module.mjs",
            ).apply { if (File(packageDir, "$module.d.mts").exists()) put("types", "$module.d.mts") }
            @Suppress("UNCHECKED_CAST")
            val packageDependencies = (packageJson["dependencies"] as? Map<String, Any?>).orEmpty().toMutableMap()
            dependencies.forEach { packageDependencies[it] = version }
            packageJson["dependencies"] = packageDependencies
            writePackageJson(packageDir, packageJson)
        }

        if (utilsDir.exists()) {
            writePackageJson(utilsDir, linkedMapOf("name" to utilsPackage, "version" to version))
        }
    }

    private fun writePackageJson(packageDir: File, content: Map<String, Any?>) {
        File(packageDir, "package.json").writeText(JsonOutput.prettyPrint(JsonOutput.toJson(content)))
    }
}

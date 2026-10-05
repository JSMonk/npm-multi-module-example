// Convention for an aggregating ("proxy") Kotlin/JS module: it is built as a JS library and
// its distribution is split into npm packages, one per project module, plus a single package with
// all the Kotlin libraries (see `JsPackages`).
plugins {
    id("js-module")
}

val jsPackages = extensions.create<JsPackagesExtension>("jsPackages").apply {
    utilsPackage.convention("kotlin-utils")
}

kotlin {
    js {
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target = "es2015"
            optIn.add("kotlin.js.ExperimentalJsExport")
        }
    }
}

// JS module names of this module and of all the project modules it (transitively) depends on.
// They are the project names, because `js-module` uses the project name as `outputModuleName`.
val projectModules: Provider<Set<String>> = configurations.named("jsRuntimeClasspath")
    .flatMap { it.incoming.resolutionResult.rootComponent }
    .map { it.projectModuleNames() }

tasks.matching { it.name.startsWith("js") && it.name.endsWith("LibraryDistribution") }.configureEach {
    val modules = projectModules
    val utilsPackage = jsPackages.utilsPackage
    inputs.property("jsPackages.projectModules", modules)
    inputs.property("jsPackages.utilsPackage", utilsPackage)
    // The distribution task doesn't remove stale outputs, so start from an empty directory
    doFirst { outputs.files.singleFile.deleteRecursively() }
    doLast { JsPackages.split(outputs.files.singleFile, modules.get(), utilsPackage.get()) }
}

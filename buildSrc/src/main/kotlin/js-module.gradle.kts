// Convention for every Kotlin/JS module of the project.
// The JS module name (`outputModuleName`) is always the Gradle project name, so the module can be
// referred by it (both as a JS module and as an npm package, see `js-library`).
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    js {
        outputModuleName = project.name
        browser()
        compilerOptions {
            optIn.add("kotlin.js.ExperimentalJsExport")
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

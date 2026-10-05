plugins {
    id("js-library")
}

// Proxy module: it only contains the exported API and aggregates the sub-modules
// into JS libraries (one npm package per module) consumed by the webApp.
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":greetingLogic"))
            implementation(project(":platformLogic"))
        }
    }
}

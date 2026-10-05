plugins {
    id("js-module")
}

kotlin {
    sourceSets {
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}

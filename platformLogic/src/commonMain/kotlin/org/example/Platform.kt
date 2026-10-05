package org.example

@JsExport
interface Platform {
    val name: String
}

@JsExport
expect fun getPlatform(): Platform

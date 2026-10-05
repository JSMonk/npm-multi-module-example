This is a Kotlin Multiplatform project targeting Web.

* [/platformLogic](./platformLogic/src) contains the platform detection logic (`Platform`, `getPlatform`).

* [/greetingLogic](./greetingLogic/src) contains the greeting formatting logic (`sayHello`).

* [/sharedLogic](./sharedLogic/src) is a proxy module: it depends on `platformLogic` and `greetingLogic`
  as `implementation` dependencies, contains only the exported (`@JsExport`) API, and declares
  `binaries.library()`, so all the sub-modules are built through it. After the build, every project JS module
  (`sharedLogic`, `greetingLogic`, `platformLogic`) is placed into its own npm package
  under `sharedLogic/build/dist/js/<mode>Library/<module>/` and imported by its package name. All the Kotlin
  libraries (stdlib, coroutines, wrappers, ...) are placed together into the `kotlin-utils` package
  (`sharedLogic/build/dist/js/<mode>Library/kotlin-utils/`) and imported as `kotlin-utils/<library>.mjs`.

* [/webApp](./webApp) contains a React web application. It uses the Kotlin/JS library produced
  by the [sharedLogic](./sharedLogic) module.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Web app:
  1. Install [Node.js](https://nodejs.org/en/download) (which includes `npm`)
  2. Build and run the web application:
     ```shell
     npm run build:shared
     npm install
     npm run start
     ```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
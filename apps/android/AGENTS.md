# Repository Guidelines

## Required Workflow

Before editing code, read [Android architecture](docs/architecture.md) in full; follow it during implementation and review. Before integration changes, read `../../AGENTS.md`, `../../README.md`, and relevant parent architecture/Rust documents.

Before UI or user-flow changes, read and follow [UI/UX guidelines](docs/ui-ux.md).

For styling, follow [design tokens](docs/design-system.md). Use MaterialTheme and Dme tokens; keep the fixed dark theme and avoid feature-local colors, spacing, or type overrides. Do not introduce shared UI components.

Change agreed decisions only on explicit user instruction. Never weaken rules, suppress checks, or rewrite documentation to justify violations. Explain conflicts before implementation; explicit user instructions take precedence.

## Project Structure

This Kotlin/Compose client communicates with BMW ECUs, initially MS43. `../../` owns the shared Rust implementation.

- `app/` (`:androidApp`): Activity, Application, DI root, service, permissions, files and wake lock.
- `desktopApp/`: macOS window, DI root, operation lifetime, files, journal and sleep protection.
- `shared/src/commonMain/kotlin/dev/coldcat/dmelab/`: features, orchestration, CSV, navigation and theme.
- `shared/src/commonMain/composeResources/`: shared strings/images; `shared/src/androidMain/`: system UI adapters.
- `ecu/src/commonMain/`: hardware API/models; `ecu/src/androidMain/`: USB/UniFFI; `ecu/src/desktopMain/`: serial/UniFFI.
- `app/src/main/res/`: Android integration resources, including notification/launcher strings.
- `app/src/test/`: JVM tests; `app/src/androidTest/`: device tests.
- `gradle/libs.versions.toml`: versions; `app/build.gradle.kts`: Android configuration.

Keep dependencies `:androidApp / :desktopApp → :shared → :ecu`; apps may also depend on `:ecu`. Both libraries have Android and JVM `desktop` targets; macOS currently uses ARM64 bindings. Follow [the multiplatform architecture](docs/multiplatform-architecture.md). Keep features vertically packaged with local `data`, `domain`, `presentation`, and `ui`. No feature or layer Gradle modules. Common code must not use Android/JVM/FFI APIs: use KMP libraries or narrow platform contracts. Libraries must not depend on apps; generated/native types must not escape ECU platform implementations.

UniFFI sources/native libraries come from `../../dist/android` or `../../dist/desktop`, never both in one compilation. Never edit generated bindings. See [Android build](docs/build.md) and [desktop build](docs/desktop.md).

## Build, Test, and Development

Run here with the configured JDK 21 Gradle toolchain and required Android SDK packages.

- `./gradlew :androidApp:assembleDebug`: build `app/build/outputs/apk/debug/dme-lab-debug.apk`.
- `./gradlew :androidApp:installDebug`: install; launch through Android Studio or launcher.
- `./gradlew :androidApp:lintDebug :shared:lint :ecu:lint`: run application and KMP library Lint.
- `./gradlew :androidApp:testDebugUnitTest`: run existing JVM tests.
- `./gradlew :androidApp:connectedDebugAndroidTest`: run existing device tests.
- `./gradlew :desktopApp:run`: launch macOS from sources.
- `./gradlew :desktopApp:createDistributable :desktopApp:packageDmg`: package macOS with its JVM.

## Architecture and Coding Style

Use official Kotlin style and `.editorconfig`: four spaces, PascalCase types/Composables, camelCase functions/properties, snake_case resources. Format with Android Studio.

Prefer internal visibility, constructor injection, one production top-level type per file, exhaustive sealed/enum branches, and named arguments. Avoid utility collections and proxy-only UseCases.

Use Koin Annotations, one Koin module per application, and `@ComponentScan("dev.coldcat.dmelab")`. Apply the Koin compiler plugin to libraries for cross-module discovery; do not manually duplicate registrations. Use Voyager behind a typed Router. Resolve dependencies at composition roots and screen containers. Desktop window/Quit must await active flash completion; Live Data stops cooperatively before exit.

Screens expose immutable `ScreenState`, accept `ScreenEvent`, and deliver transient effects as `ScreenCommand`. ViewModel owns `InternalState`; a stateless Mapper derives display state. Separate lifecycle-aware containers from pure Compose content.

Keep protocols, checksums, and flash rules in Rust. Execute blocking calls outside the UI thread, serialize session operations, and preserve cooperative cancellation. Never erase/write the bootloader.

## Testing Guidelines

Do not write or add tests, including essential or coverage-driven tests. Only explicit user instruction changes this freeze. Run relevant existing checks; report hardware verification separately.

## Code Review Rules

Before finishing, review changed code against the architecture: module/package/source-set boundaries, DI/navigation, screen state, session ownership, and cancellation. Fix violations. For code/build/resource changes, run `./gradlew :androidApp:assembleDebug :androidApp:lintDebug :shared:lint :ecu:lint` and relevant existing tests. Keep the standalone Android Lint plugin enabled in KMP libraries. Verify release with R8 when moving native packaging or consumer rules. Documentation-only changes require link/content review. Report changes, checks actually run, failures, and skipped checks with reasons. Never claim unperformed hardware verification.

## Commits and Pull Requests

Use short, action-oriented subjects. Describe purpose, changes, checks, and linked issues; include screenshots for UI changes. Exclude secrets, build output, and `local.properties`.

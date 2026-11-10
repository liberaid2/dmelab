import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.koin.compiler)
}

val dmelabArtifacts = rootProject.layout.projectDirectory.dir("../../dist/android")
val dmelabAbis = listOf("arm64-v8a", "x86_64")
val desktopArtifacts = rootProject.layout.projectDirectory.dir("../../dist/desktop")
val verifyDesktopBindings by tasks.registering {
    group = "verification"
    description = "Verify matching UniFFI JVM bindings and the macOS native library."
    val api = desktopArtifacts.file("kotlin/dev/coldcat/dmelab/ffi/dmelab_ffi.kt")
    val info = desktopArtifacts.file("BUILDINFO")
    val architecture = if (System.getProperty("os.arch") in listOf("aarch64", "arm64")) "darwin-aarch64" else "darwin-x86-64"
    val library = desktopArtifacts.file("resources/$architecture/libdmelab_ffi.dylib")
    inputs.files(api, info, library)
    doLast {
        check(listOf(api, info, library).all { it.asFile.isFile }) {
            "Missing desktop UniFFI artifacts for $architecture. Run bash scripts/build-desktop.sh from the repository root."
        }
        check(info.asFile.readLines().contains("UniFFI: 0.32.2")) { "Rebuild desktop Kotlin and native artifacts together with UniFFI 0.32.2." }
    }
}
val verifyDmelabBindings by tasks.registering {
    group = "verification"
    description = "Verify that the matching UniFFI Kotlin and Android libraries are available."
    val kotlinApi = dmelabArtifacts.file("kotlin/dev/coldcat/dmelab/ffi/dmelab_ffi.kt")
    val buildInfo = dmelabArtifacts.file("BUILDINFO")
    val nativeLibraries = dmelabAbis.map { dmelabArtifacts.file("jniLibs/$it/libdmelab_ffi.so") }
    inputs.files(kotlinApi, buildInfo, nativeLibraries)
    doLast {
        val required = listOf(kotlinApi, buildInfo) + nativeLibraries
        val missing = required.filterNot { it.asFile.isFile }
        check(missing.isEmpty()) {
            "Missing dmelab bindings: ${missing.joinToString { it.asFile.path }}. " +
                "Run bash scripts/build-android.sh from the dmelab repository root."
        }
        check(buildInfo.asFile.readText().lineSequence().any { it == "UniFFI: 0.32.2" }) {
            "Expected UniFFI 0.32.2 artifacts. Rebuild Kotlin and native libraries together."
        }
    }
}

kotlin {
    jvm("desktop") {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
    }
    android {
        namespace = "dev.coldcat.dmelab.ecu"
        compileSdk = 37
        minSdk = 27
        withHostTest { }
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
        optimization.consumerKeepRules.apply {
            publish = true
            file("consumer-rules.pro")
        }
    }
    sourceSets {
        val desktopMain by getting {
            kotlin.srcDir(desktopArtifacts.dir("kotlin"))
            resources.srcDir(desktopArtifacts.dir("resources"))
            dependencies {
                implementation(libs.koin.annotations)
                implementation(libs.jna)
            }
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain {
            kotlin.srcDir(dmelabArtifacts.dir("kotlin"))
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.koin.annotations)
                implementation("${libs.jna.get()}@aar")
                implementation(libs.usb.serial)
            }
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addStaticSourceDirectory(dmelabArtifacts.dir("jniLibs").asFile.path)
    }
}

tasks.matching { it.name == "compileAndroidMain" || it.name == "preBuild" }.configureEach {
    dependsOn(verifyDmelabBindings)
}

tasks.named("compileKotlinDesktop") { dependsOn(verifyDesktopBindings) }

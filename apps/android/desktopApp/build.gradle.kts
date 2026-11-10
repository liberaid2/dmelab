import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":ecu"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.compose.resources)
    implementation(libs.multiplatform.lifecycle.viewmodel.compose)
    implementation(libs.multiplatform.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.annotations)
}

compose.desktop {
    application {
        mainClass = "dev.coldcat.dmelab.MainKt"
        // Android Studio's bundled JBR may omit jpackage; allow a full JDK for packaging.
        javaHome = providers.gradleProperty("desktopJavaHome").orElse(System.getProperty("java.home")).get()
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "DME Lab"
            packageVersion = "1.0.0"
            modules("java.sql", "java.management", "jdk.unsupported")
            macOS {
                bundleID = "dev.coldcat.dmelab"
            }
        }
    }
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.jetbrains.intellij.platform.module")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    intellijPlatform {
        bundledPlugins(
            "org.jetbrains.kotlin",
            "com.intellij.gradle",
        )
    }
}

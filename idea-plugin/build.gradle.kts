plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.jetbrains.intellij.platform.module")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    compileOnly(projects.common)
    compileOnly(libs.bytebuddy)
    compileOnly(libs.bytebuddyAgent)
    intellijPlatform {
        bundledPlugins(
            "org.jetbrains.kotlin",
            "com.intellij.gradle",
        )
    }
}

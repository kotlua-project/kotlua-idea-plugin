/*
 * Copyright 2026 The kotlua Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.Properties

plugins {
    alias(libs.plugins.intellij.platform)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val localIdePath: String? = localProperties.getProperty("local.ide.path")

dependencies {
    implementation(projects.common)
    implementation(libs.bytebuddy)
    implementation(libs.bytebuddyAgent)
    intellijPlatform {
        pluginComposedModule(projects.common)
        pluginComposedModule(projects.ideaPlugin)
        if (!localIdePath.isNullOrEmpty()) {
            local(localIdePath)
        } else {
            intellijIdea("2026.2.2")
        }
        bundledPlugins(
            "org.jetbrains.kotlin",
            "com.intellij.gradle",
        )
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "io.github.kotlua"
        name = "kotlua"
        version = "0.1.0"
        vendor {
            name = "Kotlua Authors"
        }
    }
}

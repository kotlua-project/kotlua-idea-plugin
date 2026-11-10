package io.github.kotlua.gradle.model

import java.io.File
import java.io.Serializable

interface KotluaSourceSet : Serializable {
    val name: String
    val sourceDirs: Set<File>
    val resourceDirs: Set<File>
    val isTest: Boolean
    val dependsOn: Set<String>
    val additionalVisibleSourceSets: Set<String>
    val languageSettings: KotlinLanguageSettings
}

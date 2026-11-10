package io.github.kotlua.gradle.model

import java.io.File
import java.io.Serializable

interface KotluaCompilation : Serializable {
    val name: String
    val sourceSetName: String
    val outputDir: File
    val compilerArguments: List<String>
    val compileClasspath: Set<File>
}

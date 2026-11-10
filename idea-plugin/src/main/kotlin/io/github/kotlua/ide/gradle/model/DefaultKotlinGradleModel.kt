package io.github.kotlua.ide.gradle.model

import io.github.kotlua.gradle.model.KotlinLanguageSettings
import io.github.kotlua.gradle.model.KotluaCompilation
import io.github.kotlua.gradle.model.KotluaGradleModel
import io.github.kotlua.gradle.model.KotluaSourceSet
import java.io.File

data class DefaultKotlinLanguageSettings(
    override val languageVersion: String? = null,
    override val apiVersion: String? = null,
    override val isProgressiveMode: Boolean = false,
    override val enabledLanguageFeatures: Set<String> = emptySet(),
    override val optInAnnotationsInUse: Set<String> = emptySet(),
    override val compilerPluginArguments: Array<String> = emptyArray(),
    override val compilerPluginClasspath: Set<File> = emptySet(),
    override val freeCompilerArgs: Array<String> = emptyArray()
) : KotlinLanguageSettings {

    constructor(copy: KotlinLanguageSettings) : this(
        languageVersion = copy.languageVersion,
        apiVersion = copy.apiVersion,
        isProgressiveMode = copy.isProgressiveMode,
        enabledLanguageFeatures = copy.enabledLanguageFeatures.toSet(),
        optInAnnotationsInUse = copy.optInAnnotationsInUse.toSet(),
        compilerPluginArguments = copy.compilerPluginArguments.clone(),
        compilerPluginClasspath = copy.compilerPluginClasspath.map { File(it.path) }.toSet(),
        freeCompilerArgs = copy.freeCompilerArgs.clone()
    )
}

data class DefaultKotluaSourceSet(
    override val name: String,
    override val sourceDirs: Set<File>,
    override val resourceDirs: Set<File> = emptySet(),
    override val dependsOn: Set<String> = emptySet(),
    override val additionalVisibleSourceSets: Set<String> = emptySet(),
    override val isTest: Boolean = false,
    override val languageSettings: KotlinLanguageSettings = DefaultKotlinLanguageSettings()
) : KotluaSourceSet {

    constructor(copy: KotluaSourceSet) : this(
        name = copy.name,
        sourceDirs = copy.sourceDirs.map { File(it.path) }.toSet(),
        resourceDirs = copy.resourceDirs.map { File(it.path) }.toSet(),
        dependsOn = copy.dependsOn.toSet(),
        additionalVisibleSourceSets = copy.additionalVisibleSourceSets.toSet(),
        isTest = copy.isTest,
        languageSettings = DefaultKotlinLanguageSettings(copy.languageSettings)
    )
}

data class DefaultKotluaCompilation(
    override val name: String,
    override val sourceSetName: String,
    override val outputDir: File,
    override val compilerArguments: List<String>,
    override val compileClasspath: Set<File>
) : KotluaCompilation {

    constructor(copy: KotluaCompilation) : this(
        name = copy.name,
        sourceSetName = copy.sourceSetName,
        outputDir = File(copy.outputDir.path),
        compilerArguments = copy.compilerArguments.toList(),
        compileClasspath = copy.compileClasspath.map { File(it.path) }.toSet()
    )
}

data class DefaultKotluaGradleModel(
    override val kotluaPluginVersion: String,
    override val sourceSetsByName: Map<String, KotluaSourceSet>,
    override val compilationsBySourceSetName: Map<String, KotluaCompilation>
) : KotluaGradleModel {

    constructor(copy: KotluaGradleModel) : this(
        kotluaPluginVersion = copy.kotluaPluginVersion,
        sourceSetsByName = copy.sourceSetsByName.mapValues { (_, sourceSetProxy) ->
            DefaultKotluaSourceSet(sourceSetProxy)
        },
        compilationsBySourceSetName = copy.compilationsBySourceSetName.mapValues { (_, compilationProxy) ->
            DefaultKotluaCompilation(compilationProxy)
        }
    )
}

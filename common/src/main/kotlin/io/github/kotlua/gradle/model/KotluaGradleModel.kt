package io.github.kotlua.gradle.model

import java.io.Serializable

interface KotluaGradleModel : Serializable {
    val kotluaPluginVersion: String
    val sourceSetsByName: Map<String, KotluaSourceSet>
    val compilationsBySourceSetName: Map<String, KotluaCompilation>

    val sourceSets: List<KotluaSourceSet>
        get() = sourceSetsByName.values.toList()

    fun resolveAllDependsOn(sourceSet: KotluaSourceSet): Set<String> {
        val result = mutableSetOf<String>()
        fun collect(current: KotluaSourceSet) {
            for (depName in current.dependsOn) {
                if (result.add(depName)) {
                    sourceSetsByName[depName]?.let { collect(it) }
                }
            }
        }
        collect(sourceSet)
        return result
    }
}

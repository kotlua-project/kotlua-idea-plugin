package io.github.kotlua.ide.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ContentRootData
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ModuleDependencyData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.roots.DependencyScope
import io.github.kotlua.gradle.model.KotluaGradleModel
import io.github.kotlua.gradle.model.KotluaModelProvider
import io.github.kotlua.ide.gradle.model.DefaultKotluaGradleModel
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.util.GradleConstants

class KotluaProjectResolverExtension : AbstractProjectResolverExtension() {

    companion object {
        val KOTLUA_MODEL_KEY = Key.create(KotluaGradleModel::class.java, 100)
    }

    override fun getExtraProjectModelClasses(): Set<Class<*>> {
        error("getModelProvider() is overridden instead")
    }

    override fun getModelProvider(): ProjectImportModelProvider {
        return KotluaModelProvider()
    }

    override fun getToolingExtensionsClasses(): Set<Class<*>> {
        return setOf(KotluaGradleModel::class.java)
    }

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val model = resolverCtx.getExtraProject(gradleModule, KotluaGradleModel::class.java)
        if (model != null && model.sourceSets.isNotEmpty()) {
            ideModule.createChild(KOTLUA_MODEL_KEY, DefaultKotluaGradleModel(model))
        }
        super.populateModuleExtraModels(gradleModule, ideModule)
    }

    override fun populateModuleCompileOutputSettings(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        super.populateModuleCompileOutputSettings(gradleModule, ideModule)
        val model = resolverCtx.getExtraProject(gradleModule, KotluaGradleModel::class.java)
        if (model != null) {
            val outDir = model.compilationsBySourceSetName["luaMain"]?.outputDir
            if (outDir != null) {
                val luaSourceSetNode = ExternalSystemApiUtil.findAll(ideModule, GradleSourceSetData.KEY)
                    .firstOrNull { it.data.moduleName == "luaMain" }

                if (luaSourceSetNode != null) {
                    luaSourceSetNode.data.useExternalCompilerOutput(true)
                    luaSourceSetNode.data.setCompileOutputPath(ExternalSystemSourceType.SOURCE, outDir.absolutePath)
                }
            }
        }
    }

    override fun populateModuleContentRoots(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        super.populateModuleContentRoots(gradleModule, ideModule)

        val model = resolverCtx.getExtraProject(gradleModule, KotluaGradleModel::class.java) ?: return
        val sourceSetsNodes = ExternalSystemApiUtil.findAll(ideModule, GradleSourceSetData.KEY)
            .associateBy { it.data.moduleName }

        for ((sourceSetName, sourceSetModel) in model.sourceSetsByName) {
            val node = sourceSetsNodes[sourceSetName] ?: continue

            for (sourceDir in sourceSetModel.sourceDirs) {
                val contentRoot = ContentRootData(GradleConstants.SYSTEM_ID, sourceDir.absolutePath)
                contentRoot.storePath(ExternalSystemSourceType.SOURCE, sourceDir.absolutePath)
                node.createChild(ProjectKeys.CONTENT_ROOT, contentRoot)
            }

            for (resourceDir in sourceSetModel.resourceDirs) {
                val contentRoot = ContentRootData(GradleConstants.SYSTEM_ID, resourceDir.absolutePath)
                contentRoot.storePath(ExternalSystemSourceType.RESOURCE, resourceDir.absolutePath)
                node.createChild(ProjectKeys.CONTENT_ROOT, contentRoot)
            }
        }
    }

    override fun populateModuleDependencies(
        gradleModule: IdeaModule,
        ideModule: DataNode<ModuleData>,
        ideProject: DataNode<ProjectData>
    ) {
        super.populateModuleDependencies(gradleModule, ideModule, ideProject)

        val model = resolverCtx.getExtraProject(gradleModule, KotluaGradleModel::class.java) ?: return
        val sourceSetsMap = ExternalSystemApiUtil.getChildren(ideModule, GradleSourceSetData.KEY)
            .associateBy { it.data.moduleName }

        for ((sourceSetName, sourceSet) in model.sourceSetsByName) {
            val fromNode = sourceSetsMap[sourceSetName] ?: continue

            for (targetSourceSetName in sourceSet.dependsOn) {
                val toNode = sourceSetsMap[targetSourceSetName] ?: continue
                val moduleDependency = ModuleDependencyData(fromNode.data, toNode.data)
                moduleDependency.scope = DependencyScope.COMPILE
                moduleDependency.isExported = false
                fromNode.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependency)
            }
        }
    }
}

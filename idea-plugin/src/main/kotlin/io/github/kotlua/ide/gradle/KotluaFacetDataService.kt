@file:Suppress("UnstableApiUsage")

package io.github.kotlua.ide.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.service.project.manage.SourceFolderManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import io.github.kotlua.gradle.model.KotlinLanguageSettings
import io.github.kotlua.gradle.model.KotluaGradleModel
import io.github.kotlua.ide.gradle.model.DEFAULT_LUA_COMMON_SOURCE_SET_NAME
import io.github.kotlua.ide.gradle.model.DEFAULT_LUA_MAIN_SOURCE_SET_NAME
import io.github.kotlua.ide.gradle.model.defaultCommonCompilationOrNull
import io.github.kotlua.ide.gradle.model.defaultCommonSourceSetOrNull
import io.github.kotlua.ide.gradle.model.defaultMainCompilationOrNull
import io.github.kotlua.ide.gradle.model.defaultMainSourceSetOrNull
import io.github.kotlua.ide.platform.LuaIdePlatformKind
import io.github.kotlua.ide.platform.LuaLibraryKind
import io.github.kotlua.platform.LuaPlatforms
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.config.KotlinModuleKind
import org.jetbrains.kotlin.config.ResourceKotlinRootType
import org.jetbrains.kotlin.config.SourceKotlinRootType
import org.jetbrains.kotlin.config.TestSourceKotlinRootType
import org.jetbrains.kotlin.idea.base.platforms.StableModuleNameProvider
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.facet.getOrCreateFacet
import org.jetbrains.kotlin.idea.facet.noVersionAutoAdvance
import org.jetbrains.kotlin.idea.gradleJava.migrateNonJvmSourceFolders
import org.jetbrains.kotlin.idea.gradleJava.pathAsUrl
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

// The Kotlin IDE plugin's data services remove KotlinFacet if the Kotlin Gradle plugin is not applied,
// so we ensure that our KotlinFacet is configured afterward
@Order(Integer.MAX_VALUE)
class KotluaFacetDataService : AbstractProjectDataService<KotluaGradleModel, Void>() {

    override fun getTargetDataKey(): Key<KotluaGradleModel> = KotluaProjectResolverExtension.KOTLUA_MODEL_KEY

    override fun postProcess(
        toImport: Collection<DataNode<KotluaGradleModel>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider,
    ) {
        for (node in toImport) {
            val model = node.data
            val parentNode = node.parent ?: continue
            val parentModuleData = parentNode.data as? ModuleData ?: continue

            val sourceSetNodes = ExternalSystemApiUtil.findAll(parentNode, GradleSourceSetData.KEY)
                .associateBy { it.data.moduleName }

            val commonNode = sourceSetNodes[DEFAULT_LUA_COMMON_SOURCE_SET_NAME]
            val luaNode = sourceSetNodes[DEFAULT_LUA_MAIN_SOURCE_SET_NAME]

            val commonIdeModule = commonNode?.let { modelsProvider.findIdeModule(it.data) }
            val luaIdeModule = luaNode?.let { modelsProvider.findIdeModule(it.data) }

            if (commonIdeModule != null) {
                val rootModel = modelsProvider.getModifiableRootModel(commonIdeModule)
                model.defaultCommonSourceSetOrNull?.let {
                    configureSourceFolders(rootModel, it.sourceDirs, it.resourceDirs)
                }
                configureSdk(rootModel)
                migrateNonJvmSourceFolders(rootModel, ExternalSystemApiUtil.toExternalSource(parentModuleData.owner))
                populateNonJvmSourceRootTypes(commonNode, commonIdeModule)

                configureCommonKotlinFacet(commonIdeModule, model, modelsProvider)
            }

            if (luaIdeModule != null) {
                val rootModel = modelsProvider.getModifiableRootModel(luaIdeModule)
                model.defaultMainSourceSetOrNull?.let {
                    configureSourceFolders(rootModel, it.sourceDirs, it.resourceDirs)
                }
                configureSdk(rootModel)
                migrateNonJvmSourceFolders(rootModel, ExternalSystemApiUtil.toExternalSource(parentModuleData.owner))
                populateNonJvmSourceRootTypes(luaNode, luaIdeModule)

                configureLuaKotlinFacet(luaIdeModule, commonIdeModule, model, modelsProvider)

                attachKlibLibraries(rootModel, model, modelsProvider)
            }
        }
    }

    private fun configureCommonKotlinFacet(
        module: Module,
        model: KotluaGradleModel,
        modelsProvider: IdeModifiableModelsProvider
    ): KotlinFacet {
        val id = GradleConstants.SYSTEM_ID.id
        val facet = module.getOrCreateFacet(modelsProvider, false, id)

        with(facet.configuration.settings) {
            useProjectSettings = false
            targetPlatform = CommonPlatforms.defaultCommonPlatform
            val args = LuaIdePlatformKind.createArguments()
            model.defaultCommonCompilationOrNull?.let { compilation ->
                parseCommandLineArguments(compilation.compilerArguments, args)
            }
            model.defaultCommonSourceSetOrNull?.languageSettings?.applyTo(args)
            compilerArguments = args

            isHmppEnabled = true
            kind = KotlinModuleKind.SOURCE_SET_HOLDER
            noVersionAutoAdvance()
            updateMergedArguments()
        }
        return facet
    }

    private fun configureLuaKotlinFacet(
        module: Module,
        commonModule: Module?,
        model: KotluaGradleModel,
        modelsProvider: IdeModifiableModelsProvider,
    ): KotlinFacet {
        val id = GradleConstants.SYSTEM_ID.id
        val facet = module.getOrCreateFacet(modelsProvider, false, id)

        with(facet.configuration.settings) {
            useProjectSettings = false
            targetPlatform = LuaPlatforms.defaultLuaPlatform

            val args = LuaIdePlatformKind.createArguments()
            model.defaultMainCompilationOrNull?.let { compilation ->
                parseCommandLineArguments(compilation.compilerArguments, args)
            }
            model.defaultMainSourceSetOrNull?.languageSettings?.applyTo(args)
            compilerArguments = args

            isHmppEnabled = true
            kind = KotlinModuleKind.SOURCE_SET_HOLDER

            if (commonModule != null) {
                val stableNameProvider = StableModuleNameProvider.getInstance(module.project)
                val commonStableName = stableNameProvider.getStableModuleName(commonModule)

                dependsOnModuleNames = listOf(commonStableName)
                implementedModuleNames = listOf(commonStableName)
            }

            noVersionAutoAdvance()
            updateMergedArguments()
        }

        return facet
    }

    private fun attachKlibLibraries(
        rootModel: ModifiableRootModel,
        model: KotluaGradleModel,
        modelsProvider: IdeModifiableModelsProvider,
    ) {
        val classpath = model.defaultMainCompilationOrNull?.compileClasspath ?: return

        for (klibFile in classpath) {
            val libName = "Kotlua: ${klibFile.nameWithoutExtension}"

            var library = modelsProvider.getLibraryByName(libName)
            if (library == null) {
                library = modelsProvider.createLibrary(libName)
            }

            val libModel = modelsProvider.getModifiableLibraryModel(library) as LibraryEx.ModifiableModelEx

            val vFile = VfsUtil.findFileByIoFile(klibFile, true)
                ?: com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(klibFile)

            if (vFile != null) {
                val jarRoot = if (!vFile.isDirectory) {
                    JarFileSystem.getInstance().getJarRootForLocalFile(vFile) ?: vFile
                } else {
                    vFile
                }

                val currentUrls = libModel.getUrls(OrderRootType.CLASSES)
                if (jarRoot.url !in currentUrls) {
                    currentUrls.forEach { libModel.removeRoot(it, OrderRootType.CLASSES) }
                    libModel.addRoot(jarRoot, OrderRootType.CLASSES)
                }
            }

            libModel.kind = LuaLibraryKind

            if (rootModel.findLibraryOrderEntry(library) == null) {
                rootModel.addLibraryEntry(library)
            }
        }
    }

    private fun populateNonJvmSourceRootTypes(sourceSetNode: DataNode<GradleSourceSetData>, module: Module) {
        val sourceFolderManager = SourceFolderManager.getInstance(module.project)
        val contentRootDataNodes = ExternalSystemApiUtil.findAll(sourceSetNode, ProjectKeys.CONTENT_ROOT)
        val contentRootDataList = contentRootDataNodes.mapNotNull { it.data }
        if (contentRootDataList.isEmpty()) return

        val externalToKotlinSourceTypes = mapOf(
            ExternalSystemSourceType.SOURCE to SourceKotlinRootType,
            ExternalSystemSourceType.SOURCE_GENERATED to SourceKotlinRootType,
            ExternalSystemSourceType.TEST to TestSourceKotlinRootType,
            ExternalSystemSourceType.TEST_GENERATED to TestSourceKotlinRootType,
        )

        externalToKotlinSourceTypes.forEach { (externalType, kotlinType) ->
            val sourcesRoots = contentRootDataList.flatMap { it.getPaths(externalType) }
            sourcesRoots.forEach {
                if (!FileUtil.exists(it.path)) {
                    sourceFolderManager.addSourceFolder(module, it.pathAsUrl, kotlinType)
                }
            }
        }
    }
}

private fun KotlinLanguageSettings.applyTo(args: CommonCompilerArguments) {
    args.languageVersion = languageVersion
    args.apiVersion = apiVersion
    args.optIn = optInAnnotationsInUse.toTypedArray()
    args.pluginOptions = compilerPluginArguments
    args.pluginClasspaths = compilerPluginClasspath.map(File::getPath).toTypedArray()
    args.freeArgs = freeCompilerArgs.toMutableList()
    args.progressiveMode = isProgressiveMode
}

// KotlinSDK is not needed in our case, kotlin-stdlib-lua is used instead (at least for now)
private fun configureSdk(rootModel: ModifiableRootModel) {
    rootModel.sdk = null
}

private fun configureSourceFolders(
    rootModel: ModifiableRootModel,
    sourceDirs: Set<File>,
    resourceDirs: Set<File>,
) {
    for (sourceDir in sourceDirs) {
        val url = VfsUtil.fileToUrl(sourceDir)
        val contentEntry = rootModel.contentEntries.firstOrNull { entry ->
            VfsUtilCore.isEqualOrAncestor(entry.url, url)
        } ?: rootModel.addContentEntry(url)

        val existingFolder = contentEntry.sourceFolders.firstOrNull { it.url == url }
        if (existingFolder == null) {
            contentEntry.addSourceFolder(url, SourceKotlinRootType)
        } else if (existingFolder.rootType != SourceKotlinRootType) {
            contentEntry.removeSourceFolder(existingFolder)
            contentEntry.addSourceFolder(url, SourceKotlinRootType)
        }
    }

    for (resourceDir in resourceDirs) {
        val url = VfsUtil.fileToUrl(resourceDir)
        val contentEntry = rootModel.contentEntries.firstOrNull { entry ->
            VfsUtilCore.isEqualOrAncestor(entry.url, url)
        } ?: rootModel.addContentEntry(url)

        val existingFolder = contentEntry.sourceFolders.firstOrNull { it.url == url }
        if (existingFolder == null) {
            contentEntry.addSourceFolder(url, ResourceKotlinRootType)
        }
    }
}

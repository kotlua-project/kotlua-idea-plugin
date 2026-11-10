package io.github.kotlua.ide.patches

import com.google.gson.Gson
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription
import com.intellij.openapi.vfs.VirtualFile
import io.github.kotlua.cli.compiler.K2LuaCompilerArguments
import io.github.kotlua.ide.fir.LuaSessionComponentDelegate
import io.github.kotlua.ide.fir.LuaSessionConfigurationDelegate
import io.github.kotlua.ide.platform.LuaIdePlatformKind
import io.github.kotlua.ide.platform.LuaLibraryKind
import io.github.kotlua.platform.LuaPlatform
import io.github.kotlua.platform.LuaPlatforms
import io.github.kotlua.platform.isLua
import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers.named
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModulePlatformKind
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.idea.base.projectStructure.openapiModule
import org.jetbrains.kotlin.idea.compiler.configuration.IdeKotlinVersion
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.library.BaseKotlinLibrary
import org.jetbrains.kotlin.library.KLIB_PROPERTY_BUILTINS_PLATFORM
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.TargetPlatform
import java.lang.reflect.Modifier
import java.util.*

@OptIn(KaImplementationDetail::class, KaPlatformInterface::class)
@Suppress("UnstableApiUsage")
object LuaPlatformHooks {
    private val LOG = logger<LuaPlatformHooks>()
    private var luaComponentRegistrationInstance: Any? = null
    private var luaSessionConfigurationClass: Class<*>? = null

    // TODO: use kotlua-fir artifact
    const val LUA_PLATFORM_NAME: String = "LUA"
    const val KOTLIN_LUA_STDLIB_NAME: String = "kotlin-stdlib-lua"

    @JvmStatic
    @Synchronized
    fun getOrCreateLuaSessionComponentRegistration(classLoader: ClassLoader?): Any {
        if (luaComponentRegistrationInstance != null) {
            return luaComponentRegistrationInstance!!
        }

        val targetClassLoader = getKotlinClassLoader(classLoader)
        val interfaceClass = Class.forName(
            "org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.components.LLPlatformSessionComponentRegistration",
            true,
            targetClassLoader,
        )

        val dynamicType = ByteBuddy()
            .subclass(interfaceClass)
            .name("org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.components.LLLuaSessionComponentRegistration")
            .method(named("registerComponents"))
            .intercept(MethodDelegation.to(LuaSessionComponentDelegate::class.java))
            .method(named("getDefaultImportsProvider"))
            .intercept(MethodDelegation.to(LuaSessionComponentDelegate::class.java))
            .make()
            .load(targetClassLoader, ClassLoadingStrategy.Default.INJECTION)
            .loaded

        LOG.debug("Created LLLuaSessionComponentRegistration using $targetClassLoader")

        luaComponentRegistrationInstance = dynamicType.getDeclaredConstructor().newInstance()
        return luaComponentRegistrationInstance!!
    }

    @JvmStatic
    @Synchronized
    fun getOrCreateLuaSessionConfigurationClass(classLoader: ClassLoader?): Class<*> {
        if (luaSessionConfigurationClass != null) {
            return luaSessionConfigurationClass!!
        }

        val targetClassLoader = getKotlinClassLoader(classLoader)
        val interfaceClass = Class.forName(
            "org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.configuration.LLPlatformSessionConfiguration",
            true,
            targetClassLoader,
        )

        luaSessionConfigurationClass = ByteBuddy()
            .subclass(interfaceClass)
            .name("org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.configuration.LLLuaSessionConfiguration")
            .defineConstructor(Modifier.PUBLIC)
            .withParameters(Project::class.java)
            .intercept(MethodCall.invoke(Any::class.java.getDeclaredConstructor()))
            .method(named("createBinaryLibrarySymbolProviders"))
            .intercept(MethodDelegation.to(LuaSessionConfigurationDelegate::class.java))
            .make()
            .load(targetClassLoader, ClassLoadingStrategy.Default.INJECTION)
            .loaded

        LOG.debug("Created LLLuaSessionConfiguration using $targetClassLoader")

        return luaSessionConfigurationClass!!
    }

    @JvmStatic
    fun shouldSuppressKotlinNotConfigured(project: Any?, file: Any?): Boolean {
        val proj = project as? Project ?: return false
        val vFile = file as? VirtualFile ?: return false

        val module = ModuleUtilCore.findModuleForFile(vFile, proj) ?: return false
        val facet = KotlinFacet.get(module) ?: return false
        val settings = facet.configuration.settings

        val platform = settings.targetPlatform ?: return false

        return platform.isLua() ||
                platform.componentPlatforms.any { it is LuaPlatform } ||
                settings.compilerArguments is K2LuaCompilerArguments
    }

    @JvmStatic
    fun isLuaStdlib(library: Any?): Boolean {
        val baseLib = library as? BaseKotlinLibrary ?: return false
        val properties = baseLib.manifestProperties

        val builtinsPlatform = properties.getProperty(KLIB_PROPERTY_BUILTINS_PLATFORM)
        val uniqueName = properties.getProperty(KLIB_PROPERTY_UNIQUE_NAME)

        val isLua = builtinsPlatform.equals(LUA_PLATFORM_NAME, ignoreCase = true)
        val isStdlibName = uniqueName == KOTLIN_LUA_STDLIB_NAME || uniqueName == "stdlib" || uniqueName == "kotlin"
        return isLua && isStdlibName
    }

    @JvmStatic
    fun patchCommonPlatform(original: TargetPlatform?): TargetPlatform {
        if (original == null) return LuaPlatforms.defaultLuaPlatform
        val luaComponents = LuaPlatforms.defaultLuaPlatform.componentPlatforms

        if (original.componentPlatforms.containsAll(luaComponents)) {
            return original
        }

        return TargetPlatform(original.componentPlatforms + luaComponents)
    }

    @JvmStatic
    fun patchAllSimplePlatforms(original: List<TargetPlatform>): List<TargetPlatform> {
        if (original.any { it.isLua() }) return original
        return original.plusElement(LuaPlatforms.defaultLuaPlatform)
    }

    @JvmStatic
    fun logModuleDependencies(module: Any?) {
        val sourceModule = module as? KaSourceModule ?: return
        if (!sourceModule.targetPlatform.isLua()) return

        val intellijModule = sourceModule.openapiModule
        val rootManager = ModuleRootManager.getInstance(intellijModule)

        LOG.debug {
            """
                >>> [DEBUG Dependencies] Checking KaSourceModule: ${sourceModule.name}
                    Module TargetPlatform: ${sourceModule.targetPlatform}
                    Physical OrderEntries in IDEA Module (${rootManager.orderEntries.size}):
            """.trimIndent()
        }
        rootManager.orderEntries.forEach { entry ->
            when (entry) {
                is LibraryOrderEntry -> {
                    val lib = entry.library
                    LOG.debug("* Library: '${entry.libraryName}' (exists: ${lib != null})")
                }

                else -> LOG.debug("* Entry: ${entry.presentableName} (${entry.javaClass.simpleName})")
            }
        }

        val directDeps = sourceModule.directRegularDependencies
        LOG.debug("K2 Analysis API Dependencies (${directDeps.size}):")
        directDeps.forEach { dep ->
            LOG.debug("-> $dep (class: ${dep.javaClass.simpleName}, platform: ${dep.targetPlatform})")
        }
    }

    @JvmStatic
    fun isLuaSession(session: Any?): Boolean {
        val firSession = session as? LLFirSession ?: return false
        return isLuaModulePlatformKind(firSession.ktModule.targetPlatform)
    }

    @JvmStatic
    fun isLuaPersistentLibraryKind(kind: Any?): Boolean {
        if (kind == null) return false
        if (kind is LuaLibraryKind) return true

        val kindId = (kind as? PersistentLibraryKind<*>)?.kindId
        return kindId != null && kindId.contains("lua", ignoreCase = true)
    }

    @JvmStatic
    fun onGetPlatformKindCalled(session: Any?, currentKind: Any?) {
        val firSession = session as? LLFirSession ?: return
        val stack = Throwable().stackTrace
        val callers = stack.drop(2).take(15)

        LOG.debug {
            """
                [DEBUG FIR] getPlatformKind() called for Lua session: $firSession
                Current value: $currentKind
                Caller hierarchy:
            """.trimMargin()
        }
        callers.forEach { frame ->
            LOG.debug("at $frame")
        }
    }

    private fun getKotlinClassLoader(preferred: ClassLoader?): ClassLoader {
        if (preferred != null) return preferred
        return PluginManagerCore.getPlugin(PluginId.getId("org.jetbrains.kotlin"))?.pluginClassLoader
            ?: LuaPlatformHooks::class.java.classLoader
    }

    @JvmStatic
    fun createLuaPlatformSessionConfiguration(project: Any, classLoader: ClassLoader? = null): Any {
        val projectInstance = project as Project
        val clazz = getOrCreateLuaSessionConfigurationClass(classLoader)
        return clazz.getDeclaredConstructor(Project::class.java).newInstance(projectInstance)
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun patchComponentRegistration(originalList: List<Any>, classLoader: ClassLoader? = null): List<Any> {
        val luaRegistration = getOrCreateLuaSessionComponentRegistration(classLoader)
        return originalList + luaRegistration
    }

    @JvmStatic
    fun getLuaTargetPlatform(module: Any?): TargetPlatform {
        return LuaPlatforms.defaultLuaPlatform
    }

    @JvmStatic
    fun isKlibLibraryRootForLua(rootFile: VirtualFile, targetPlatform: TargetPlatform): Boolean {
        if (checkLuaKlibComponent(rootFile)) {
            return true
        }

        try {
            val children = rootFile.children ?: return false
            for (child in children) {
                if (checkLuaKlibComponent(child)) {
                    return true
                }
            }
        } catch (e: Throwable) {
            return false
        }

        return false
    }

    private fun checkLuaKlibComponent(componentFile: VirtualFile): Boolean {
        val properties = IdeKotlinVersion.klibManifestProperties(componentFile)
            ?: loadManifestPropertiesDirectly(componentFile)
            ?: return false

        if (!properties.containsKey(KLIB_PROPERTY_UNIQUE_NAME)) return false

        val builtinsPlatform = properties.getProperty(KLIB_PROPERTY_BUILTINS_PLATFORM)
        if (builtinsPlatform != null && builtinsPlatform.equals("LUA", ignoreCase = true)) {
            LOG.debug("Accepted Lua KLib component: ${componentFile.path} (platform=LUA)")
            return true
        }

        val uniqueName = properties.getProperty(KLIB_PROPERTY_UNIQUE_NAME)
        if (uniqueName != null && uniqueName.contains("lua", ignoreCase = true)) {
            LOG.debug("Accepted Lua KLib component by uniqueName: $uniqueName")
            return true
        }

        return false
    }

    private fun loadManifestPropertiesDirectly(file: VirtualFile): Properties? {
        val manifestFile = file.findChild("default")?.findChild("manifest")
            ?: file.findChild("manifest")
            ?: return null
        return try {
            Properties().apply {
                manifestFile.inputStream.use { load(it) }
            }
        } catch (e: Throwable) {
            null
        }
    }

    @JvmStatic
    fun isLuaModulePlatformKind(platform: TargetPlatform): Boolean {
        return platform.all { it is LuaPlatform }
    }

    @JvmStatic
    fun isLuaTargetPlatform(platform: TargetPlatform?): Boolean {
        return platform.isLua()
    }

    @JvmStatic
    fun isLuaPlatformKind(platformKind: Any?): Boolean {
        return platformKind is LuaIdePlatformKind
    }

    @JvmStatic
    fun getCompilerArguments(structureInstance: Any, platformKind: Any?): CommonCompilerArguments? {
        return null
    }

    @JvmStatic
    fun getLuaModulePlatformKind(targetPlatform: Any?): KaModulePlatformKind {
        val stack = Throwable().stackTrace
        val callers = stack.drop(2).take(10)

        LOG.debug {
            """
                [DEBUG PlatformKind] toModulePlatformKind() called for Lua: $targetPlatform
                Returning: KaModulePlatformKind.METADATA
                Caller hierarchy:
            """.trimMargin()
        }
        callers.forEach { frame ->
            LOG.debug("at $frame")
        }

        return KaModulePlatformKind.METADATA
    }

    @JvmStatic
    fun getLibraryKind(platformKind: Any?): PersistentLibraryKind<*> {
        return LuaLibraryKind
    }

    @JvmStatic
    fun isLuaLibraryFile(file: Any?): Boolean {
        return file is VirtualFile && file.extension == "klib" && file.name.contains("lua")
    }

    @JvmStatic
    fun getLibraryPlatformKind(file: Any?): IdePlatformKind {
        LOG.debug("Detected Lua library platform kind for file: ${(file as? VirtualFile)?.name}")
        return LuaIdePlatformKind
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun createLuaArguments(platform: Any?, init: Any?): CommonCompilerArguments {
        val arguments: CommonCompilerArguments = K2LuaCompilerArguments()

        if (init is Function1<*, *>) {
            (init as Function1<CommonCompilerArguments, Unit>).invoke(arguments)
        }

        return arguments
    }

    @JvmStatic
    fun getLibraryVersionProvider(structureInstance: Any, platformKind: Any?): (Library) -> IdeKotlinVersion? {
        return { library: Library ->
            try {
                val files = library.getFiles(OrderRootType.CLASSES)
                val klibFile = files.firstOrNull { it.extension == "klib" }

                if (klibFile != null) {
                    IdeKotlinVersion.fromKLibManifest(klibFile)
                } else {
                    null
                }
            } catch (e: Throwable) {
                null
            }
        }
    }

    @JvmStatic
    fun getLibraryDescription(project: Project): CustomLibraryDescription? {
        return null
    }

    @JvmStatic
    fun isLuaCompilerArguments(args: Any?): Boolean {
        return args is K2LuaCompilerArguments
    }

    @JvmStatic
    fun isLuaSerializedString(str: String?): Boolean {
        return str != null && str.startsWith("L")
    }

    @JvmStatic
    fun serializeLuaArguments(args: Any?, gson: Any?): String {
        val gsonInstance = gson as Gson
        return "L" + gsonInstance.toJson(args)
    }

    @JvmStatic
    fun deserializeLuaArguments(str: String?, gson: Any?): CommonCompilerArguments? {
        if (str == null || str.length <= 1) return null
        val gsonInstance = gson as Gson
        return gsonInstance.fromJson(str.substring(1), K2LuaCompilerArguments::class.java)
    }

    @JvmStatic
    fun patchDeserializedPlatformIfNeeded(platformString: String, originalResult: TargetPlatform?): TargetPlatform {
        return if (originalResult == null) {
            LuaPlatforms.defaultLuaPlatform
        } else {
            TargetPlatform(originalResult.componentPlatforms + LuaPlatforms.defaultLuaPlatform.componentPlatforms)
        }
    }

    @JvmStatic
    fun beforeInitializeIfNeeded(
        facetSettings: Any?,
        module: Any?,
        rootModel: Any?,
        platform: Any?,
        compilerVersion: Any?,
    ) {
        // breakpoint
    }

    @JvmStatic
    fun afterInitializeIfNeeded(
        facetSettings: Any?,
        module: Any?,
        rootModel: Any?,
        platform: Any?,
        compilerVersion: Any?,
        error: Throwable?,
    ) {
        // breakpoint
        error?.printStackTrace()
    }

    @JvmStatic
    fun beforeSerializeFacetSettings(settings: Any?, element: Any?) {
        // breakpoint
    }

    @JvmStatic
    fun afterSerializeFacetSettings(settings: Any?, element: Any?, thrown: Throwable?) {
        // breakpoint
        if (thrown != null) {
            LOG.debug("ERROR during serializeFacetSettings: ${thrown.message}")
        }
    }

    @JvmStatic
    fun beforeDeserializeFacetSettings(element: Any?) {
        // breakpoint
    }

    @JvmStatic
    fun afterDeserializeFacetSettings(element: Any?, resultSettings: Any?, thrown: Throwable?) {
        // breakpoint
        if (thrown != null) {
            LOG.debug("ERROR during deserializeFacetSettings: ${thrown.message}")
        }
    }
}

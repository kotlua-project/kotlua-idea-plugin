package io.github.kotlua.ide

import com.intellij.ide.ApplicationInitializedListener
import io.github.kotlua.ide.patches.patchCommonPlatforms
import io.github.kotlua.ide.patches.patchCompilerArgumentsSerializer
import io.github.kotlua.ide.patches.patchFacetSerialization
import io.github.kotlua.ide.patches.patchKLibUtils
import io.github.kotlua.ide.patches.patchKaLibraryModuleImpl
import io.github.kotlua.ide.patches.patchKaModulePlatformKind
import io.github.kotlua.ide.patches.patchKotlinFacetUtils
import io.github.kotlua.ide.patches.patchKotlinLibrary
import io.github.kotlua.ide.patches.patchKotlinLibraryKind
import io.github.kotlua.ide.patches.patchKotlinSetupEnvironmentNotification
import io.github.kotlua.ide.patches.patchLLFirFactories
import io.github.kotlua.ide.patches.patchLLFirSession
import io.github.kotlua.ide.patches.patchPlatformStructure
import io.github.kotlua.ide.patches.patchPluginClassLoader
import io.github.kotlua.ide.patches.patchProjectConfigurationUtils
import net.bytebuddy.agent.ByteBuddyAgent

@Suppress("UnstableApiUsage")
class PatcherListener : ApplicationInitializedListener {

    override suspend fun execute() {
        try {
            val instrumentation = ByteBuddyAgent.install()
            with(instrumentation) {
                patchPluginClassLoader()
                patchCommonPlatforms()
                patchLLFirSession()
                patchCompilerArgumentsSerializer()
                patchFacetSerialization()
                patchProjectConfigurationUtils()
                patchKotlinFacetUtils()
                patchPlatformStructure()
                patchKaLibraryModuleImpl()
                patchKaModulePlatformKind()
                patchLLFirFactories()
                patchKotlinLibrary()
                patchKotlinLibraryKind()
                patchKLibUtils()
                patchKotlinSetupEnvironmentNotification()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

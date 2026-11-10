@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package io.github.kotlua.ide.fir

import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.NullDeserializedContainerSourceProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLKotlinStubBasedLibrarySymbolProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.session.KlibBasedSymbolProvider
import org.jetbrains.kotlin.library.KlibConstants.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.uniqueName
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

@OptIn(KaImplementationDetail::class, KaPlatformInterface::class)
object LuaSessionConfigurationDelegate {

    private val LOG = logger<LuaSessionConfigurationDelegate>()

    @JvmStatic
    fun createBinaryLibrarySymbolProviders(
        session: LLFirSession,
        scope: GlobalSearchScope
    ): List<FirSymbolProvider> {
        val project = session.project
        val platformSettings = KotlinPlatformSettings.getInstance(project)

        LOG.debug("LuaSessionConfiguration: creating symbol providers (origin=${platformSettings.deserializedDeclarationsOrigin})")

        return when (platformSettings.deserializedDeclarationsOrigin) {
            KotlinDeserializedDeclarationsOrigin.BINARIES -> {
                val moduleData = session.moduleData
                val moduleDataProvider = SingleModuleDataProvider(moduleData)
                val klibs = resolveLibraryKlibs(session)

                LOG.debug("LuaSessionConfiguration: resolved ${klibs.size} KLIBs: ${klibs.joinToString(", ") { it.uniqueName }}")

                listOf(
                    KlibBasedSymbolProvider(
                        session,
                        moduleDataProvider,
                        session.kotlinScopeProvider,
                        klibs
                    )
                )
            }
            KotlinDeserializedDeclarationsOrigin.STUBS -> {
                listOf(
                    LLKotlinStubBasedLibrarySymbolProvider(
                        session,
                        NullDeserializedContainerSourceProvider,
                        scope
                    )
                )
            }
        }
    }

    private fun resolveLibraryKlibs(session: LLFirSession): List<KotlinLibrary> {
        val ktLibraryModule = session.ktModule as? KaLibraryModule ?: return emptyList()

        return ktLibraryModule.binaryRoots
            .filter { it.isDirectory() || it.extension == KLIB_FILE_EXTENSION }
            .mapNotNull { path -> path.tryResolveAsKLib() }
    }

    private fun Path.tryResolveAsKLib(): KotlinLibrary? {
        return try {
            KlibLoader { libraryPaths(absolutePathString()) }
                .load()
                .librariesStdlibFirst
                .singleOrNull()
        } catch (e: Exception) {
            LOG.warn("Cannot resolve Lua KLib: $this", e)
            null
        }
    }
}
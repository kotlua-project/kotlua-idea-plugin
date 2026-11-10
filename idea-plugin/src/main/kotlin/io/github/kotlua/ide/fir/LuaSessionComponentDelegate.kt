@file:Suppress("UnstableApiUsage")

package io.github.kotlua.ide.fir

import com.intellij.openapi.diagnostic.logger
import io.github.kotlua.resolve.LuaDefaultImportsProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.scopes.FirDefaultImportsProviderHolder
import org.jetbrains.kotlin.fir.scopes.impl.FirEnumEntriesSupport
import org.jetbrains.kotlin.resolve.DefaultImportsProvider

// TODO: use kotlua-fir artifact
@OptIn(SessionConfiguration::class)
object LuaSessionComponentDelegate {

    private val LOG = logger<LuaSessionComponentDelegate>()

    @JvmStatic
    fun registerComponents(session: Any, platformSpecificSymbolProviders: List<*>) {
        if (session is FirSession) {
            LOG.debug("Registering Lua FIR session components for: $session")
            session.register(FirEnumEntriesSupport(session))
            session.register(FirDefaultImportsProviderHolder.of(LuaDefaultImportsProvider))
        }
    }

    @JvmStatic
    fun getDefaultImportsProvider(): DefaultImportsProvider {
        return LuaDefaultImportsProvider
    }
}

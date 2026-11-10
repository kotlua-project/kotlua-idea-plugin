package io.github.kotlua.ide.platform

import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.PersistentLibraryKind

object LuaLibraryKind : PersistentLibraryKind<DummyLibraryProperties>("kotlin.lua") {
    override fun createDefaultProperties(): DummyLibraryProperties {
        return DummyLibraryProperties.INSTANCE
    }
}

package io.github.kotlua.ide.platform

import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import org.jetbrains.kotlin.idea.base.codeInsight.tooling.AbstractGenericTestIconProvider
import org.jetbrains.kotlin.idea.base.codeInsight.tooling.IdePlatformKindTooling
import org.jetbrains.kotlin.idea.projectModel.KotlinPlatform
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import javax.swing.Icon

class LuaIdePlatformKindTooling : IdePlatformKindTooling() {

    override val kind: IdePlatformKind
        get() = LuaIdePlatformKind

    override val mavenLibraryIds: List<String> = listOf(
        "kotlin-stdlib-lua"
    )

    override val gradlePluginId: String = "io.github.kotlua"

    override val gradlePlatformIds: List<KotlinPlatform> = emptyList()

    override val libraryKind: PersistentLibraryKind<*>? = null

    override val testIconProvider: AbstractGenericTestIconProvider = object : AbstractGenericTestIconProvider() {
        override fun isKotlinTestDeclaration(declaration: KtNamedDeclaration): Boolean {
            return false
        }
    }

    override fun getTestIcon(declaration: KtNamedDeclaration, allowSlowOperations: Boolean): Icon? = null

    override fun acceptsAsEntryPoint(function: KtFunction): Boolean {
        if (function.isLocal) return false

        val name = function.name ?: return false
        if (name != "main") return false

        val valueParameters = function.valueParameters
        return valueParameters.isEmpty() || valueParameters.size == 1
    }
}
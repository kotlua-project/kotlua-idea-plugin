@file:Suppress("DEPRECATION_ERROR", "DeprecatedCallableAddReplaceWith")
package io.github.kotlua.ide.platform

import io.github.kotlua.cli.compiler.K2LuaCompilerArguments
import io.github.kotlua.platform.LuaPlatforms
import io.github.kotlua.platform.isLua
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.platform.IdePlatform
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.TargetPlatformVersion

object LuaIdePlatformKind : IdePlatformKind() {
    override val defaultPlatform = LuaPlatforms.defaultLuaPlatform
    override val argumentsClass: Class<out CommonCompilerArguments>
        get() = K2LuaCompilerArguments::class.java
    override val name = "LUA"

    override fun supportsTargetPlatform(platform: TargetPlatform): Boolean {
        return platform.isLua()
    }

    @Deprecated(
        "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
        level = DeprecationLevel.ERROR
    )
    override fun getDefaultPlatform() = Platform

    override fun platformByCompilerArguments(arguments: CommonCompilerArguments): TargetPlatform? {
        if (arguments !is K2LuaCompilerArguments) return null

        return defaultPlatform
    }

    override fun createArguments(): CommonCompilerArguments {
        return K2LuaCompilerArguments()
    }

    data object Platform : IdePlatform<LuaIdePlatformKind, K2LuaCompilerArguments>() {
        override val version: TargetPlatformVersion = TargetPlatformVersion.NoVersion
        override val kind: LuaIdePlatformKind get() = LuaIdePlatformKind

        override fun createArguments(init: K2LuaCompilerArguments.() -> Unit) = K2LuaCompilerArguments()
            .apply(init)
    }
}
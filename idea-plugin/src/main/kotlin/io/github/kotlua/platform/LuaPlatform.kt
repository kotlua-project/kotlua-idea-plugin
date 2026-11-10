/*
 * Copyright 2026 The kotlua Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.kotlua.platform

import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.TargetPlatformVersion
import org.jetbrains.kotlin.platform.toTargetPlatform

enum class LuaTarget(
    val versionString: String,
) : TargetPlatformVersion {
    LUA_5_1("5.1"),
    LUA_5_2("5.2"),
    LUA_5_3("5.3"),
    LUA_5_4("5.4"),
    LUAJIT("LuaJIT"),
    ;

    override val description: String
        get() = versionString

    companion object {
        val DEFAULT = LUA_5_4

        fun fromStringOrNull(version: String?): LuaTarget? =
            when (version?.lowercase()) {
                "5.1" -> LUA_5_1
                "5.2" -> LUA_5_2
                "5.3" -> LUA_5_3
                "5.4" -> LUA_5_4
                "luajit", "jit" -> LUAJIT
                else -> null
            }
    }
}

abstract class LuaPlatform : SimplePlatform("Lua") {
    override val oldFashionedDescription: String
        get() = "Lua "
}

class LuaPlatformWithTarget(
    val targetVersion: LuaTarget,
) : LuaPlatform() {
    override val targetPlatformVersion: TargetPlatformVersion
        get() = targetVersion

    override val oldFashionedDescription: String
        get() = "Lua " + targetVersion.description

    override fun toString(): String = "$platformName ($targetVersion)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LuaPlatformWithTarget) return false
        return targetVersion == other.targetVersion
    }

    override fun hashCode(): Int = targetVersion.hashCode()
}

object LuaPlatforms {
    private val luaTargetToPlatform: Map<LuaTarget, TargetPlatform> =
        LuaTarget.entries.associateWith { LuaPlatformWithTarget(it).toTargetPlatform() }

    val defaultLuaPlatform: TargetPlatform = luaTargetToPlatform[LuaTarget.DEFAULT]!!

    val lua51: TargetPlatform = luaTargetToPlatform[LuaTarget.LUA_5_1]!!
    val lua54: TargetPlatform = luaTargetToPlatform[LuaTarget.LUA_5_4]!!
    val luaJit: TargetPlatform = luaTargetToPlatform[LuaTarget.LUAJIT]!!

    val allLuaPlatforms: List<TargetPlatform> = luaTargetToPlatform.values.toList()

    fun luaPlatformByTarget(target: LuaTarget): TargetPlatform = luaTargetToPlatform[target] ?: defaultLuaPlatform
}

fun TargetPlatform?.isLua(): Boolean = this?.singleOrNull() is LuaPlatform

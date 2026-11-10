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
package io.github.kotlua.cli.compiler

import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArgumentsConfigurator
import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments

class K2LuaCompilerArguments : CommonKlibBasedCompilerArguments() {
    @Argument(
        value = "-Xinclude",
        valueDescription = "<path>",
        description = "Path to the main KLib to be compiled into executable Lua.",
    )
    var includes: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-nopack",
        description = "Don't pack the library into a klib file.",
    )
    var nopack: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-libraries",
        valueDescription = "<path>",
        description = "Paths to dependent KLibs separated by path separator.",
    )
    var libraries: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-ir-output-dir",
        valueDescription = "<directory>",
        description = "Destination directory for generated KLib or Lua files.",
    )
    var outputDir: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-ir-output-name",
        valueDescription = "<name>",
        description = "Base name of generated files (e.g. 'app.klib' or 'app.lua').",
    )
    var moduleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-main",
        valueDescription = "{call|noCall}",
        description = "Generate call to main() function upon execution.",
    )
    var main: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xlua-version",
        valueDescription = "{5.1|5.2|5.3|5.4|luajit}",
        description = "Target Lua language/runtime version.",
    )
    var luaVersion: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @field:Transient
    override val configurator: CommonCompilerArgumentsConfigurator = K2LuaCompilerArgumentsConfigurator()
}

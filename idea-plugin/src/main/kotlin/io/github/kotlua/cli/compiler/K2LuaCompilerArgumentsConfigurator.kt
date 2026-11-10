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

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArgumentsConfigurator

class K2LuaCompilerArgumentsConfigurator : CommonKlibBasedCompilerArgumentsConfigurator() {
    override fun isSecondStage(arguments: CommonCompilerArguments): Boolean =
        with(arguments) {
            require(this is K2LuaCompilerArguments)

            return includes != null
        }
}

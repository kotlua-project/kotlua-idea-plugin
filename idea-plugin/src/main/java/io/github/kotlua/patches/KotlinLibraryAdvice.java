package io.github.kotlua.patches;

import io.github.kotlua.ide.patches.LuaPlatformHooks;
import net.bytebuddy.asm.Advice;

public class KotlinLibraryAdvice {

    public static class IsAnyPlatformStdlibAdvice {

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.Argument(0) Object library,
                @Advice.Return(readOnly = false) boolean result
        ) {
            if (!result) {
                result = LuaPlatformHooks.isLuaStdlib(library);
            }
        }
    }
}

package io.github.kotlua.patches;

import io.github.kotlua.ide.patches.LuaPlatformHooks;
import net.bytebuddy.asm.Advice;
import org.jetbrains.kotlin.platform.TargetPlatform;

public class KotlinLibraryKindAdvice {

    public static class GetPlatformAdvice {

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean onEnter(
                @Advice.Argument(0) Object libraryKind
        ) {
            return LuaPlatformHooks.isLuaPersistentLibraryKind(libraryKind);
        }

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.Argument(0) Object libraryKind,
                @Advice.Enter boolean skipped,
                @Advice.Return(readOnly = false) TargetPlatform result
        ) {
            if (skipped) {
                result = LuaPlatformHooks.getLuaTargetPlatform(libraryKind);
            }
        }
    }
}

package io.github.kotlua.patches;

import com.intellij.openapi.vfs.VirtualFile;
import io.github.kotlua.ide.patches.LuaPlatformHooks;
import net.bytebuddy.asm.Advice;
import org.jetbrains.kotlin.platform.TargetPlatform;

public class KLibUtilsAdvice {

    public static class IsKlibLibraryRootForPlatformAdvice {

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean onEnter(
                @Advice.Argument(1) TargetPlatform targetPlatform
        ) {
            return LuaPlatformHooks.isLuaTargetPlatform(targetPlatform);
        }

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.Argument(0) VirtualFile rootFile,
                @Advice.Argument(1) TargetPlatform targetPlatform,
                @Advice.Enter boolean skipped,
                @Advice.Return(readOnly = false) boolean result
        ) {
            if (skipped) {
                result = LuaPlatformHooks.isKlibLibraryRootForLua(rootFile, targetPlatform);
            }
        }
    }
}
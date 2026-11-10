package io.github.kotlua.patches;

import io.github.kotlua.ide.patches.LuaPlatformHooks;
import net.bytebuddy.asm.Advice;
import org.jetbrains.kotlin.platform.TargetPlatform;
import java.util.List;

public class CommonPlatformsAdvice {

    public static class GetDefaultCommonPlatformAdvice {
        @Advice.OnMethodExit
        public static void onExit(
                @Advice.Return(readOnly = false) TargetPlatform result
        ) {
            result = LuaPlatformHooks.patchCommonPlatform(result);
        }
    }

    public static class GetAllSimplePlatformsAdvice {
        @Advice.OnMethodExit
        public static void onExit(
                @Advice.Return(readOnly = false) List<TargetPlatform> result
        ) {
            result = LuaPlatformHooks.patchAllSimplePlatforms(result);
        }
    }
}
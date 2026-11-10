package io.github.kotlua.patches;

import io.github.kotlua.ide.patches.LuaPlatformHooks;
import net.bytebuddy.asm.Advice;
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModulePlatformKind;
import org.jetbrains.kotlin.platform.TargetPlatform;

public class KaModulePlatformKindAdvice {

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean onEnter(
            @Advice.Argument(0) TargetPlatform targetPlatform
    ) {
        return LuaPlatformHooks.isLuaModulePlatformKind(targetPlatform);
    }

    @Advice.OnMethodExit
    public static void onExit(
            @Advice.Argument(0) TargetPlatform targetPlatform,
            @Advice.Enter boolean skipped,
            @Advice.Return(readOnly = false) KaModulePlatformKind result
    ) {
        if (skipped) {
            result = LuaPlatformHooks.getLuaModulePlatformKind(targetPlatform);
        }
    }
}

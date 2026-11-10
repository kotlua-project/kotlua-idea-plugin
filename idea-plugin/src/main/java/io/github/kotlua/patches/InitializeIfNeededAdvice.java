package io.github.kotlua.patches;

import io.github.kotlua.ide.patches.LuaPlatformHooks;
import net.bytebuddy.asm.Advice;

public class InitializeIfNeededAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(
            @Advice.Argument(0) Object facetSettings,
            @Advice.Argument(1) Object module,
            @Advice.Argument(2) Object rootModel,
            @Advice.Argument(3) Object platform,
            @Advice.Argument(4) Object compilerVersion
    ) {
        LuaPlatformHooks.beforeInitializeIfNeeded(facetSettings, module, rootModel, platform, compilerVersion);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
            @Advice.Argument(0) Object facetSettings,
            @Advice.Argument(1) Object module,
            @Advice.Argument(2) Object rootModel,
            @Advice.Argument(3) Object platform,
            @Advice.Argument(4) Object compilerVersion,
            @Advice.Thrown(readOnly = false) Throwable error
    ) {
        LuaPlatformHooks.afterInitializeIfNeeded(facetSettings, module, rootModel, platform, compilerVersion, error);

        if (error != null) {
            error = null;
        }
    }

}

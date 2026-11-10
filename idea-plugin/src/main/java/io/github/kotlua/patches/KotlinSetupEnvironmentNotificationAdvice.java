package io.github.kotlua.patches;

import io.github.kotlua.ide.patches.LuaPlatformHooks;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

public class KotlinSetupEnvironmentNotificationAdvice {

    public static class CollectNotificationDataAdvice {

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean onEnter(
                @Advice.Argument(0) Object project,
                @Advice.Argument(1) Object file
        ) {
            return LuaPlatformHooks.shouldSuppressKotlinNotConfigured(project, file);
        }

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.Enter boolean skipped,
                @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object result
        ) {
            if (skipped) {
                result = null;
            }
        }
    }
}

package io.github.kotlua.patches;

import io.github.kotlua.ide.patches.LuaPlatformHooks;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.jetbrains.kotlin.platform.TargetPlatform;

import java.util.List;

public class LLFirFactoryAdvice {

    public static class SessionConfigurationAdvice {

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean onEnter(
                @Advice.Argument(0) TargetPlatform targetPlatform
        ) {
            return LuaPlatformHooks.isLuaModulePlatformKind(targetPlatform);
        }

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.Argument(0) TargetPlatform targetPlatform,
                @Advice.Argument(1) Object project,
                @Advice.This Object companionInstance,
                @Advice.Enter boolean skipped,
                @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object result
        ) {
            if (skipped) {
                result = LuaPlatformHooks.createLuaPlatformSessionConfiguration(
                        project,
                        companionInstance.getClass().getClassLoader()
                );
            }
        }
    }

    public static class ComponentRegistrationAdvice {

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.Argument(0) TargetPlatform targetPlatform,
                @Advice.This Object companionInstance,
                @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) List result
        ) {
            if (LuaPlatformHooks.isLuaTargetPlatform(targetPlatform)) {
                result = LuaPlatformHooks.patchComponentRegistration(
                        result,
                        companionInstance.getClass().getClassLoader()
                );
            }
        }
    }

    public static class CreateSourcesSessionAdvice {

        @Advice.OnMethodEnter
        public static void onEnter(
                @Advice.Argument(0) Object module
        ) {
            LuaPlatformHooks.logModuleDependencies(module);
        }
    }
}

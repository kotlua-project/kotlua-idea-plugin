package io.github.kotlua.patches;

import io.github.kotlua.ide.patches.LuaPlatformHooks;
import net.bytebuddy.asm.Advice;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.config.KotlinFacetSettings;
import org.jetbrains.kotlin.platform.TargetPlatform;

public class FacetSerializationAdvice {

    public static class CreateArgumentsAdvice {

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean onEnter(
                @Advice.Argument(0) TargetPlatform targetPlatform
        ) {
            return LuaPlatformHooks.isLuaTargetPlatform(targetPlatform);
        }

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.Argument(0) Object targetPlatform,
                @Advice.Argument(1) Object initLambda,
                @Advice.Enter boolean skipped,
                @Advice.Return(readOnly = false) CommonCompilerArguments result
        ) {
            if (skipped) {
                result = LuaPlatformHooks.createLuaArguments(targetPlatform, initLambda);
            }
        }
    }

    public static class SerializeFacetSettingsAdvice {

        @Advice.OnMethodEnter
        public static void onEnter(
                @Advice.Argument(0) Object settings,
                @Advice.Argument(1) Object element
        ) {
            LuaPlatformHooks.beforeSerializeFacetSettings(settings, element);
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void onExit(
                @Advice.Argument(0) Object settings,
                @Advice.Argument(1) Object element,
                @Advice.Thrown Throwable thrown
        ) {
            LuaPlatformHooks.afterSerializeFacetSettings(settings, element, thrown);
        }
    }

    public static class DeserializeFacetSettingsAdvice {

        @Advice.OnMethodEnter
        public static void onEnter(
                @Advice.Argument(0) Object element
        ) {
            LuaPlatformHooks.beforeDeserializeFacetSettings(element);
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void onExit(
                @Advice.Argument(0) Object element,
                @Advice.Return KotlinFacetSettings resultSettings,
                @Advice.Thrown Throwable thrown
        ) {
            LuaPlatformHooks.afterDeserializeFacetSettings(element, resultSettings, thrown);
        }
    }

    public static class DeserializeTargetPlatformAdvice {

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.Argument(0) String platformString,
                @Advice.Return(readOnly = false) TargetPlatform result
        ) {
            if (platformString != null && platformString.contains("Lua")) {
                result = LuaPlatformHooks.patchDeserializedPlatformIfNeeded(platformString, result);
            }
        }
    }
}

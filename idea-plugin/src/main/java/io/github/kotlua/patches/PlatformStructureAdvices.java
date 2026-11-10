package io.github.kotlua.patches;

import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import io.github.kotlua.ide.patches.LuaPlatformHooks;
import kotlin.jvm.functions.Function1;
import net.bytebuddy.asm.Advice;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.idea.compiler.configuration.IdeKotlinVersion;
import org.jetbrains.kotlin.platform.IdePlatformKind;

public class PlatformStructureAdvices {

    public static class GetCompilerArgumentsAdvice {
        
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean onEnter(@Advice.Argument(0) Object platformKind) {
            return LuaPlatformHooks.isLuaPlatformKind(platformKind);
        }

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.This Object thisInstance,
                @Advice.Argument(0) Object platformKind,
                @Advice.Enter boolean skipped,
                @Advice.Return(readOnly = false) CommonCompilerArguments result
        ) {
            if (skipped) {
                result = LuaPlatformHooks.getCompilerArguments(thisInstance, platformKind);
            }
        }
    }

    public static class GetLibraryKindAdvice {
        
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean onEnter(@Advice.Argument(0) Object platformKind) {
            return LuaPlatformHooks.isLuaPlatformKind(platformKind);
        }

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.Argument(0) Object platformKind,
                @Advice.Enter boolean skipped,
                @Advice.Return(readOnly = false) PersistentLibraryKind<?> result
        ) {
            if (skipped) {
                result = LuaPlatformHooks.getLibraryKind(platformKind);
            }
        }
    }

    public static class GetLibraryPlatformKindAdvice {
        
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean onEnter(@Advice.Argument(0) Object file) {
            return LuaPlatformHooks.isLuaLibraryFile(file);
        }

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.Argument(0) Object file,
                @Advice.Enter boolean skipped,
                @Advice.Return(readOnly = false) IdePlatformKind result
        ) {
            if (skipped) {
                result = LuaPlatformHooks.getLibraryPlatformKind(file);
            }
        }
    }

    public static class GetLibraryVersionProviderAdvice {

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean onEnter(@Advice.Argument(0) Object platformKind) {
            return LuaPlatformHooks.isLuaPlatformKind(platformKind);
        }

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.This Object thisInstance,
                @Advice.Argument(0) Object platformKind,
                @Advice.Enter boolean skipped,
                @Advice.Return(readOnly = false) Function1<Library, IdeKotlinVersion> result
        ) {
            if (skipped) {
                result = LuaPlatformHooks.getLibraryVersionProvider(thisInstance, platformKind);
            }
        }
    }
}
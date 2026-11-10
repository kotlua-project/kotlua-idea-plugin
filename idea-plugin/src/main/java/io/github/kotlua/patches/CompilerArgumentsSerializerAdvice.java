package io.github.kotlua.patches;

import io.github.kotlua.ide.patches.LuaPlatformHooks;
import net.bytebuddy.asm.Advice;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;

public class CompilerArgumentsSerializerAdvice {

    public static class SerializeAdvice {

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean onEnter(
                @Advice.Argument(0) CommonCompilerArguments args
        ) {
            return LuaPlatformHooks.isLuaCompilerArguments(args);
        }

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.Argument(0) CommonCompilerArguments args,
                @Advice.FieldValue("gson") Object gson,
                @Advice.Enter boolean skipped,
                @Advice.Return(readOnly = false) String result
        ) {
            if (skipped) {
                result = LuaPlatformHooks.serializeLuaArguments(args, gson);
            }
        }
    }

    public static class DeserializeAdvice {

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean onEnter(
                @Advice.Argument(0) String str
        ) {
            return LuaPlatformHooks.isLuaSerializedString(str);
        }

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.Argument(0) String str,
                @Advice.FieldValue("gson") Object gson,
                @Advice.Enter boolean skipped,
                @Advice.Return(readOnly = false) CommonCompilerArguments result
        ) {
            if (skipped) {
                result = LuaPlatformHooks.deserializeLuaArguments(str, gson);
            }
        }
    }
}
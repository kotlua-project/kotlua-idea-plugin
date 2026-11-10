package io.github.kotlua.patches;

import io.github.kotlua.ide.patches.LuaPlatformHooks;
import net.bytebuddy.asm.Advice;
import org.jetbrains.kotlin.platform.IdePlatformKind;
import org.jetbrains.kotlin.platform.TargetPlatform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class KaLibraryModuleAdvice {

    public static class GetTargetPlatformGetterAdvice {

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean onEnter(
                @Advice.This Object thisModule,
                @Advice.Origin MethodHandles.Lookup lookup
        ) throws Throwable {
            MethodType type = MethodType.methodType(IdePlatformKind.class);
            MethodHandle handle = lookup.findVirtual(thisModule.getClass(), "getIdePlatformKind", type);
            Object kind = handle.invoke(thisModule);
            return LuaPlatformHooks.isLuaPlatformKind(kind);
        }

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.This Object thisModule,
                @Advice.Enter boolean skipped,
                @Advice.Return(readOnly = false) TargetPlatform result
        ) {
            if (skipped) {
                result = LuaPlatformHooks.getLuaTargetPlatform(thisModule);
            }
        }
    }
}

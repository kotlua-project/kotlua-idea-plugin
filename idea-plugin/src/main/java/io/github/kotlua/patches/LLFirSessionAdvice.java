package io.github.kotlua.patches;

import io.github.kotlua.ide.patches.LuaPlatformHooks;
import net.bytebuddy.asm.Advice;
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModulePlatformKind;

public class LLFirSessionAdvice {

    public static class IsMetadataSessionAdvice {

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.This Object session,
                @Advice.Return(readOnly = false) boolean isMetadata
        ) {
            if (isMetadata && LuaPlatformHooks.isLuaSession(session)) {
                isMetadata = false;
            }
        }
    }

    public static class GetPlatformKindAdvice {

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.This Object session,
                @Advice.Return KaModulePlatformKind platformKind
        ) {
            if (LuaPlatformHooks.isLuaSession(session)) {
                LuaPlatformHooks.onGetPlatformKindCalled(session, platformKind);
            }
        }
    }
}
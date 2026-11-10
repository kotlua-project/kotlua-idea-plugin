package io.github.kotlua.patches;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import io.github.kotlua.ide.patches.LuaPlatformHooks;
import net.bytebuddy.asm.Advice;

public class GetLibraryDescriptionAdvice {

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean onEnter(
            @Advice.Argument(1) Object platformKind
    ) {
        return LuaPlatformHooks.isLuaPlatformKind(platformKind);
    }

    @Advice.OnMethodExit
    public static void onExit(
            @Advice.Argument(0) Project project,
            @Advice.Enter boolean skipped,
            @Advice.Return(readOnly = false) CustomLibraryDescription result
    ) {
        if (skipped) {
            result = LuaPlatformHooks.getLibraryDescription(project);
        }
    }
}

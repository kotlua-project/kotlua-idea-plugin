package io.github.kotlua.patches;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import net.bytebuddy.asm.Advice;

@SuppressWarnings("UnstableApiUsage")
public class PluginClassLoaderAdvice {

    @Advice.OnMethodExit
    public static void onExit(
            @Advice.Argument(0) String name,
            @Advice.Return(readOnly = false) Class<?> result
    ) {
        if (result == null && name.startsWith("io.github.kotlua.")) {
            try {
                PluginId myPluginId = PluginId.getId("io.github.kotlua");
                
                IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(myPluginId);
                if (pluginDescriptor != null) {
                    ClassLoader kotluaClassLoader = pluginDescriptor.getPluginClassLoader();
                    if (kotluaClassLoader != null) {
                        result = kotluaClassLoader.loadClass(name);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
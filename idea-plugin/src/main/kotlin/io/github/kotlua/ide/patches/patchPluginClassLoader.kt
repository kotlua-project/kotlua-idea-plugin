package io.github.kotlua.ide.patches

import io.github.kotlua.patches.PluginClassLoaderAdvice
import net.bytebuddy.agent.builder.AgentBuilder.*
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.matcher.ElementMatchers.named
import net.bytebuddy.matcher.ElementMatchers.takesArgument
import java.lang.instrument.Instrumentation

fun Instrumentation.patchPluginClassLoader() {
    Default()
        .with(RedefinitionStrategy.RETRANSFORMATION)
        .with(TypeStrategy.Default.DECORATE)
        .with(InitializationStrategy.NoOp.INSTANCE)
        .with(
            LocationStrategy { classLoader, _ ->
                ClassFileLocator.Compound(
                    ClassFileLocator.ForClassLoader.of(classLoader),
                    ClassFileLocator.ForClassLoader.ofSystemLoader(),
                    ClassFileLocator.ForClassLoader.ofBootLoader()
                )
            }
        )
        .with(Listener.StreamWriting.toSystemOut().withErrorsOnly())
        .type(named("com.intellij.ide.plugins.cl.PluginClassLoader"))
        .transform { builder, _, _, _, _ ->
            builder.visit(
                Advice.to(PluginClassLoaderAdvice::class.java)
                    .on(
                        named<MethodDescription>("tryLoadingClass")
                            .and(takesArgument(0, String::class.java))
                            .and(takesArgument(1, Boolean::class.javaPrimitiveType))
                    )
            )
        }
        .installOn(this)
}

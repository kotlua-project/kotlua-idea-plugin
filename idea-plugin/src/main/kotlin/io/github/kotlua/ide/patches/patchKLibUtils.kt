package io.github.kotlua.ide.patches

import io.github.kotlua.patches.KLibUtilsAdvice
import net.bytebuddy.agent.builder.AgentBuilder.*
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.matcher.ElementMatchers.named
import net.bytebuddy.matcher.ElementMatchers.takesArguments
import java.lang.instrument.Instrumentation

fun Instrumentation.patchKLibUtils() {
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
        .type(named("org.jetbrains.kotlin.idea.base.platforms.KLibUtils"))
        .transform { builder, _, _, _, _ ->
            builder.visit(
                Advice.to(KLibUtilsAdvice.IsKlibLibraryRootForPlatformAdvice::class.java)
                    .on(
                        named<MethodDescription>("isKlibLibraryRootForPlatform")
                            .and(takesArguments(2))
                    )
            )
        }
        .installOn(this)
}

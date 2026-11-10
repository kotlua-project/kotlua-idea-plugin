package io.github.kotlua.ide.patches

import io.github.kotlua.patches.PlatformStructureAdvices
import net.bytebuddy.agent.builder.AgentBuilder.*
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.matcher.ElementMatchers.named
import net.bytebuddy.matcher.ElementMatchers.takesArguments
import java.lang.instrument.Instrumentation

fun Instrumentation.patchPlatformStructure() {
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
        .type(named("org.jetbrains.kotlin.idea.base.platforms.IdePlatformKindProjectStructure"))
        .transform { builder, _, _, _, _ ->
            builder.visit(
                Advice.to(PlatformStructureAdvices.GetCompilerArgumentsAdvice::class.java)
                    .on(named<MethodDescription>("getCompilerArguments").and(takesArguments(1)))
            )
                .visit(
                    Advice.to(PlatformStructureAdvices.GetLibraryVersionProviderAdvice::class.java)
                        .on(named<MethodDescription>("getLibraryVersionProvider").and(takesArguments(1)))
                )
        }
        .type(named("org.jetbrains.kotlin.idea.base.platforms.IdePlatformKindProjectStructure\$Companion"))
        .transform { builder, _, _, _, _ ->
            builder
                .visit(
                    Advice.to(PlatformStructureAdvices.GetLibraryKindAdvice::class.java)
                        .on(named<MethodDescription>("getLibraryKind").and(takesArguments(1)))
                )
                .visit(
                    Advice.to(PlatformStructureAdvices.GetLibraryPlatformKindAdvice::class.java)
                        .on(named<MethodDescription>("getLibraryPlatformKind").and(takesArguments(1)))
                )
        }
        .installOn(this)
}

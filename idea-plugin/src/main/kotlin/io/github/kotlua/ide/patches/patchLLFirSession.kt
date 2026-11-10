package io.github.kotlua.ide.patches

import io.github.kotlua.patches.LLFirSessionAdvice
import net.bytebuddy.agent.builder.AgentBuilder.*
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.matcher.ElementMatchers.named
import java.lang.instrument.Instrumentation

fun Instrumentation.patchLLFirSession() {
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
        .type(named("org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession"))
        .transform { builder, _, _, _, _ ->
            builder
                .visit(
                    Advice.to(LLFirSessionAdvice.IsMetadataSessionAdvice::class.java)
                        .on(named("isMetadataSession\$org_jetbrains_kotlin_low_level_api_fir"))
                )
                .visit(
                    Advice.to(LLFirSessionAdvice.GetPlatformKindAdvice::class.java)
                        .on(named("getPlatformKind\$org_jetbrains_kotlin_low_level_api_fir"))
                )
        }
        .installOn(this)
}

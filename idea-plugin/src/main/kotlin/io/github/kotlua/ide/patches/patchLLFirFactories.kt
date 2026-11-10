package io.github.kotlua.ide.patches

import io.github.kotlua.patches.LLFirFactoryAdvice
import net.bytebuddy.agent.builder.AgentBuilder.*
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.matcher.ElementMatchers.named
import net.bytebuddy.matcher.ElementMatchers.takesArguments
import java.lang.instrument.Instrumentation

fun Instrumentation.patchLLFirFactories() {
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
        .type(named($$"org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.configuration.LLPlatformSessionConfiguration$Companion"))
        .transform { builder, _, _, _, _ ->
            builder.visit(
                Advice.to(LLFirFactoryAdvice.SessionConfigurationAdvice::class.java)
                    .on(named<MethodDescription>("forPlatform").and(takesArguments(2)))
            )
        }
        .type(named($$"org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.components.LLPlatformSessionComponentRegistration$Companion"))
        .transform { builder, _, _, _, _ ->
            builder.visit(
                Advice.to(LLFirFactoryAdvice.ComponentRegistrationAdvice::class.java)
                    .on(named<MethodDescription>("forPlatform").and(takesArguments(1)))
            )
        }
        .type(named("org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.LLFirSessionFactory"))
        .transform { builder, _, _, _, _ ->
            builder.visit(
                Advice.to(LLFirFactoryAdvice.CreateSourcesSessionAdvice::class.java)
                    .on(named<MethodDescription>("createSourcesSession").and(takesArguments(1)))
            )
        }
        .installOn(this)
}

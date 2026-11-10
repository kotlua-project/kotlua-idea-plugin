package io.github.kotlua.ide.patches

import io.github.kotlua.patches.FacetSerializationAdvice
import net.bytebuddy.agent.builder.AgentBuilder.*
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.matcher.ElementMatchers.named
import net.bytebuddy.matcher.ElementMatchers.takesArguments
import java.lang.instrument.Instrumentation

fun Instrumentation.patchFacetSerialization() {
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
        .type(named("org.jetbrains.kotlin.config.FacetSerializationKt"))
        .transform { builder, _, _, _, _ ->
            builder
                .visit(
                    Advice.to(FacetSerializationAdvice.CreateArgumentsAdvice::class.java)
                        .on(
                            named<MethodDescription>("createArguments")
                                .and(takesArguments(2))
                        )
                )
                .visit(
                    Advice.to(FacetSerializationAdvice.SerializeFacetSettingsAdvice::class.java)
                        .on(
                            named<MethodDescription>("serializeFacetSettings")
                                .and(takesArguments(2))
                        )
                )
                .visit(
                    Advice.to(FacetSerializationAdvice.DeserializeFacetSettingsAdvice::class.java)
                        .on(
                            named<MethodDescription>("deserializeFacetSettings")
                                .and(takesArguments(1))
                        )
                )
                .visit(
                    Advice.to(FacetSerializationAdvice.DeserializeTargetPlatformAdvice::class.java)
                        .on(
                            named<MethodDescription>("deserializeTargetPlatformByComponentPlatforms")
                            .and(takesArguments(1))
                        )
                )
        }
        .installOn(this)
}

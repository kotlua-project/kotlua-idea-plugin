package io.github.kotlua.gradle.model

import com.intellij.gradle.toolingExtension.modelAction.GradleModelController
import org.gradle.tooling.model.gradle.GradleBuild
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider.GradleModelConsumer

class KotluaModelProvider : ProjectImportModelProvider {

    @Suppress("UnstableApiUsage")
    override fun populateModels(
        controller: GradleModelController,
        buildModels: Collection<GradleBuild>,
        modelConsumer: GradleModelConsumer
    ) {
        controller.fetchRequest(buildModels, KotluaGradleModel::class.java)
            .executionMode(GradleModelController.GradleModelFetchRequest.GradleExecutionMode.SEQUENTIAL)
            .suppressFailures()
            .execute(modelConsumer)
    }
}
package io.github.kotlua.ide.platform

import com.intellij.openapi.roots.ProjectFileIndex
import io.github.kotlua.platform.LuaPlatforms
import org.jetbrains.kotlin.idea.base.facet.platform.TargetPlatformDetector
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile

class KotluaTargetPlatformDetector : TargetPlatformDetector {
    override fun detectPlatform(file: KtFile): TargetPlatform? {
        val virtualFile = file.originalFile.virtualFile ?: file.virtualFile ?: return null
        val project = file.project

        val module = ProjectFileIndex.getInstance(project).getModuleForFile(virtualFile) ?: return null
        val facet = KotlinFacet.get(module) ?: return null

        if (facet.configuration.settings.targetPlatform == LuaPlatforms.defaultLuaPlatform) {
            return LuaPlatforms.defaultLuaPlatform
        }

        return null
    }
}

package io.github.kotlua.ide.platform

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.LibraryType
import com.intellij.openapi.roots.libraries.LibraryTypeService
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration
import com.intellij.openapi.roots.libraries.ui.DescendentBasedRootFilter
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent
import com.intellij.openapi.roots.libraries.ui.LibraryPropertiesEditor
import com.intellij.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor
import com.intellij.openapi.roots.libraries.ui.RootDetector
import com.intellij.openapi.roots.ui.configuration.libraryEditor.DefaultLibraryRootsComponentDescriptor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinIcons
import javax.swing.Icon
import javax.swing.JComponent

class KotluaLibraryType : LibraryType<DummyLibraryProperties>(LuaLibraryKind) {

    override fun getCreateActionName(): String = "Kotlin/Lua"

    override fun getIcon(properties: DummyLibraryProperties?): Icon = KotlinIcons.SMALL_LOGO

    override fun createPropertiesEditor(editorComponent: LibraryEditorComponent<DummyLibraryProperties>): LibraryPropertiesEditor? {
        return null
    }

    override fun createLibraryRootsComponentDescriptor(): LibraryRootsComponentDescriptor {
        return RootsComponentDescriptor
    }

    override fun createNewLibrary(
        parentComponent: JComponent,
        contextDirectory: VirtualFile?,
        project: Project
    ): NewLibraryConfiguration? = LibraryTypeService.getInstance().createLibraryFromFiles(
        RootsComponentDescriptor,
        parentComponent, 
        contextDirectory, 
        this,
        project
    )

    override fun detect(classesRoots: List<VirtualFile>): DummyLibraryProperties? {
        for (root in classesRoots) {
            if (isAcceptedForLuaLibrary(root.extension)) {
                return DummyLibraryProperties.INSTANCE
            }
        }
        return null
    }

    companion object {
        fun isAcceptedForLuaLibrary(extension: String?): Boolean = extension == "klib"
    }

    object RootsComponentDescriptor : DefaultLibraryRootsComponentDescriptor() {
        override fun createAttachFilesChooserDescriptor(libraryName: String?): FileChooserDescriptor {
            val descriptor = FileChooserDescriptor(true, true, true, false, true, true)
                .withExtensionFilter("Kotlin/Lua Library (*.klib)", "klib")
                .withFileFilter { FileElement.isArchive(it) || isAcceptedForLuaLibrary(it.extension) }
            
            descriptor.title = if (StringUtil.isEmpty(libraryName))
                "Attach Kotlin/Lua Files"
            else
                "Attach Kotlin/Lua Files to $libraryName"
            descriptor.description = "Choose .klib files or directories with Lua libraries"
            return descriptor
        }

        override fun getRootTypes(): Array<OrderRootType?> = arrayOf(OrderRootType.CLASSES, OrderRootType.SOURCES)

        override fun getRootDetectors(): List<RootDetector> = arrayListOf(
            DescendentBasedRootFilter(OrderRootType.CLASSES, false, "Kotlin/Lua KLIB files") {
                isAcceptedForLuaLibrary(it.extension)
            },
            DescendentBasedRootFilter.createFileTypeBasedFilter(
                OrderRootType.SOURCES, 
                false, 
                KotlinFileType.INSTANCE, 
                "sources"
            )
        )
    }
}

package com.ugarosa.neovim.window

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.ugarosa.neovim.buffer.NeovimBufferManager
import com.ugarosa.neovim.common.focusEditor
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.client.api.SplitDirection
import com.ugarosa.neovim.rpc.client.api.resetWindow
import com.ugarosa.neovim.rpc.client.api.splitWindow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.PROJECT)
class NeovimWindowManager(
    private val project: Project,
) {
    private val logger = myLogger()
    private val client = service<NeovimClient>()
    private val bufferManager = project.service<NeovimBufferManager>()

    private val prevLayout = AtomicReference<EditorLayout>()
    private val windowMap = ConcurrentHashMap<WindowId, NeovimWindow>()

    suspend fun syncLayout() {
        val layout = EditorLayout.Parser.current(project) ?: return
        if (layout == prevLayout.get()) {
            onSelected()
            return
        }
        prevLayout.set(layout)

        logger.warn("Layout changed: $layout")
        val lastWindowId = client.resetWindow()
        splitNeovimWindow(layout, lastWindowId)

        onSelected()
    }

    private suspend fun onSelected() {
        focusEditor()?.let { editor ->
            val buffer = bufferManager.findByEditor(editor)
            buffer.onSelected()
        }
    }

    private suspend fun splitNeovimWindow(
        layout: EditorLayout,
        windowId: WindowId,
    ) {
        when (layout) {
            is EditorLayout.Leaf.Grid -> {
                windowMap[windowId] = initializeGrid(windowId, layout.editor)
            }

            is EditorLayout.Leaf.Diff -> {
                val rightWindowId = client.splitWindow(windowId, SplitDirection.Right)

                val leftWindow = initializeGrid(windowId, layout.left.editor)
                val rightWindow = initializeGrid(rightWindowId, layout.right.editor)
                val diff = NeovimWindow.Diff(leftWindow, rightWindow)

                windowMap[windowId] = diff
                windowMap[rightWindowId] = diff
            }

            is EditorLayout.Leaf.Patch -> {
                val midWindowId = client.splitWindow(windowId, SplitDirection.Right)
                val rightWindowId = client.splitWindow(midWindowId, SplitDirection.Right)

                val leftWindow = initializeGrid(windowId, layout.left.editor)
                val midWindow = initializeGrid(midWindowId, layout.mid.editor)
                val rightWindow = initializeGrid(rightWindowId, layout.right.editor)
                val patch = NeovimWindow.Patch(leftWindow, midWindow, rightWindow)

                windowMap[windowId] = patch
                windowMap[midWindowId] = patch
                windowMap[rightWindowId] = patch
            }

            is EditorLayout.HorizontalSplit -> {
                val belowWindowId = client.splitWindow(windowId, SplitDirection.Below)
                splitNeovimWindow(layout.top, windowId)
                splitNeovimWindow(layout.bottom, belowWindowId)
            }

            is EditorLayout.VerticalSplit -> {
                val rightWindowId = client.splitWindow(windowId, SplitDirection.Right)
                splitNeovimWindow(layout.left, windowId)
                splitNeovimWindow(layout.right, rightWindowId)
            }
        }
    }

    private suspend fun initializeGrid(
        windowId: WindowId,
        editor: EditorEx,
    ): NeovimWindow.Grid {
        val buffer = bufferManager.register(editor, windowId)
        return NeovimWindow.Grid(windowId, buffer)
    }
}

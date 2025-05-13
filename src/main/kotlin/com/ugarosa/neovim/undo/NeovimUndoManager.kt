package com.ugarosa.neovim.undo

import com.intellij.history.Label
import com.intellij.history.LocalHistory
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.jetbrains.rd.util.AtomicInteger
import com.ugarosa.neovim.logger.myLogger

@Service(Service.Level.PROJECT)
class NeovimUndoManager(
    private val project: Project,
) {
    private val logger = myLogger()
    private val checkPoints = mutableListOf<Label>()
    private var currentIdx = -1

    private val prefix = "NeovimUndo"
    private val id = AtomicInteger(0)

    private fun nextName(): String {
        return "$prefix:${id.incrementAndGet()}"
    }

    fun setCheckpoint() {
        if (currentIdx < checkPoints.size - 1) {
            checkPoints.subList(currentIdx + 1, checkPoints.size).clear()
        }
        val label = LocalHistory.getInstance().putSystemLabel(project, nextName())
        logger.trace("setCheckpoint: $label")
        checkPoints.add(label)
        currentIdx = checkPoints.size - 1
    }

    private fun revertTo(label: Label) {
        project.guessProjectDir()?.let {
            label.revert(project, it)
        }
    }

    fun undo() {
        if (currentIdx > 0) {
            currentIdx--
            val label = checkPoints[currentIdx]
            logger.trace("undo: $label")
            revertTo(label)
        }
    }

    fun redo() {
        if (currentIdx < checkPoints.size - 1) {
            currentIdx++
            val label = checkPoints[currentIdx]
            logger.trace("redo: $label")
            revertTo(label)
        }
    }
}

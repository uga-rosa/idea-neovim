package com.ugarosa.neovim.undo

import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.CommandProcessorEx
import com.intellij.openapi.command.CommandToken
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.ugarosa.neovim.logger.myLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.APP)
class NeovimUndoManager {
    private val logger = myLogger()
    private val commandProcessorEx = CommandProcessor.getInstance() as CommandProcessorEx
    private var token: CommandToken? = null

    suspend fun start(
        project: Project,
        document: Document,
    ) = withContext(Dispatchers.EDT) {
        if (token != null) return@withContext
        token =
            commandProcessorEx.startCommand(
                project,
                "Insert Mode Input",
                document,
                UndoConfirmationPolicy.DEFAULT,
            )
        logger.trace("Start undo block: $token")
    }

    suspend fun finish() =
        withContext(Dispatchers.EDT) {
            token?.let {
                commandProcessorEx.finishCommand(it, null)
                token = null
                logger.trace("Finish undo block: $it")
            }
        }
}

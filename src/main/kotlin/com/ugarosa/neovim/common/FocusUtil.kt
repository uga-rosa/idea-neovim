package com.ugarosa.neovim.common

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.ugarosa.neovim.logger.MyLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = MyLogger.getInstance("com.ugarosa.neovim.common.FocusUtils")

// This function is not safe to call from a non-EDT thread.
fun unsafeFocusEditor(): EditorEx? {
    val dataContext = getFocusContext() ?: return null
    val editor = CommonDataKeys.EDITOR.getData(dataContext)
    return editor as? EditorEx
}

suspend fun focusEditor(): EditorEx? = withContext(Dispatchers.EDT) { unsafeFocusEditor() }

suspend fun focusProject(): Project? =
    withContext(Dispatchers.EDT) {
        val dataContext = getFocusContext() ?: return@withContext null
        CommonDataKeys.PROJECT.getData(dataContext)
    }

private fun getFocusContext(): DataContext? {
    val focusOwner =
        IdeFocusManager.getGlobalInstance().focusOwner
            ?: return null
    return try {
        DataManager.getInstance().getDataContext(focusOwner)
    } catch (e: Exception) {
        logger.warn("Failed to get DataContext", e)
        null
    }
}

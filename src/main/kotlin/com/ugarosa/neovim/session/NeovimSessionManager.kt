package com.ugarosa.neovim.session

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.ugarosa.neovim.rpc.BufferId
import kotlinx.coroutines.CoroutineScope

interface NeovimSessionManager {
    suspend fun getSession(editor: Editor): NeovimEditorSession

    suspend fun getSession(bufferId: BufferId): NeovimEditorSession

    suspend fun getEditor(bufferId: BufferId): Editor

    fun register(
        scope: CoroutineScope,
        editor: Editor,
        project: Project,
        disposable: Disposable,
    )
}

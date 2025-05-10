package com.ugarosa.neovim.session

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.ugarosa.neovim.rpc.BufferId
import kotlinx.coroutines.CoroutineScope

interface NeovimSessionManager {
    suspend fun get(bufferId: BufferId): NeovimEditorSession

    suspend fun get(editor: Editor): NeovimEditorSession

    fun register(
        scope: CoroutineScope,
        editor: Editor,
        project: Project,
        disposable: Disposable,
    )
}

package com.ugarosa.neovim.adapter.idea.lifecycle

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.components.service
import com.ugarosa.neovim.adapter.idea.undo.NvimUndoManager
import com.ugarosa.neovim.adapter.nvim.incoming.IncomingEventsRegistry

class NvimAppLifecycleListener : AppLifecycleListener {
    override fun appFrameCreated(commandLineArgs: List<String?>) {
        service<AppInitializer>()
        service<IdeaBufferCoordinatorRegistry>()
        service<IncomingEventsRegistry>()
        service<NvimUndoManager>()
    }
}

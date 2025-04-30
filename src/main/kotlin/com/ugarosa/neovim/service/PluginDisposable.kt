package com.ugarosa.neovim.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class PluginDisposable : Disposable {
    override fun dispose() {}
}

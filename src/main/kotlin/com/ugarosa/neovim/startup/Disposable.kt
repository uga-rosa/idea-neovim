package com.ugarosa.neovim.startup

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
class AppDisposable : Disposable {
    override fun dispose() {}
}

@Service(Service.Level.PROJECT)
class ProjectDisposable : Disposable {
    override fun dispose() {}
}

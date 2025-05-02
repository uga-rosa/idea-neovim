package com.ugarosa.neovim.startup

import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
class CoroutineService(
    val scope: CoroutineScope,
)

package com.ugarosa.neovim.common

import com.intellij.openapi.components.service
import com.ugarosa.neovim.config.neovim.NeovimOptionManager
import com.ugarosa.neovim.config.neovim.NeovimOptionManagerImpl
import com.ugarosa.neovim.mode.NeovimModeManager
import com.ugarosa.neovim.mode.NeovimModeManagerImpl
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.client.NeovimRpcClientImpl

fun getClient(): NeovimRpcClient = service<NeovimRpcClientImpl>()

fun getOptionManager(): NeovimOptionManager = service<NeovimOptionManagerImpl>()

fun getModeManager(): NeovimModeManager = service<NeovimModeManagerImpl>()

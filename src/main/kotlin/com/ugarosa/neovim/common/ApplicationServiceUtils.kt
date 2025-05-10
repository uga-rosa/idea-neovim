package com.ugarosa.neovim.common

import com.intellij.openapi.components.service
import com.ugarosa.neovim.config.idea.NeovimKeymapSettings
import com.ugarosa.neovim.config.idea.NeovimKeymapSettingsImpl
import com.ugarosa.neovim.config.neovim.NeovimOptionManager
import com.ugarosa.neovim.config.neovim.NeovimOptionManagerImpl
import com.ugarosa.neovim.keymap.router.NeovimKeyRouter
import com.ugarosa.neovim.keymap.router.NeovimKeyRouterImpl
import com.ugarosa.neovim.mode.NeovimModeManager
import com.ugarosa.neovim.mode.NeovimModeManagerImpl
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.client.NeovimRpcClientImpl
import com.ugarosa.neovim.session.NeovimSessionManager
import com.ugarosa.neovim.session.NeovimSessionManagerImpl

fun getClient(): NeovimRpcClient = service<NeovimRpcClientImpl>()

fun getSessionManager(): NeovimSessionManager = service<NeovimSessionManagerImpl>()

fun getOptionManager(): NeovimOptionManager = service<NeovimOptionManagerImpl>()

fun getModeManager(): NeovimModeManager = service<NeovimModeManagerImpl>()

fun getKeyRouter(): NeovimKeyRouter = service<NeovimKeyRouterImpl>()

fun getKeymapSettings(): NeovimKeymapSettings = service<NeovimKeymapSettingsImpl>()

package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.rpc.client.NvimClient

suspend fun NvimClient.installHook() {
    val chanId = deferredChanId.await()
    execLuaNotify("hook", "install", listOf(chanId))
}

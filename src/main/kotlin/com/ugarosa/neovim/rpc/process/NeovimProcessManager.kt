package com.ugarosa.neovim.rpc.process

import java.io.InputStream
import java.io.OutputStream

interface NeovimProcessManager {
    fun getInputStream(): InputStream

    fun getOutputStream(): OutputStream

    fun close()
}

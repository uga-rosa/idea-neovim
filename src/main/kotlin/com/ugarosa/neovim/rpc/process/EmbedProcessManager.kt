package com.ugarosa.neovim.rpc.process

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class EmbedProcessManager : NeovimProcessManager {
    private val process: Process
    private val inputStream: InputStream
    private val outputStream: OutputStream

    init {
        val pluginRoot =
            PluginManagerCore
                .getPlugin(PluginId.getId("com.ugarosa.neovim"))!!
                .pluginPath.toAbsolutePath().toString()
        val rtpDir = "$pluginRoot/runtime"
        val builder =
            ProcessBuilder(
                "nvim",
                "--embed",
                "--headless",
                "--cmd",
                "set rtp+=$rtpDir",
            )
        builder.redirectErrorStream(true)
        process = builder.start()
        inputStream = process.inputStream
        outputStream = process.outputStream
    }

    override fun getInputStream() = inputStream

    override fun getOutputStream() = outputStream

    override fun close() {
        runCatching { inputStream.close() }
        runCatching { outputStream.close() }
        runCatching { process.destroy() }
        runCatching {
            if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
            }
        }
    }
}

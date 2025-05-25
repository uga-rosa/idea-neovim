package com.ugarosa.neovim.rpc.process

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class NvimProcessManager {
    private val process: Process
    private val inputStream: InputStream
    private val outputStream: OutputStream

    init {
        val plugin = PluginManagerCore.getPlugin(PluginId.getId("com.ugarosa.neovim"))!!
        val rtpDir = plugin.pluginPath.resolve("runtime").toAbsolutePath().toString()
        val builder =
            ProcessBuilder(
                "nvim",
                "--embed",
                "--headless",
                "--cmd",
                "let g:intellij=v:true",
                "-c",
                "execute 'set rtp+=$rtpDir'",
                "-c",
                "runtime plugin/intellij.lua",
            )
        builder.redirectErrorStream(true)
        process = builder.start()
        inputStream = process.inputStream
        outputStream = process.outputStream
    }

    fun getInputStream() = inputStream

    fun getOutputStream() = outputStream

    fun close() {
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

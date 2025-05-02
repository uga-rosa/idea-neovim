package com.ugarosa.neovim.rpc

import com.intellij.openapi.diagnostic.thisLogger
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.TimeUnit

interface NeovimProcessManager {
    fun getInputStream(): InputStream

    fun getOutputStream(): OutputStream

    fun close()
}

class NeovimProcessManagerImpl : NeovimProcessManager {
    private val delegate: NeovimProcessManager =
        run {
            val addr = System.getProperty("nvim.listen.address")
            thisLogger().warn("Neovim address: $addr")
            if (addr != null) {
                SocketProcessManager(addr)
            } else {
                EmbedProcessManager()
            }
        }

    override fun getInputStream() = delegate.getInputStream()

    override fun getOutputStream() = delegate.getOutputStream()

    override fun close() = delegate.close()
}

class SocketProcessManager(
    address: String,
) : NeovimProcessManager {
    private val socket: Socket
    private val inputStream: InputStream
    private val outputStream: OutputStream

    init {
        val (host, portStr) = address.split(":")
        val port = portStr.toInt()
        socket = Socket(host, port)
        inputStream = socket.getInputStream()
        outputStream = socket.getOutputStream()
    }

    override fun getInputStream() = inputStream

    override fun getOutputStream() = outputStream

    override fun close() {
        socket.close()
    }
}

class EmbedProcessManager : NeovimProcessManager {
    private val process: Process
    private val inputStream: InputStream
    private val outputStream: OutputStream

    init {
        val builder =
            ProcessBuilder(
                "nvim",
                "--embed",
                "--headless",
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

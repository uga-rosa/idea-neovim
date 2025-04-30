package com.ugarosa.neovim.infra

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class NeovimProcessManager {
    private lateinit var process: Process
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream

    init {
        val debugAddr = System.getenv("NVIM_LISTEN_ADDRESS")
        if (debugAddr != null) {
            connectToExistingProcess(debugAddr)
        } else {
            startNewProcess()
        }
    }

    private fun connectToExistingProcess(debugAddress: String) {
        val (host, portStr) = debugAddress.split(":")
        val port = portStr.toInt()
        val socket = Socket(host, port)
        inputStream = socket.getInputStream()
        outputStream = socket.getOutputStream()
    }

    private fun startNewProcess() {
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

    fun getInputStream(): InputStream = inputStream

    fun getOutputStream(): OutputStream = outputStream
}

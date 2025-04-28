package com.ugarosa.neovim.infra

import java.io.InputStream
import java.io.OutputStream

class NeovimProcessManager {
    private lateinit var process: Process
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream

    init {
        start()
    }

    private fun start() {
        val builder = ProcessBuilder(
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
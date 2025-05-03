package com.ugarosa.neovim.rpc.process

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

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

package com.ugarosa.neovim.infra

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.ugarosa.neovim.common.utf8ByteOffsetToCharOffset
import org.msgpack.core.MessagePack
import org.msgpack.value.Value
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger

class NeovimRpcClient(
    input: InputStream,
    output: OutputStream
) {
    private val messageIdGenerator = AtomicInteger(0)
    private val packer = MessagePack.newDefaultPacker(output)
    private val unpacker = MessagePack.newDefaultUnpacker(input)

    fun sendRequest(method: String, params: List<Any?> = emptyList()): Int {
        val msgId = messageIdGenerator.getAndIncrement()

        packer.packArrayHeader(4)
        packer.packInt(0) // 0 = Request
        packer.packInt(msgId)
        packer.packString(method)
        packParams(params)
        packer.flush()

        return msgId
    }

    fun receiveResponse(): Response {
        unpacker.unpackArrayHeader() // should be 4
        val type = unpacker.unpackInt()
        check(type == 1) { "Expected response (1), but got $type" }

        val msgId = unpacker.unpackInt()
        val error = unpacker.unpackValue()
        val result = unpacker.unpackValue()

        return Response(msgId, error, result)
    }

    fun initializeNeovimBuffer(editor: Editor) {
        val document = editor.document
        val text = document.text
        val lines = text.split("\n")

        sendRequest(
            "nvim_buf_set_lines",
            listOf(0, 0, -1, false, lines)
        )
        receiveResponse()
    }

    fun sendInput(key: String) {
        sendRequest("nvim_input", listOf(key))
        receiveResponse()
    }

    fun getCursor(editor: Editor): LogicalPosition {
        val document = editor.document

        sendRequest("nvim_win_get_cursor", listOf(0))
        val response = receiveResponse()

        val cursorArray = response.result.asArrayValue().list()
        // Neovim uses (1, 0) byte-based indexing
        val nvimRow = cursorArray[0].asIntegerValue().asInt()
        val nvimByteCol = cursorArray[1].asIntegerValue().asInt()

        val lineIndex = nvimRow - 1 // IntelliJ uses 0-based line indexing

        if (lineIndex < 0 || lineIndex >= document.lineCount) {
            return LogicalPosition(0, 0)
        }

        val lineStartOffset = document.getLineStartOffset(lineIndex)
        val lineEndOffset = document.getLineEndOffset(lineIndex)

        val lineText = document.text.substring(lineStartOffset, lineEndOffset)

        val correctedCol = utf8ByteOffsetToCharOffset(lineText, nvimByteCol)

        return LogicalPosition(lineIndex, correctedCol)
    }

    private fun packParam(param: Any?) {
        when (param) {
            is String -> packer.packString(param)
            is Int -> packer.packInt(param)
            is Boolean -> packer.packBoolean(param)
            is List<*> -> {
                packer.packArrayHeader(param.size)
                param.forEach { packParam(it) }
            }

            null -> packer.packNil()
            else -> throw IllegalArgumentException("Unsupported param type: ${param::class}")
        }
    }

    private fun packParams(params: List<Any?>) {
        packer.packArrayHeader(params.size)
        params.forEach { packParam(it) }
    }

    data class Response(
        val msgId: Int,
        val error: Value,
        val result: Value
    )
}
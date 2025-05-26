package com.ugarosa.neovim.adapter.nvim.outgoing

import com.intellij.openapi.components.service
import com.ugarosa.neovim.bus.BufferChanged
import com.ugarosa.neovim.bus.IdeaDocumentChanged
import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.rpc.client.NvimClient
import com.ugarosa.neovim.rpc.client.api.CHANGED_TICK
import com.ugarosa.neovim.rpc.client.api.bufVar
import com.ugarosa.neovim.rpc.client.api.bufferAttach
import com.ugarosa.neovim.rpc.client.api.bufferSetLines
import com.ugarosa.neovim.rpc.client.api.bufferSetText
import com.ugarosa.neovim.rpc.client.api.insert
import com.ugarosa.neovim.rpc.client.api.modifiable
import com.ugarosa.neovim.rpc.client.api.noModifiable
import com.ugarosa.neovim.rpc.client.api.setCursor
import com.ugarosa.neovim.rpc.client.api.setFiletype

class DocumentCommandAdapter(
    private val bufferId: BufferId,
) {
    private val client = service<NvimClient>()
    private val ignoreTicks = mutableSetOf<Long>()

    suspend fun replaceAll(lines: List<String>) {
        client.bufferSetLines(bufferId, 0, -1, lines)
    }

    suspend fun setFiletype(path: String) {
        client.setFiletype(bufferId, path)
    }

    suspend fun attach() {
        client.bufferAttach(bufferId)
    }

    suspend fun send(event: BufferChanged) {
        val docChanged = event.documentChanged
        val currentTick = client.bufVar(bufferId, CHANGED_TICK)
        when (docChanged) {
            is IdeaDocumentChanged.NearCursor -> {
                val beforeOffset = docChanged.caretOffset
                val afterOffset = event.caretMoved?.offset ?: beforeOffset
                val advanced = afterOffset - (beforeOffset - docChanged.beforeDelete)
                val inputBefore = docChanged.text.take(advanced)
                val inputAfter = docChanged.text.drop(advanced)
                val ignoreIncrement =
                    client.insert(docChanged.beforeDelete, docChanged.afterDelete, inputBefore, inputAfter)
                ignoreTicks.addAll(currentTick + 1..currentTick + ignoreIncrement)
            }

            is IdeaDocumentChanged.FarCursor -> {
                ignoreTicks.add(currentTick + 1)
                client.bufferSetText(bufferId, docChanged.start, docChanged.end, docChanged.replacement)
                event.caretMoved?.let { caretMoved ->
                    client.setCursor(bufferId, caretMoved.pos)
                }
            }
        }
    }

    fun isIgnored(tick: Long): Boolean {
        return ignoreTicks.remove(tick)
    }

    suspend fun changeModifiable(isWritable: Boolean) {
        if (isWritable) {
            client.modifiable(bufferId)
        } else {
            client.noModifiable(bufferId)
        }
    }
}

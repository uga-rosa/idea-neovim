package com.ugarosa.neovim.adapter.nvim.outgoing

import com.intellij.openapi.components.service
import com.ugarosa.neovim.domain.buffer.FixedChange
import com.ugarosa.neovim.domain.buffer.RepeatableChange
import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.rpc.client.NvimClient
import com.ugarosa.neovim.rpc.client.api.CHANGED_TICK
import com.ugarosa.neovim.rpc.client.api.bufVar
import com.ugarosa.neovim.rpc.client.api.bufferAttach
import com.ugarosa.neovim.rpc.client.api.bufferSetLines
import com.ugarosa.neovim.rpc.client.api.bufferSetText
import com.ugarosa.neovim.rpc.client.api.input
import com.ugarosa.neovim.rpc.client.api.modifiable
import com.ugarosa.neovim.rpc.client.api.noModifiable
import com.ugarosa.neovim.rpc.client.api.sendRepeatableChange
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

    suspend fun setText(change: FixedChange) {
        val currentTick = client.bufVar(bufferId, CHANGED_TICK)
        ignoreTicks.add(currentTick + 1)
        client.bufferSetText(bufferId, change.start, change.end, change.replacement)
    }

    suspend fun sendRepeatableChange(change: RepeatableChange) {
        val currentTick = client.bufVar(bufferId, CHANGED_TICK)
        ignoreTicks.addAll(currentTick + 1..currentTick + change.ignoreTickIncrement)
        client.sendRepeatableChange(change)
    }

    suspend fun escape() {
        client.input("<Esc>")
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

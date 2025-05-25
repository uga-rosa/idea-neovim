package com.ugarosa.neovim.bus

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Service(Service.Level.APP)
class NvimToIdeaBus(
    private val scope: CoroutineScope,
) : Disposable {
    private val batchWindow: Duration = 50.milliseconds
    private val channel = Channel<NvimToIdeaEvent>(Channel.UNLIMITED)

    fun tryEmit(event: NvimToIdeaEvent) {
        channel.trySend(event)
    }

    private val _batchedBufLines = MutableSharedFlow<List<NvimBufLines>>()
    val batchedBufLines: SharedFlow<List<NvimBufLines>> = _batchedBufLines.asSharedFlow()

    private val _latestCursor = MutableSharedFlow<NvimCursorMoved>()
    val latestCursor: SharedFlow<NvimCursorMoved> = _latestCursor.asSharedFlow()

    private val _latestMode = MutableSharedFlow<ModeChanged>()
    val latestMode: SharedFlow<ModeChanged> = _latestMode.asSharedFlow()

    private val _latestVisualSelection = MutableSharedFlow<VisualSelectionChanged>()
    val latestVisualSelection: SharedFlow<VisualSelectionChanged> = _latestVisualSelection.asSharedFlow()

    init {
        scope.launch {
            val bufBucket = mutableListOf<NvimBufLines>()
            var cursorShadow: NvimCursorMoved? = null
            var modeShadow: ModeChanged? = null
            var visualShadow: VisualSelectionChanged? = null

            suspend fun flush() {
                if (bufBucket.isNotEmpty()) {
                    _batchedBufLines.emit(bufBucket.toList())
                    bufBucket.clear()
                }
                cursorShadow?.let {
                    _latestCursor.emit(it)
                    cursorShadow = null
                }
                modeShadow?.let {
                    _latestMode.emit(it)
                    modeShadow = null
                }
                visualShadow?.let {
                    _latestVisualSelection.emit(it)
                    visualShadow = null
                }
            }

            fun received(event: NvimToIdeaEvent) {
                when (event) {
                    is NvimBufLines -> bufBucket.add(event)

                    is NvimCursorMoved -> cursorShadow = event

                    is ModeChanged -> modeShadow = event

                    is VisualSelectionChanged -> visualShadow = event
                }
            }

            while (isActive) {
                val first = channel.receive()
                received(first)

                while (true) {
                    val next = withTimeoutOrNull(batchWindow) { channel.receive() }
                    if (next == null) break
                    received(next)
                }
                flush()
            }
        }
    }

    override fun dispose() {
        scope.cancel()
    }
}

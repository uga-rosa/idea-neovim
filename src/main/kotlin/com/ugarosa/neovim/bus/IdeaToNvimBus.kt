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

data class BufferChanged(
    val documentChanged: IdeaDocumentChanged,
    var caretMoved: IdeaCaretMoved?,
)

@Service(Service.Level.APP)
class IdeaToNvimBus(
    private val scope: CoroutineScope,
) : Disposable {
    private val batchWindow: Duration = 20.milliseconds
    private val channel = Channel<IdeaToNvimEvent>(Channel.UNLIMITED)

    fun tryEmit(event: IdeaToNvimEvent) {
        channel.trySend(event)
    }

    private val _bufferChanges = MutableSharedFlow<BufferChanged>()
    val bufferChanges: SharedFlow<BufferChanged> = _bufferChanges.asSharedFlow()

    private val _caretMoved = MutableSharedFlow<IdeaCaretMoved>()
    val caretMoved: SharedFlow<IdeaCaretMoved> = _caretMoved.asSharedFlow()

    private val _editorSelected = MutableSharedFlow<EditorSelected>()
    val editorSelected: SharedFlow<EditorSelected> = _editorSelected.asSharedFlow()

    private val _changeModifiable = MutableSharedFlow<ChangeModifiable>()
    val changeModifiable: SharedFlow<ChangeModifiable> = _changeModifiable.asSharedFlow()

    init {
        scope.launch {
            val bufBucket = mutableListOf<BufferChanged>()
            var caretShadow: IdeaCaretMoved? = null
            var editorShadow: EditorSelected? = null
            val changeModifiableBucket = mutableListOf<ChangeModifiable>()

            suspend fun flush() {
                if (bufBucket.isNotEmpty()) {
                    bufBucket.forEach { _bufferChanges.emit(it) }
                    bufBucket.clear()
                }
                caretShadow?.let {
                    _caretMoved.emit(it)
                    caretShadow = null
                }
                editorShadow?.let {
                    _editorSelected.emit(it)
                    editorShadow = null
                }
                if (changeModifiableBucket.isNotEmpty()) {
                    changeModifiableBucket.forEach { _changeModifiable.emit(it) }
                    changeModifiableBucket.clear()
                }
            }

            fun received(event: IdeaToNvimEvent) {
                when (event) {
                    is IdeaDocumentChanged -> {
                        bufBucket.add(BufferChanged(event, null))
                    }

                    is IdeaCaretMoved -> {
                        if (bufBucket.isNotEmpty()) {
                            bufBucket.last().caretMoved = event
                        } else {
                            caretShadow = event
                        }
                    }

                    is EditorSelected -> editorShadow = event

                    is ChangeModifiable -> changeModifiableBucket.add(event)
                }
            }

            while (isActive) {
                val event = channel.receive()
                received(event)
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

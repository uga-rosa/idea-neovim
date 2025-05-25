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

@Service(Service.Level.APP)
class IdeaToNvimBus(
    private val scope: CoroutineScope,
) : Disposable {
    private val channel = Channel<IdeaToNvimEvent>(Channel.UNLIMITED)

    fun tryEmit(event: IdeaToNvimEvent) {
        channel.trySend(event)
    }

    private val _documentChanges = MutableSharedFlow<IdeaDocumentChanged>()
    val documentChanges: SharedFlow<IdeaDocumentChanged> = _documentChanges.asSharedFlow()

    private val _caretMoved = MutableSharedFlow<IdeaCaretMoved>()
    val caretMoved: SharedFlow<IdeaCaretMoved> = _caretMoved.asSharedFlow()

    private val _editorSelected = MutableSharedFlow<EditorSelected>()
    val editorSelected: SharedFlow<EditorSelected> = _editorSelected.asSharedFlow()

    private val _changeModifiable = MutableSharedFlow<ChangeModifiable>()
    val changeModifiable: SharedFlow<ChangeModifiable> = _changeModifiable.asSharedFlow()

    init {
        scope.launch {
            while (isActive) {
                when (val event = channel.receive()) {
                    is IdeaDocumentChanged -> _documentChanges.emit(event)

                    is IdeaCaretMoved -> _caretMoved.emit(event)

                    is EditorSelected -> _editorSelected.emit(event)

                    is ChangeModifiable -> _changeModifiable.emit(event)
                }
            }
        }
    }

    override fun dispose() {
        scope.cancel()
    }
}

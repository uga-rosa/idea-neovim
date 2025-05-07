package com.ugarosa.neovim.config.idea

import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation
import com.ugarosa.neovim.rpc.event.NeovimModeKind

data class UserKeyMapping(
    val modes: List<NeovimModeKind>,
    val lhs: List<NeovimKeyNotation>,
    val rhs: List<KeyMappingAction>,
)

sealed class KeyMappingAction {
    data class SendToNeovim(val key: NeovimKeyNotation) : KeyMappingAction()

    data class ExecuteIdeaAction(val actionId: String) : KeyMappingAction()
}

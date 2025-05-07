package com.ugarosa.neovim.config.idea

import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation
import com.ugarosa.neovim.mode.NeovimModeKind

data class UserKeyMapping(
    @XCollection(propertyElementName = "modes", elementName = "mode")
    val modes: List<NeovimModeKind>,
    @XCollection(propertyElementName = "lhs", elementName = "key")
    val lhs: List<NeovimKeyNotation>,
    @XCollection(propertyElementName = "rhs", elementName = "action")
    val rhs: List<KeyMappingAction>,
) {
    @Suppress("unused")
    constructor() : this(modes = emptyList(), lhs = emptyList(), rhs = emptyList())
}

@JvmName("joinNeovimKeyNotations")
fun List<NeovimKeyNotation>.join() = joinToString("") { it.toString() }

@JvmName("joinKeyMappingActions")
fun List<KeyMappingAction>.join() = joinToString("") { it.toString() }

sealed class KeyMappingAction {
    data class SendToNeovim(
        @Tag val key: NeovimKeyNotation,
    ) : KeyMappingAction() {
        @Suppress("unused")
        constructor() : this(NeovimKeyNotation())

        override fun toString(): String {
            return key.toString()
        }
    }

    data class ExecuteIdeaAction(
        @Attribute val actionId: String,
    ) : KeyMappingAction() {
        @Suppress("unused")
        constructor() : this("")

        override fun toString(): String {
            return "<Action>($actionId)"
        }
    }

    companion object {
        // This regex will match: <Action>(actionId)
        private val regex = Regex("""<Action>\(([^)]+)\)""")

        fun parseNotations(notations: String): List<KeyMappingAction> {
            return NeovimKeyNotation.parseNotations(notations)
                .map { notation ->
                    val mr = regex.find(notation.toString())
                    if (mr != null) {
                        val actionId = mr.groupValues[1]
                        ExecuteIdeaAction(actionId)
                    } else {
                        SendToNeovim(notation)
                    }
                }
        }
    }
}

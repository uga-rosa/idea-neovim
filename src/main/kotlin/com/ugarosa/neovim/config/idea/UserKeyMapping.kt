package com.ugarosa.neovim.config.idea

import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation
import com.ugarosa.neovim.mode.NeovimModeKind

data class UserKeyMapping(
    @Tag
    val mode: MapMode,
    @XCollection(
        propertyElementName = "lhs",
        elementName = "key",
        elementTypes = [NeovimKeyNotation::class],
    )
    val lhs: List<NeovimKeyNotation>,
    @XCollection(
        propertyElementName = "rhs",
        elementName = "action",
        elementTypes = [KeyMappingAction.SendToNeovim::class, KeyMappingAction.ExecuteIdeaAction::class],
    )
    val rhs: List<KeyMappingAction>,
) {
    @Suppress("unused")
    constructor() : this(mode = MapMode(), lhs = emptyList(), rhs = emptyList())
}

data class MapMode(
    @Attribute val value: String,
) {
    constructor() : this("")

    fun toModeKinds(): List<NeovimModeKind> {
        if (value.isEmpty()) return listOf(NeovimModeKind.NORMAL, NeovimModeKind.VISUAL, NeovimModeKind.SELECT)
        return value.toList().flatMap {
            when (it) {
                'n' -> listOf(NeovimModeKind.NORMAL)
                'v' -> listOf(NeovimModeKind.VISUAL, NeovimModeKind.SELECT)
                'x' -> listOf(NeovimModeKind.VISUAL)
                's' -> listOf(NeovimModeKind.SELECT)
                '!' -> listOf(NeovimModeKind.INSERT, NeovimModeKind.COMMAND)
                'i' -> listOf(NeovimModeKind.INSERT)
                'c' -> listOf(NeovimModeKind.COMMAND)
                else -> emptyList()
            }
        }
    }
}

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

package com.ugarosa.neovim.config.idea

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.XCollection
import com.ugarosa.neovim.keymap.notation.NeovimKeyModifier
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation

@Service(Service.Level.APP)
@State(
    name = "NeovimKeymapSettings",
    storages = [
        Storage(value = "neovim_keymap.xml", roamingType = RoamingType.DEFAULT),
    ],
)
class NeovimKeymapSettingsImpl : NeovimKeymapSettings,
    SerializablePersistentStateComponent<NeovimKeymapSettingsImpl.State>(State()) {
    data class State(
        @JvmField @XCollection val mappings: List<UserKeyMapping> = defaultMappings(),
    )

    override fun getUserKeyMappings(): List<UserKeyMapping> = state.mappings

    override fun setUserKeyMappings(mappings: List<UserKeyMapping>) {
        updateState { it.copy(mappings = mappings) }
    }

    companion object {
        private fun defaultMappings(): List<UserKeyMapping> {
            val esc = NeovimKeyNotation(emptyList(), "Esc")
            val u = NeovimKeyNotation(emptyList(), "u")
            val undo = KeyMappingAction.ExecuteIdeaAction(IdeActions.ACTION_UNDO)
            val ctrlR = NeovimKeyNotation(listOf(NeovimKeyModifier.CTRL), "R")
            val redo = KeyMappingAction.ExecuteIdeaAction(IdeActions.ACTION_REDO)
            return listOf(
                UserKeyMapping(
                    mode = MapMode("i"),
                    lhs = listOf(esc),
                    rhs = listOf(KeyMappingAction.SendToNeovim(esc)),
                ),
                UserKeyMapping(
                    mode = MapMode("n"),
                    lhs = listOf(u),
                    rhs = listOf(undo),
                ),
                UserKeyMapping(
                    mode = MapMode("n"),
                    lhs = listOf(ctrlR),
                    rhs = listOf(redo),
                ),
            )
        }
    }
}

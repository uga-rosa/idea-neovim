package com.ugarosa.neovim.config.idea

import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation
import com.ugarosa.neovim.mode.NeovimModeKind

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
        @JvmField val mappings: List<UserKeyMapping> = defaultMappings(),
    )

    override fun getUserKeyMappings(): List<UserKeyMapping> = state.mappings

    override fun setUserKeyMappings(mappings: List<UserKeyMapping>) {
        updateState { it.copy(mappings = mappings) }
    }

    companion object {
        private fun defaultMappings(): List<UserKeyMapping> {
            val esc = NeovimKeyNotation(emptyList(), "Esc")
            return listOf(
                UserKeyMapping(
                    modes = listOf(NeovimModeKind.INSERT),
                    lhs = listOf(esc),
                    rhs = listOf(KeyMappingAction.SendToNeovim(esc)),
                ),
            )
        }
    }
}

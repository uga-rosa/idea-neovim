package com.ugarosa.neovim.config.idea

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.XCollection
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation

@Service(Service.Level.APP)
@State(
    name = "NeovimKeymapSettings",
    storages = [
        Storage(value = "neovim_keymap.xml", roamingType = RoamingType.DEFAULT),
    ],
)
class NeovimKeymapSettings :
    SerializablePersistentStateComponent<NeovimKeymapSettings.State>(State()) {
    data class State(
        @JvmField @XCollection val mappings: List<UserKeyMapping> = defaultMappings(),
    )

    fun getUserKeyMappings(): List<UserKeyMapping> = state.mappings

    fun setUserKeyMappings(mappings: List<UserKeyMapping>) {
        updateState { it.copy(mappings = mappings) }
    }

    companion object {
        private fun defaultMappings(): List<UserKeyMapping> {
            return buildList {
                fun mapNvim(
                    mode: String,
                    lhsStr: String,
                    rhsStr: String,
                ) {
                    val lhs = NeovimKeyNotation.parseNotations(lhsStr)
                    val rhs =
                        NeovimKeyNotation.parseNotations(rhsStr)
                            .map { KeyMappingAction.SendToNeovim(it) }
                    add(UserKeyMapping(MapMode(mode), lhs, rhs))
                }

                fun mapIdea(
                    mode: String,
                    lhsStr: String,
                    actionId: String,
                ) {
                    val lhs = NeovimKeyNotation.parseNotations(lhsStr)
                    val rhs = listOf(KeyMappingAction.ExecuteIdeaAction(actionId))
                    add(UserKeyMapping(MapMode(mode), lhs, rhs))
                }

                // Insert mode Esc
                mapNvim("i", "<Esc>", "<Esc>")
                // Undo/Redo
                mapIdea("n", "u", IdeActions.ACTION_UNDO)
                mapIdea("n", "<C-r>", IdeActions.ACTION_REDO)
                // Folding
                mapIdea("n", "zo", IdeActions.ACTION_EXPAND_REGION)
                mapIdea("n", "zO", IdeActions.ACTION_EXPAND_REGION_RECURSIVELY)
                mapIdea("n", "zc", IdeActions.ACTION_COLLAPSE_REGION)
                mapIdea("n", "zC", IdeActions.ACTION_COLLAPSE_REGION_RECURSIVELY)
                mapIdea("n", "za", IdeActions.ACTION_EXPAND_COLLAPSE_TOGGLE_REGION)
                mapNvim("n", "zA", "<Nop>")
                mapNvim("n", "zv", "<Nop>")
                mapNvim("n", "zx", "<Nop>")
                mapNvim("n", "zX", "<Nop>")
                mapNvim("n", "zm", "<Nop>")
                mapIdea("n", "zM", IdeActions.ACTION_COLLAPSE_ALL_REGIONS)
                mapIdea("n", "zr", IdeActions.ACTION_EXPAND_ALL_TO_LEVEL_1)
                mapIdea("n", "zR", IdeActions.ACTION_EXPAND_ALL_REGIONS)
            }
        }
    }
}

package com.ugarosa.neovim.extension

import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext
import com.ugarosa.neovim.action.NeovimKeyAction

class NeovimActionPromoter : ActionPromoter {
    // Promote NeovimKeyAction to the top
    override fun promote(
        actions: List<AnAction>,
        context: DataContext,
    ): List<AnAction> {
        return actions.sortedByDescending { action ->
            if (action is NeovimKeyAction) 1 else 0
        }
    }
}

package com.ugarosa.neovim.startup

import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.ugarosa.neovim.handler.NeovimTypedActionHandler
import com.ugarosa.neovim.infra.NeovimRpcClient

fun inject(client: NeovimRpcClient): TypedActionHandler? {
    val typedAction = TypedAction.getInstance()
    val rawHandler = typedAction.rawHandler
    val handlerClass = rawHandler.javaClass

    val field =
        handlerClass.declaredFields
            .firstOrNull { it.type == TypedActionHandler::class.java }
            ?: return null

    field.isAccessible = true
    val original =
        field.get(rawHandler) as? TypedActionHandler
            ?: return null

    val newHandler = NeovimTypedActionHandler(original, client)
    field.set(rawHandler, newHandler)

    return original
}

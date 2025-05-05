package com.ugarosa.neovim.common

import kotlinx.coroutines.CompletableDeferred

fun <T> CompletableDeferred<T>.tryComplete(value: T) {
    if (!isCompleted) {
        complete(value)
    }
}

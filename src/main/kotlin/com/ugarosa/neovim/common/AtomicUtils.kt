package com.ugarosa.neovim.common

import com.jetbrains.rd.util.AtomicReference

// Returns true if the value was different and was updated
fun <T> AtomicReference<T>.setIfDifferent(newValue: T): Boolean {
    val old =
        getAndUpdate { current ->
            if (current == newValue) {
                current
            } else {
                newValue
            }
        }
    return old != newValue
}

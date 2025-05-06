package com.ugarosa.neovim.common

import java.util.concurrent.atomic.AtomicReference

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

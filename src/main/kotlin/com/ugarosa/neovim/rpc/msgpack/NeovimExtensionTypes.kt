package com.ugarosa.neovim.rpc.msgpack

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import org.msgpack.core.MessagePack
import org.msgpack.value.Value

// :h api-definitions
// Special types (msgpack EXT)
data class BufferId(val value: Long)

data class WindowId(val value: Long)

data class TabPageId(val value: Long)

sealed interface DecodeError {
    data object ValueTypeMismatch : DecodeError

    data object NvimTypeMismatch : DecodeError

    data object InvalidData : DecodeError
}

fun Value.asBufferId(): Either<DecodeError, BufferId> = toTypedId(0, ::BufferId)

fun Value.asWindowId(): Either<DecodeError, WindowId> = toTypedId(1, ::WindowId)

fun Value.asTabPageId(): Either<DecodeError, TabPageId> = toTypedId(2, ::TabPageId)

private inline fun <A> Value.toTypedId(
    expectedType: Byte,
    constructor: (Long) -> A,
): Either<DecodeError, A> =
    either {
        ensure(isExtensionValue) { DecodeError.ValueTypeMismatch }
        val ext = asExtensionValue()
        ensure(ext.type == expectedType) { DecodeError.NvimTypeMismatch }
        val value =
            Either.catch { ext.data.toLongFromMsgpack() }
                .mapLeft { DecodeError.InvalidData }
                .bind()
        constructor(value)
    }

private fun ByteArray.toLongFromMsgpack(): Long {
    return MessagePack.newDefaultUnpacker(this).unpackLong()
}

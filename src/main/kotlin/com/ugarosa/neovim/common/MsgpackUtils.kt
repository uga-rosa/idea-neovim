package com.ugarosa.neovim.common

import com.ugarosa.neovim.rpc.function.logger
import org.msgpack.value.Value

fun <T> Value.decode(f: (Value) -> T): T? =
    runCatching {
        f(this)
    }.getOrElse {
        logger.warn("Failed to decode value: $this", it)
        null
    }

fun Value.asStringMap(): Map<String, Any> =
    asMapValue().map()
        .mapKeys { it.key.asStringValue().asString() }
        .mapValues { it.value.asAny() }

fun Value.asAny(): Any =
    when {
        isBooleanValue -> this.asBooleanValue().boolean
        isIntegerValue -> this.asIntegerValue().toInt()
        isFloatValue -> this.asFloatValue().toDouble()
        isStringValue -> this.asStringValue().asString()
        isBinaryValue -> this.asBinaryValue().asByteArray()
        isArrayValue -> this.asArrayValue().list().map { it.asAny() }
        isMapValue ->
            this.asMapValue().map()
                .mapKeys { it.key.asStringValue().asString() }
                .mapValues { it.value.asAny() }

        isExtensionValue -> this.asExtensionValue().data

        isNilValue -> throw IllegalArgumentException("Expected a value, but got nil")
        else -> throw IllegalArgumentException("Unsupported MsgPack type: $this")
    }

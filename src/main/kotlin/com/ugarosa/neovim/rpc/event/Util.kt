package com.ugarosa.neovim.rpc.event

import org.msgpack.value.Value

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

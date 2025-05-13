package com.ugarosa.neovim.rpc.type

/**
 * Response object from Neovim. :h api-types
 */
sealed class NeovimObject() {
    object Nil : NeovimObject()

    data class Bool(val bool: Boolean) : NeovimObject()

    data class Int64(val long: Long) : NeovimObject()

    data class Float64(val double: Double) : NeovimObject()

    data class Str(val str: String) : NeovimObject()

    data class Array(val list: List<NeovimObject>) : NeovimObject()

    data class Dict(val map: Map<String, NeovimObject>) : NeovimObject()

    // EXT type
    data class BufferId(val long: Long) : NeovimObject()

    data class WindowId(val long: Long) : NeovimObject()

    data class TabpageId(val long: Long) : NeovimObject()

    // Utility methods
    fun asNil(): Nil = this as Nil

    fun asBool(): Bool = this as Bool

    fun asInt64(): Int64 = this as Int64

    fun asFloat64(): Float64 = this as Float64

    fun asStr(): Str = this as Str

    fun asArray(): Array = this as Array

    fun asDict(): Dict = this as Dict

    fun asStringMap(): Map<String, Any> =
        when (this) {
            is Dict -> map.mapValues { it.value.asAny() }
            else -> error("Cannot convert $this to StringMap")
        }

    fun asBufferId(): BufferId =
        when (this) {
            is BufferId -> this
            // My custom event sends bufferId as just an Int64
            is Int64 -> BufferId(long)
            else -> error("Cannot convert $this to BufferId")
        }

    fun asWindowId(): WindowId = this as WindowId

    fun asTabpageId(): TabpageId = this as TabpageId

    fun asAny(): Any {
        return when (this) {
            is Nil -> error("Nil cannot be converted to Any")
            is Bool -> bool
            is Int64 -> long
            is Float64 -> double
            is Str -> str
            is Array -> list.map { it.asAny() }
            is Dict -> map.mapValues { it.value.asAny() }
            is BufferId -> long
            is WindowId -> long
            is TabpageId -> long
        }
    }
}

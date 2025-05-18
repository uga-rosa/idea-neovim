package com.ugarosa.neovim.rpc.transport

import com.ugarosa.neovim.buffer.BufferId
import com.ugarosa.neovim.rpc.type.TabpageId
import com.ugarosa.neovim.window.WindowId

/**
 * Response object from Neovim. :h api-types
 */
@Suppress("unused")
sealed class NeovimObject() {
    data object Nil : NeovimObject()

    data class Bool(val bool: Boolean) : NeovimObject() {
        override fun toString(): String {
            return "$bool"
        }
    }

    data class Int64(val long: Long) : NeovimObject() {
        override fun toString(): String {
            return "$long"
        }
    }

    data class Float64(val double: Double) : NeovimObject() {
        override fun toString(): String {
            return "$double"
        }
    }

    data class Str(val str: String) : NeovimObject() {
        override fun toString(): String {
            return "\"$str\""
        }
    }

    data class Array(val list: List<NeovimObject>) : NeovimObject() {
        override fun toString(): String {
            return "$list"
        }
    }

    data class Dict(val map: Map<String, NeovimObject>) : NeovimObject() {
        override fun toString(): String {
            return "$map"
        }
    }

    // EXT type
    data class Buffer(val long: Long) : NeovimObject() {
        override fun toString(): String {
            return "Buffer($long)"
        }
    }

    data class Window(val long: Long) : NeovimObject() {
        override fun toString(): String {
            return "Window($long)"
        }
    }

    data class Tabpage(val long: Long) : NeovimObject() {
        override fun toString(): String {
            return "Tabpage($long)"
        }
    }

    // Utility methods
    fun asNull(): Nothing? = (this as Nil).let { null }

    fun asBool(): Boolean = (this as Bool).bool

    fun asInt(): Int = (this as Int64).long.toInt()

    fun asLong(): Long = (this as Int64).long

    fun asFloat(): Float = (this as Float64).double.toFloat()

    fun asDouble(): Double = (this as Float64).double

    fun asString(): String = (this as Str).str

    fun asArray(): List<NeovimObject> = (this as Array).list

    fun asDict(): Map<String, NeovimObject> = (this as Dict).map

    fun asStringMap(): Map<String, Any> = asDict().mapValues { it.value.asAny() }

    fun asBufferId(): BufferId =
        when (this) {
            is Buffer -> BufferId(long)
            // My custom event sends bufferId as just an Int64
            is Int64 -> BufferId(long)
            else -> error("Cannot convert $this to BufferId")
        }

    fun asWindowId(): WindowId =
        when (this) {
            is Window -> WindowId(long)
            // My custom event sends windowId as just an Int64
            is Int64 -> WindowId(long)
            else -> error("Cannot convert $this to WindowId")
        }

    fun asTabpageId(): TabpageId = TabpageId((this as Tabpage).long)

    fun asAny(): Any {
        return when (this) {
            is Nil -> error("Nil cannot be converted to Any")
            is Bool -> bool
            is Int64 -> long
            is Float64 -> double
            is Str -> str
            is Array -> list.map { it.asAny() }
            is Dict -> map.mapValues { it.value.asAny() }
            is Buffer -> long
            is Window -> long
            is Tabpage -> long
        }
    }
}

package com.maddy.formvalidator

typealias Subscribe = (value: FormValue<Any>?) -> Unit
typealias Unsubscribe = () -> Unit

typealias OnFocusListener = (Any?, Boolean) -> Unit
typealias OnChangeListener = (Any?) -> Unit

open class FormInput(
    val valueProvider: () -> Any?,
    val setError: (String?) -> Unit,
    val setValue: (FormValue<Any>) -> Unit
) {
    open fun setOnFocusListener(listener: OnFocusListener): Unsubscribe {
        return {}
    }

    open fun setOnChangeListener(listener: OnChangeListener): Unsubscribe {
        return {}
    }
}


class FormValue<V : Any>(val value: V? = null) {

    fun asAny(): Any? = value

    fun asInt(default: Int = 0): Int {
        return asIntOrNull() ?: default
    }

    fun asIntOrNull(): Int? {
        return if (value is Int) value
        else null
    }

    fun asFloat(default: Float = 0F): Float {
        return asFloatOrNull() ?: default
    }

    fun asFloatOrNull(): Float? {
        return if (value is Float) value
        else null
    }

    fun asDouble(default: Double = 0.0): Double {
        return asDoubleOrNull() ?: default
    }

    fun asDoubleOrNull(): Double? {
        return if (value is Double) value
        else null
    }

    fun asBoolean(default: Boolean = false): Boolean {
        return asBooleanNull() ?: default
    }

    fun asBooleanNull(): Boolean? {
        return when (value) {
            is Boolean -> value
            is String -> {
                when (value) {
                    "true" -> true
                    "false" -> false
                    else -> null
                }
            }
            else -> null
        }
    }

    fun asLong(default: Long = 0L): Long {
        return asLongOrNull() ?: default
    }

    fun asLongOrNull(): Long? {
        return if (value is Long) value
        else null
    }

    fun asString(default: String = ""): String {
        return asStringOrNull() ?: default
    }

    fun asStringOrNull(): String? {
        return if (value is String) value
        else null
    }

    inline fun <reified C> asCollection(default: Collection<C> = emptyList()): Collection<C> {
        return asCollectionOrNull() ?: default
    }

    inline fun <reified C> asCollectionOrNull(): Collection<C>? {
        return if (value is Collection<*>) value.checkItemsAre()
        else null
    }

    inline fun <reified C> asArray(default: Array<C> = emptyArray()): Array<C> {
        return asArrayOrNull() ?: default
    }

    inline fun <reified C> asArrayOrNull(): Array<C>? {
        return if (value is Array<*>) value.checkItemsAre()
        else null
    }

    override fun toString(): String {
        return this.value?.toString().orEmpty()
    }
}

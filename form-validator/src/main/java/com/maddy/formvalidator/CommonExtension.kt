package com.maddy.formvalidator

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Collection<*>.checkItemsAre() =
    if (all { it is T })
        this as Collection<T>
    else null

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Array<*>.checkItemsAre() =
    if (all { it is T })
        this as Array<T>
    else null

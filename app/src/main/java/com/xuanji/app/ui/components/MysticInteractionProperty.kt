package com.xuanji.app.ui.components

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Bind UI fields to one immutable session reducer, rather than maintaining a second local copy. */
internal class MysticInteractionProperty<T>(
    private val read: () -> T,
    private val write: (T) -> Unit
) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = read()
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = write(value)
}

/*
 * Copyright 2019 Russell Wolf
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.russhwolf.settings.serialization

import com.russhwolf.settings.Settings
import kotlinx.serialization.AbstractSerialFormat
import kotlinx.serialization.CompositeDecoder
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.KSerializer
import kotlinx.serialization.NamedValueDecoder
import kotlinx.serialization.NamedValueEncoder
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.UpdateMode
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> Settings.serialDelegate(serializer: KSerializer<T>, key: String? = null): ReadWriteProperty<Any?, T> =
    SerializedSettingsDelegate(this, serializer, key)

private class SerializedSettingsDelegate<T>(
    private val settings: Settings,
    private val serializer: KSerializer<T>,
    key: String?
) : ReadWriteProperty<Any?, T> {

    private val decoder = key?.let { key -> SettingsDecoder(settings, key) }
    private val encoder = key?.let { key -> SettingsEncoder(settings, key) }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val decoder = decoder ?: SettingsDecoder(settings, property.name)
        return serializer.deserialize(decoder)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val encoder = encoder ?: SettingsEncoder(settings, property.name)
        serializer.serialize(encoder, value)
    }
}

fun Settings.serialFormat(context: SerialModule = EmptyModule): SettingsSerialFormat =
    SettingsSerialFormatImpl(this, context)

interface SettingsSerialFormat {
    fun <T> serializeToSettings(serializer: KSerializer<T>, key: String, obj: T)
    fun <T> deserializeFromSettings(serializer: KSerializer<T>, key: String): T
}

private class SettingsSerialFormatImpl(
    private val settings: Settings,
    context: SerialModule = EmptyModule
) : AbstractSerialFormat(context), SettingsSerialFormat {

    override fun <T> serializeToSettings(serializer: KSerializer<T>, key: String, obj: T) =
        serializer.serialize(SettingsEncoder(settings, key), obj)

    override fun <T> deserializeFromSettings(serializer: KSerializer<T>, key: String): T =
        serializer.deserialize(SettingsDecoder(settings, key))
}

private class SettingsEncoder(private val settings: Settings, key: String) : NamedValueEncoder(rootName = key) {

    override fun encodeTaggedNull(tag: String) = settings.putBoolean("$tag?", false)
    override fun encodeTaggedNotNullMark(tag: String) = settings.putBoolean("$tag?", true)

    override fun encodeTaggedBoolean(tag: String, value: Boolean) = settings.putBoolean(tag, value)
    override fun encodeTaggedByte(tag: String, value: Byte) = settings.putInt(tag, value.toInt())
    override fun encodeTaggedChar(tag: String, value: Char) = settings.putInt(tag, value.toInt())
    override fun encodeTaggedDouble(tag: String, value: Double) = settings.putDouble(tag, value)
    override fun encodeTaggedEnum(tag: String, enumDescription: SerialDescriptor, ordinal: Int) =
        settings.putInt(tag, ordinal)

    override fun encodeTaggedFloat(tag: String, value: Float) = settings.putFloat(tag, value)
    override fun encodeTaggedInt(tag: String, value: Int) = settings.putInt(tag, value)
    override fun encodeTaggedLong(tag: String, value: Long) = settings.putLong(tag, value)
    override fun encodeTaggedShort(tag: String, value: Short) = settings.putInt(tag, value.toInt())
    override fun encodeTaggedString(tag: String, value: String) = settings.putString(tag, value)
    override fun encodeTaggedUnit(tag: String) = settings.putBoolean(tag, true)
}

private class SettingsDecoder(private val settings: Settings, key: String) : NamedValueDecoder(rootName = key) {
    override val updateMode: UpdateMode = UpdateMode.OVERWRITE

    // READ_ALL doesn't work on native and we have no natural index, so we count manually instead (similar to Json)
    private var position = 0

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        // Reset on start to ensure we can decode multiple times and still get correct indices
        position = 0
        return super.beginStructure(desc, *typeParams)
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        if (position < desc.elementsCount) {
            return position++
        }
        return READ_DONE
    }

    override fun decodeTaggedNull(tag: String): Nothing? = null
    override fun decodeTaggedNotNullMark(tag: String) = settings.getBoolean("$tag?", false)

    override fun decodeTaggedBoolean(tag: String): Boolean = settings.getBoolean(tag)
    override fun decodeTaggedByte(tag: String): Byte = settings.getInt(tag).toByte()
    override fun decodeTaggedChar(tag: String): Char = settings.getInt(tag).toChar()
    override fun decodeTaggedDouble(tag: String): Double = settings.getDouble(tag)
    override fun decodeTaggedEnum(tag: String, enumDescription: SerialDescriptor): Int = settings.getInt(tag)
    override fun decodeTaggedFloat(tag: String): Float = settings.getFloat(tag)
    override fun decodeTaggedInt(tag: String): Int = settings.getInt(tag)
    override fun decodeTaggedLong(tag: String): Long = settings.getLong(tag)
    override fun decodeTaggedShort(tag: String): Short = settings.getInt(tag).toShort()
    override fun decodeTaggedString(tag: String): String = settings.getString(tag)
    override fun decodeTaggedUnit(tag: String) = Unit
}

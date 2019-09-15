package com.russhwolf.settings.coroutines

import com.russhwolf.settings.ExperimentalListener
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals

@UseExperimental(ExperimentalListener::class, ExperimentalCoroutinesApi::class)
abstract class CoroutineExtensionsTest {

    abstract val settings: ObservableSettings

    private fun <T> flowTest(
        flowBuilder: ObservableSettings.(String, T) -> Flow<T>,
        setter: Settings.(String, T) -> Unit,
        defaultValue: T,
        firstValue: T,
        secondValue: T
    ) = suspendTest {
        val flow = settings.flowBuilder("foo", defaultValue)

        val output = mutableListOf<T>()

        withTimeoutOrNull(1000) {
            flow
                .onStart {
                    launch {
                        settings.setter("foo", firstValue)
                        delay(1)
                        settings.setter("foo", firstValue)
                        delay(1)
                        settings.setter("bar", firstValue)
                        delay(1)
                        settings.setter("foo", secondValue)
                        delay(1)
                        settings.remove("foo")
                    }
                }
                .take(3)
                .collect { output.add(it) }
        }

        assertEquals(listOf(firstValue, secondValue, defaultValue), output)
    }

    private inline fun <reified T : Any> nullableFlowTest(
        crossinline flowBuilder: ObservableSettings.(String) -> Flow<T?>,
        crossinline setter: Settings.(String, T) -> Unit,
        firstValue: T,
        secondValue: T
    ) = flowTest(
        flowBuilder = { key, _ -> flowBuilder(key) },
        setter = { key, value -> if (value != null) setter(key, value) else remove(key) },
        defaultValue = null,
        firstValue = firstValue,
        secondValue = secondValue
    )

    @Test
    fun intFlowTest() = flowTest(
        flowBuilder = ObservableSettings::intFlow,
        setter = Settings::putInt,
        defaultValue = 0,
        firstValue = 3,
        secondValue = 8
    )

    @Test
    fun longFlowTest() = flowTest(
        flowBuilder = ObservableSettings::longFlow,
        setter = Settings::putLong,
        defaultValue = 0L,
        firstValue = 3L,
        secondValue = 8L
    )

    @Test
    fun stringFlowTest() = flowTest(
        flowBuilder = ObservableSettings::stringFlow,
        setter = Settings::putString,
        defaultValue = "",
        firstValue = "bar",
        secondValue = "baz"
    )

    @Test
    fun floatFlowTest() = flowTest(
        flowBuilder = ObservableSettings::floatFlow,
        setter = Settings::putFloat,
        defaultValue = 0f,
        firstValue = 3f,
        secondValue = 8f
    )

    @Test
    fun doubleFlowTest() = flowTest(
        flowBuilder = ObservableSettings::doubleFlow,
        setter = Settings::putDouble,
        defaultValue = 0.0,
        firstValue = 3.0,
        secondValue = 8.0
    )

    @Test
    fun booleanFlowTest() = flowTest(
        flowBuilder = ObservableSettings::booleanFlow,
        setter = Settings::putBoolean,
        defaultValue = false,
        firstValue = true,
        secondValue = false
    )

    @Test
    fun intOrNullFlowTest() = nullableFlowTest(
        flowBuilder = ObservableSettings::intOrNullFlow,
        setter = Settings::putInt,
        firstValue = 3,
        secondValue = 8
    )

    @Test
    fun longOrNullFlowTest() = nullableFlowTest(
        flowBuilder = ObservableSettings::longOrNullFlow,
        setter = Settings::putLong,
        firstValue = 3L,
        secondValue = 8L
    )

    @Test
    fun stringOrNullFlowTest() = nullableFlowTest(
        flowBuilder = ObservableSettings::stringOrNullFlow,
        setter = Settings::putString,
        firstValue = "bar",
        secondValue = "baz"
    )

    @Test
    fun floatOrNullFlowTest() = nullableFlowTest(
        flowBuilder = ObservableSettings::floatOrNullFlow,
        setter = Settings::putFloat,
        firstValue = 3f,
        secondValue = 8f
    )

    @Test
    fun doubleOrNullFlowTest() = nullableFlowTest(
        flowBuilder = ObservableSettings::doubleOrNullFlow,
        setter = Settings::putDouble,
        firstValue = 3.0,
        secondValue = 8.0
    )

    @Test
    fun booleanOrNullFlowTest() = nullableFlowTest(
        flowBuilder = ObservableSettings::booleanOrNullFlow,
        setter = Settings::putBoolean,
        firstValue = true,
        secondValue = false
    )
}

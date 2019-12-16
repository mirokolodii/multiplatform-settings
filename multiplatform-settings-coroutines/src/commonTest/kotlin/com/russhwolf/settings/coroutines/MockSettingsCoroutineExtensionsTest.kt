package com.russhwolf.settings.coroutines

import com.russhwolf.settings.ExperimentalListener
import com.russhwolf.settings.MockSettings
import com.russhwolf.settings.ObservableSettings

class MockSettingsCoroutineExtensionsTest : CoroutineExtensionsTest() {
    @UseExperimental(ExperimentalListener::class)
    override val settings: ObservableSettings = MockSettings()
}

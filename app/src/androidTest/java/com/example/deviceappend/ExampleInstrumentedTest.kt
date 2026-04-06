package com.example.deviceappend

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Contexto de la app bajo prueba
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.deviceappend", appContext.packageName)
    }
}
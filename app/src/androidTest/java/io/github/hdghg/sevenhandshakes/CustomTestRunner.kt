package io.github.hdghg.sevenhandshakes

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * The runner named by testInstrumentationRunner in app/build.gradle.kts.
 * It runs device tests with Hilt's test application in place of [MainApplication],
 * so tests can swap in their own dependencies.
 */
class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(classLoader: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(classLoader, HiltTestApplication::class.java.name, context)
}

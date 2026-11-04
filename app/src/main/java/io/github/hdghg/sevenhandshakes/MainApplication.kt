package io.github.hdghg.sevenhandshakes

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application that sets up Timber.
 */
@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.i("App started: %s %s", BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE)
    }

    companion object {
        const val LOG_TAG = "SevenHandshakes"
    }
}

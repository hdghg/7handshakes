package io.github.hdghg.sevenhandshakes

import android.app.Application
import io.github.hdghg.sevenhandshakes.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

/**
 * Application that sets up Timber and the Koin dependency graph.
 */
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        startKoin {
            androidContext(this@MainApplication)
            modules(appModules)
        }
        Timber.i("App started: %s %s", BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE)
    }

    companion object {
        const val LOG_TAG = "SevenHandshakes"
    }
}

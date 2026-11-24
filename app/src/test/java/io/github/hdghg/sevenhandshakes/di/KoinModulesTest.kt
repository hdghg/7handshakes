package io.github.hdghg.sevenhandshakes.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.github.hdghg.sevenhandshakes.data.share.ShareImportStore
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Checks that every class in [appModules] can get each of its constructor parameters,
 * the check Hilt used to do at compile time.
 */
@OptIn(KoinExperimentalAPI::class)
class KoinModulesTest {
    @Test
    fun appModules_provideEveryConstructorParameter() {
        module { includes(appModules) }.verify(
            extraTypes = listOf(
                // Given by Android: androidContext() and the navigation arguments of a screen.
                Context::class,
                SavedStateHandle::class,
                // ShareImporter's primary constructor is for tests; the app uses the one taking RoomShareImportStore.
                ShareImportStore::class,
                Function1::class,
            ),
        )
    }
}

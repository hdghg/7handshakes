package io.github.hdghg.sevenhandshakes.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Names the [CoroutineDispatcher] for blocking IO. */
val IoDispatcher: Qualifier = named("IoDispatcher")

/** Names the [CoroutineDispatcher] for CPU-bound work. */
val DefaultDispatcher: Qualifier = named("DefaultDispatcher")

/** Names the [CoroutineScope] that lives as long as the app. */
val ApplicationScope: Qualifier = named("ApplicationScope")

val coroutinesModule = module {
    factory<CoroutineDispatcher>(IoDispatcher) { Dispatchers.IO }
    factory<CoroutineDispatcher>(DefaultDispatcher) { Dispatchers.Default }
    single<CoroutineScope>(ApplicationScope) {
        CoroutineScope(SupervisorJob() + get<CoroutineDispatcher>(DefaultDispatcher))
    }
}

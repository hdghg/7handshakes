package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.gratitude.GratitudeDao
import io.github.hdghg.sevenhandshakes.data.source.gratitude.GratitudeRecord
import io.github.hdghg.sevenhandshakes.di.ApplicationScope
import io.github.hdghg.sevenhandshakes.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultGratitudeRepository
    @Inject
    constructor(
        private val gratitudeDao: GratitudeDao,
        @param:DefaultDispatcher private val dispatcher: CoroutineDispatcher,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) : GratitudeRepository {
        override fun getGratitudesStream(): Flow<List<GratitudeRecord>> = gratitudeDao.observeAll()
    }

package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDao
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import io.github.hdghg.sevenhandshakes.di.ApplicationScope
import io.github.hdghg.sevenhandshakes.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultReviewRepository
    @Inject
    constructor(
        private val gratitudeDao: ReviewDao,
        @param:DefaultDispatcher private val dispatcher: CoroutineDispatcher,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) : ReviewRepository {
        override fun getGratitudesStream(): Flow<List<ReviewRecord>> = gratitudeDao.observeAll()
    }

package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.gratitude.GratitudeRecord
import kotlinx.coroutines.flow.Flow

interface GratitudeRepository {
    fun getGratitudesStream(): Flow<List<GratitudeRecord>>
}

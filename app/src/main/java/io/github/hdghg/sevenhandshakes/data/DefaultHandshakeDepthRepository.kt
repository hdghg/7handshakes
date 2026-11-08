package io.github.hdghg.sevenhandshakes.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultHandshakeDepthRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : HandshakeDepthRepository {
        override fun getHandshakeDepthStream(): Flow<Int> =
            dataStore.data
                .map { (it[HANDSHAKE_DEPTH] ?: DEFAULT_HANDSHAKE_DEPTH).coerceIn(0, MAX_HANDSHAKE_DEPTH) }
                .distinctUntilChanged()

        override suspend fun setHandshakeDepth(depth: Int) {
            val clamped = depth.coerceIn(0, MAX_HANDSHAKE_DEPTH)
            dataStore.edit { it[HANDSHAKE_DEPTH] = clamped }
            Timber.d("Handshake depth set to %d", clamped)
        }

        private companion object {
            val HANDSHAKE_DEPTH = intPreferencesKey("handshake_depth")
        }
    }

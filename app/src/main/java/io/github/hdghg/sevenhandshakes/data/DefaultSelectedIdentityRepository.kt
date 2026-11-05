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
class DefaultSelectedIdentityRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : SelectedIdentityRepository {
        override fun getSelectedIdentityIdStream(): Flow<Int?> =
            dataStore.data
                .map { it[SELECTED_IDENTITY_ID] }
                .distinctUntilChanged()

        override suspend fun selectIdentity(id: Int) {
            dataStore.edit { it[SELECTED_IDENTITY_ID] = id }
            Timber.d("Identity %d selected", id)
        }

        private companion object {
            val SELECTED_IDENTITY_ID = intPreferencesKey("selected_identity_id")
        }
    }

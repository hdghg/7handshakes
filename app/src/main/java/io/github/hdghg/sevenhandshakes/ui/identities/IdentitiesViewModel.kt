package io.github.hdghg.sevenhandshakes.ui.identities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.data.IdentityRepository
import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityWithContact
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class IdentitiesUiState(
    val items: List<IdentityWithContact> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class IdentitiesViewModel
    @Inject
    constructor(
        identityRepository: IdentityRepository,
    ) : ViewModel() {
        val uiState: StateFlow<IdentitiesUiState> =
            identityRepository
                .getIdentitiesStream()
                .map { IdentitiesUiState(items = it) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = IdentitiesUiState(isLoading = true),
                )
    }

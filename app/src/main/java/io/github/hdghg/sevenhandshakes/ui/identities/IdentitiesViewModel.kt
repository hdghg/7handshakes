package io.github.hdghg.sevenhandshakes.ui.identities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.data.IdentityRepository
import io.github.hdghg.sevenhandshakes.data.SelectedIdentityRepository
import io.github.hdghg.sevenhandshakes.data.selectedIdentityOf
import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityWithContact
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IdentitiesUiState(
    val selected: IdentityWithContact? = null,
    val others: List<IdentityWithContact> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class IdentitiesViewModel
    @Inject
    constructor(
        identityRepository: IdentityRepository,
        private val selectedIdentityRepository: SelectedIdentityRepository,
    ) : ViewModel() {
        val uiState: StateFlow<IdentitiesUiState> =
            combine(
                identityRepository.getIdentitiesStream(),
                selectedIdentityRepository.getSelectedIdentityIdStream(),
            ) { identities, selectedId ->
                val selected = selectedIdentityOf(identities, selectedId)
                IdentitiesUiState(
                    selected = selected,
                    others = identities.filter { it !== selected },
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = IdentitiesUiState(isLoading = true),
            )

        fun selectIdentity(id: Int) {
            viewModelScope.launch { selectedIdentityRepository.selectIdentity(id) }
        }
    }

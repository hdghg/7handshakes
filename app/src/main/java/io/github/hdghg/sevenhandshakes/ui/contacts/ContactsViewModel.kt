package io.github.hdghg.sevenhandshakes.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ContactsUiState(
    val items: List<ContactRecord> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class ContactsViewModel
    @Inject
    constructor(
        contactRepository: ContactRepository,
    ) : ViewModel() {
        val uiState: StateFlow<ContactsUiState> =
            contactRepository
                .getContactsStream()
                .map { ContactsUiState(items = it) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = ContactsUiState(isLoading = true),
                )
    }

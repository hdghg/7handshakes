package io.github.hdghg.sevenhandshakes.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ContactsUiState(
    val items: List<ContactRecord> = emptyList(),
    val isLoading: Boolean = false,
    /** When false, contacts without a name or email are left out of [items]. */
    val showUnknown: Boolean = false,
    /** True when contacts exist but all of them are hidden as unknown. */
    val allHidden: Boolean = false,
)

@HiltViewModel
class ContactsViewModel
    @Inject
    constructor(
        contactRepository: ContactRepository,
    ) : ViewModel() {
        private val showUnknown = MutableStateFlow(false)

        val uiState: StateFlow<ContactsUiState> =
            combine(contactRepository.getContactsStream(), showUnknown) { contacts, showUnknown ->
                val visible = if (showUnknown) contacts else contacts.filter { it.displayName != null }
                ContactsUiState(
                    items = visible,
                    showUnknown = showUnknown,
                    allHidden = visible.isEmpty() && contacts.isNotEmpty(),
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ContactsUiState(isLoading = true),
            )

        fun setShowUnknown(value: Boolean) {
            showUnknown.value = value
        }
    }

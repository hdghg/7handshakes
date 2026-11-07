package io.github.hdghg.sevenhandshakes.ui.contactdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ContactDetailUiState(
    val contact: ContactRecord? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class ContactDetailViewModel
    @Inject
    constructor(
        contactRepository: ContactRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val contactId: Int = checkNotNull(savedStateHandle[MyAppDestinationsArgs.CONTACT_ID_ARG])

        val uiState: StateFlow<ContactDetailUiState> =
            contactRepository
                .getContactStream(contactId)
                .map { ContactDetailUiState(contact = it) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = ContactDetailUiState(isLoading = true),
                )
    }

package io.github.hdghg.sevenhandshakes.ui.contactlink

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.ui.BackTopAppBar
import io.github.hdghg.sevenhandshakes.ui.Field
import io.github.hdghg.sevenhandshakes.ui.displayNameOrUnknown

/**
 * Shown when a contact link is opened: the contact it holds, and a button to save it.
 * Once the contact is saved, or if the link has nothing to add to a contact the app has,
 * [openContact] is called with its id.
 */
@Composable
fun ContactLinkScreen(
    onBack: () -> Unit,
    openContact: (contactId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactLinkViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.savedContactId) {
        uiState.savedContactId?.let(openContact)
    }
    ContactLinkContent(
        // A contact with nothing left to save is shown by its own screen, opened above.
        contact = uiState.contact.takeIf { uiState.savedContactId == null },
        isLinkInvalid = uiState.isLinkInvalid,
        isSaving = uiState.isSaving,
        onSave = viewModel::save,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun ContactLinkContent(
    contact: ContactPreview?,
    isLinkInvalid: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BackTopAppBar(title = stringResource(id = R.string.contact_details), onBack = onBack) },
    ) { paddingValues ->
        if (contact == null) {
            if (isLinkInvalid) {
                Text(
                    text = stringResource(id = R.string.contact_link_invalid),
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(all = margin),
                )
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(all = margin),
        ) {
            Field(label = stringResource(id = R.string.contact_details)) {
                Text(
                    // As everywhere: the name, else the email, else "Unknown contact".
                    text = displayNameOrUnknown(contact.name ?: contact.email),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            contact.email?.let { email ->
                Field(label = stringResource(id = R.string.contact_email)) {
                    SelectionContainer {
                        Text(text = email, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Field(label = stringResource(id = R.string.contact_public_key)) {
                SelectionContainer {
                    Text(text = contact.publicKey, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.menu_save_contact))
            }
        }
    }
}

@Preview
@Composable
private fun ContactLinkScreenPreview() {
    Surface {
        ContactLinkContent(
            contact = ContactPreview(
                name = "Alice",
                email = "alice@example.com",
                publicKey = "MCowBQYDK2VwAyEAabcdefghijklmnopqrstuvwxyz0123456789ABCD",
            ),
            isLinkInvalid = false,
            isSaving = false,
            onSave = {},
            onBack = {},
        )
    }
}

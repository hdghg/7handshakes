package io.github.hdghg.sevenhandshakes.ui.contacts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.ui.ContactsTopAppBar

@Composable
fun ContactsScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ContactsContent(
        contacts = uiState.items,
        isLoading = uiState.isLoading,
        openDrawer = openDrawer,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun ContactsContent(
    contacts: List<ContactRecord>,
    isLoading: Boolean,
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ContactsTopAppBar(openDrawer) },
    ) { paddingValues ->
        if (contacts.isEmpty() && !isLoading) {
            Text(
                text = stringResource(id = R.string.contacts_empty),
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(all = dimensionResource(id = R.dimen.horizontal_margin)),
            )
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(contacts, key = { it.id }) { contact ->
                    ContactItem(contact)
                }
            }
        }
    }
}

@Composable
private fun ContactItem(contact: ContactRecord) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    val textColor = if (contact.isIgnored) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = margin, vertical = margin / 2),
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            modifier = Modifier.padding(end = margin),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name ?: stringResource(id = R.string.contact_unnamed),
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
            )
            Text(
                text = contact.publicKey,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (contact.isFavorite) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = stringResource(id = R.string.contact_favorite),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Preview
@Composable
fun ContactsScreenPreview() {
    Surface {
        ContactsContent(
            contacts = listOf(
                ContactRecord(id = 1, publicKey = "MCowBQYDK2VwAyEAabc", name = "Alice", isFavorite = true, isIgnored = false),
                ContactRecord(id = 2, publicKey = "MCowBQYDK2VwAyEAdef", name = null, isFavorite = false, isIgnored = false),
                ContactRecord(id = 3, publicKey = "MCowBQYDK2VwAyEAghi", name = "Bob", isFavorite = false, isIgnored = true),
            ),
            isLoading = false,
            openDrawer = {},
        )
    }
}

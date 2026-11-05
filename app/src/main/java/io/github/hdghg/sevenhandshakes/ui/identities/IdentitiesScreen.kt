package io.github.hdghg.sevenhandshakes.ui.identities

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
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
import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityRecord
import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityWithContact
import io.github.hdghg.sevenhandshakes.ui.IdentitiesTopAppBar

@Composable
fun IdentitiesScreen(
    openDrawer: () -> Unit,
    addIdentity: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IdentitiesViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    IdentitiesContent(
        identities = uiState.items,
        isLoading = uiState.isLoading,
        openDrawer = openDrawer,
        addIdentity = addIdentity,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun IdentitiesContent(
    identities: List<IdentityWithContact>,
    isLoading: Boolean,
    openDrawer: () -> Unit,
    addIdentity: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { IdentitiesTopAppBar(openDrawer) },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = addIdentity) {
                Icon(Icons.Filled.Add, stringResource(id = R.string.add_identity))
            }
        },
    ) { paddingValues ->
        if (identities.isEmpty() && !isLoading) {
            Text(
                text = stringResource(id = R.string.identities_empty),
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(all = dimensionResource(id = R.dimen.horizontal_margin)),
            )
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(identities, key = { it.identity.id }) { item ->
                    IdentityItem(item)
                }
            }
        }
    }
}

/**
 * Shows the linked contact's name and public key. The private key is never displayed.
 */
@Composable
private fun IdentityItem(item: IdentityWithContact) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = margin, vertical = margin / 2),
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = null,
            modifier = Modifier.padding(end = margin),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.contact.name ?: stringResource(id = R.string.identity_unnamed),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = item.contact.publicKey,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
fun IdentitiesScreenPreview() {
    Surface {
        IdentitiesContent(
            identities = listOf(
                IdentityWithContact(
                    identity = IdentityRecord(id = 1, privateKey = "priv1", contactId = 1),
                    contact = ContactRecord(id = 1, publicKey = "MCowBQYDK2VwAyEAabc", name = "Me", isFavorite = false, isIgnored = false),
                ),
                IdentityWithContact(
                    identity = IdentityRecord(id = 2, privateKey = "priv2", contactId = 2),
                    contact = ContactRecord(id = 2, publicKey = "MCowBQYDK2VwAyEAdef", name = null, isFavorite = false, isIgnored = false),
                ),
            ),
            isLoading = false,
            openDrawer = {},
            addIdentity = {},
        )
    }
}

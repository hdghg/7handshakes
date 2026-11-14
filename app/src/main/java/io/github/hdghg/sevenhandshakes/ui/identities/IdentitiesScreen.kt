package io.github.hdghg.sevenhandshakes.ui.identities

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import io.github.hdghg.sevenhandshakes.util.DeviceUnlock
import kotlinx.coroutines.launch

@Composable
fun IdentitiesScreen(
    openDrawer: () -> Unit,
    addIdentity: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IdentitiesViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Not saved across recreation on purpose: the private key is shown again only after a new unlock.
    var sharedIdentity by remember { mutableStateOf<IdentityWithContact?>(null) }
    val unlockTitle = stringResource(id = R.string.identity_unlock_title)
    val unlockSubtitle = stringResource(id = R.string.identity_unlock_subtitle)
    val noScreenLock = stringResource(id = R.string.identity_no_screen_lock)
    IdentitiesContent(
        selected = uiState.selected,
        others = uiState.others,
        isLoading = uiState.isLoading,
        openDrawer = openDrawer,
        addIdentity = addIdentity,
        selectIdentity = viewModel::selectIdentity,
        sharePrivateKey = { identity ->
            DeviceUnlock.confirm(
                context = context,
                title = unlockTitle,
                subtitle = unlockSubtitle,
                onSuccess = { sharedIdentity = identity },
                onUnavailable = { scope.launch { snackbarHostState.showSnackbar(noScreenLock) } },
            )
        },
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )

    sharedIdentity?.let { identity ->
        PrivateKeyQrDialog(
            name = identity.contact.name ?: stringResource(id = R.string.identity_unnamed),
            privateKey = identity.identity.privateKey,
            onDismiss = { sharedIdentity = null },
        )
    }
}

@Composable
private fun IdentitiesContent(
    selected: IdentityWithContact?,
    others: List<IdentityWithContact>,
    isLoading: Boolean,
    openDrawer: () -> Unit,
    addIdentity: () -> Unit,
    selectIdentity: (Int) -> Unit,
    sharePrivateKey: (IdentityWithContact) -> Unit,
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
        if (selected == null && !isLoading) {
            Text(
                text = stringResource(id = R.string.identities_empty),
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(all = dimensionResource(id = R.dimen.horizontal_margin)),
            )
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                if (selected != null) {
                    item(key = "selected_header") {
                        SectionHeader(stringResource(id = R.string.identities_selected))
                    }
                    item(key = selected.identity.id) {
                        IdentityItem(selected, isSelected = true, onSharePrivateKey = { sharePrivateKey(selected) })
                    }
                }
                if (others.isNotEmpty()) {
                    item(key = "others_header") {
                        SectionHeader(stringResource(id = R.string.identities_others))
                    }
                    items(others, key = { it.identity.id }) { item ->
                        IdentityItem(item, onSharePrivateKey = { sharePrivateKey(item) }) {
                            TextButton(onClick = { selectIdentity(item.identity.id) }) {
                                Text(stringResource(id = R.string.identity_select))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Marks the selected identity. A mid green that reads on both light and dark backgrounds. */
private val SelectedIdentityColor = Color(0xFF43A047)

/**
 * Shows the linked contact's name and public key. The private key is not displayed here;
 * [onSharePrivateKey] asks to show it as a QR code. The selected identity is highlighted in green.
 */
@Composable
private fun IdentityItem(
    item: IdentityWithContact,
    onSharePrivateKey: () -> Unit,
    isSelected: Boolean = false,
    action: @Composable () -> Unit = {},
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) SelectedIdentityColor.copy(alpha = 0.16f) else Color.Transparent)
            .padding(horizontal = margin, vertical = margin / 2),
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = null,
            tint = if (isSelected) SelectedIdentityColor else LocalContentColor.current,
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
        action()
        IconButton(onClick = onSharePrivateKey) {
            Icon(Icons.Filled.QrCode2, stringResource(id = R.string.identity_show_qr))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = margin, end = margin, top = margin, bottom = margin / 4),
    )
}

@Preview
@Composable
fun IdentitiesScreenPreview() {
    Surface {
        IdentitiesContent(
            selected = IdentityWithContact(
                identity = IdentityRecord(id = 1, privateKey = "priv1", contactId = 1),
                contact = ContactRecord(id = 1, publicKey = "MCowBQYDK2VwAyEAabc", name = "Me", isFavorite = false, isIgnored = false),
            ),
            others = listOf(
                IdentityWithContact(
                    identity = IdentityRecord(id = 2, privateKey = "priv2", contactId = 2),
                    contact = ContactRecord(id = 2, publicKey = "MCowBQYDK2VwAyEAdef", name = null, isFavorite = false, isIgnored = false),
                ),
            ),
            isLoading = false,
            openDrawer = {},
            addIdentity = {},
            selectIdentity = {},
            sharePrivateKey = {},
        )
    }
}

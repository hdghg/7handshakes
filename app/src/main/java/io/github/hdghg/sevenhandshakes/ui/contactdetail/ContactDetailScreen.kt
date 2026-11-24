package io.github.hdghg.sevenhandshakes.ui.contactdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.HandshakePath
import io.github.hdghg.sevenhandshakes.data.ReviewExchange
import io.github.hdghg.sevenhandshakes.ui.SummaryRating
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.ui.ContactDetailTopAppBar
import io.github.hdghg.sevenhandshakes.ui.Field
import io.github.hdghg.sevenhandshakes.ui.LinkQrDialog
import io.github.hdghg.sevenhandshakes.ui.ShareActions
import io.github.hdghg.sevenhandshakes.ui.contactLabel
import io.github.hdghg.sevenhandshakes.ui.copyLinkAsRichText
import io.github.hdghg.sevenhandshakes.ui.shareAsFile
import io.github.hdghg.sevenhandshakes.ui.shareLinkAsText

@Composable
fun ContactDetailScreen(
    onBack: () -> Unit,
    onEdit: (contactId: Int) -> Unit,
    onInspectConnection: (publicKey: String) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onDeleted()
    }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    if (showDeleteDialog) {
        DeleteContactDialog(
            isUsedByIdentity = uiState.isUsedByIdentity,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteContact()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
    val context = LocalContext.current
    val shareLink = uiState.shareLink
    // What a link sent as formatted text reads: the contact as it is shown everywhere.
    val shareLabel = uiState.contact?.let { contactLabel(it) }.orEmpty()
    var showQr by rememberSaveable { mutableStateOf(false) }
    if (showQr && shareLink != null) {
        LinkQrDialog(
            title = stringResource(id = R.string.contact_details),
            link = shareLink,
            qrDescription = stringResource(id = R.string.contact_qr_description),
            onDismiss = { showQr = false },
        )
    }
    ContactDetailContent(
        contact = uiState.contact,
        isLoading = uiState.isLoading,
        handshakeRatings = uiState.handshakeRatings,
        handshakeDepth = uiState.handshakeDepth,
        onEdit = { uiState.contact?.let { onEdit(it.id) } },
        onInspectConnection = { uiState.contact?.let { onInspectConnection(it.publicKey) } },
        onDelete = { if (uiState.contact != null) showDeleteDialog = true },
        share = shareLink?.let { link ->
            ShareActions(
                asQr = { showQr = true },
                asLink = { shareLinkAsText(context, link) },
                asRichText = { copyLinkAsRichText(context, link, shareLabel) },
                asFile = ::shareAsFile,
            )
        },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun ContactDetailContent(
    contact: ContactRecord?,
    isLoading: Boolean,
    handshakeRatings: List<ReviewExchange>?,
    handshakeDepth: Int,
    onEdit: () -> Unit,
    onInspectConnection: () -> Unit,
    onDelete: () -> Unit,
    share: ShareActions?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ContactDetailTopAppBar(
                onBack = onBack,
                onEdit = onEdit,
                onDelete = onDelete,
                share = share,
            )
        },
    ) { paddingValues ->
        if (contact == null) {
            if (!isLoading) {
                Text(
                    text = stringResource(id = R.string.contact_not_found),
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
                    text = contactLabel(contact),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            contact.email?.takeIf { it.isNotBlank() }?.let { email ->
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
            if (contact.isFavorite) {
                Flag(Icons.Filled.Star, stringResource(id = R.string.contact_favorite), MaterialTheme.colorScheme.secondary)
            }
            if (contact.isIgnored) {
                Flag(Icons.Filled.Block, stringResource(id = R.string.contact_ignored), MaterialTheme.colorScheme.error)
            }
            // Brief: the sums only. The connection inspector explains them and shows every handshake level.
            if (handshakeRatings != null && handshakeDepth > 0) {
                Field(label = stringResource(id = R.string.summary_rating_title)) {
                    SummaryRating(
                        ratings = handshakeRatings,
                        showCaptions = false,
                        isCalculating = handshakeRatings.size < handshakeDepth,
                    )
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onInspectConnection) {
                            Text(text = stringResource(id = R.string.summary_rating_details))
                        }
                    }
                }
            }
        }
    }
}

/** Asks before deleting; for a contact an identity uses, only explains why it cannot be deleted. */
@Composable
private fun DeleteContactDialog(isUsedByIdentity: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.delete_contact_title)) },
        text = {
            Text(
                text = stringResource(
                    id = if (isUsedByIdentity) R.string.delete_contact_used_by_identity else R.string.delete_contact_message,
                ),
            )
        },
        confirmButton = {
            if (isUsedByIdentity) {
                TextButton(onClick = onDismiss) { Text(text = stringResource(id = android.R.string.ok)) }
            } else {
                TextButton(onClick = onConfirm) {
                    Text(text = stringResource(id = R.string.delete_contact_confirm), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = if (isUsedByIdentity) {
            null
        } else {
            { TextButton(onClick = onDismiss) { Text(text = stringResource(id = android.R.string.cancel)) } }
        },
    )
}

@Composable
private fun Flag(icon: ImageVector, label: String, tint: Color) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = margin / 2),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.padding(end = margin / 2))
        Text(text = label)
    }
}

@Preview
@Composable
fun ContactDetailScreenPreview() {
    Surface {
        ContactDetailContent(
            contact = ContactRecord(
                id = 1,
                publicKey = "MCowBQYDK2VwAyEAabcdefghijklmnopqrstuvwxyz0123456789ABCD",
                name = "Alice",
                isFavorite = true,
                isIgnored = true,
            ),
            isLoading = false,
            handshakeRatings = listOf(
                ReviewExchange(
                    receivedPath = HandshakePath(listOf(1, 9), listOf(6)),
                    givenPath = HandshakePath(listOf(9, 1), listOf(3)),
                ),
            ),
            handshakeDepth = 3,
            onEdit = {},
            onInspectConnection = {},
            onDelete = {},
            share = ShareActions(asQr = {}, asLink = {}, asRichText = {}, asFile = {}),
            onBack = {},
        )
    }
}
